package com.ibtrader.infrastructure.broker.ibkr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibtrader.domain.event.BrokerConnectedEvent;
import com.ibtrader.domain.event.BrokerDisconnectedEvent;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.DomainEventPublisher;
import com.ibtrader.domain.port.outbound.MarketDataCache;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the lifecycle of the optional Interactive Brokers client connection.
 */
@Slf4j
@Component
public class IbConnectionManager {

    private static final String BROKER_NAME = "INTERACTIVE_BROKERS";

    private final IbConnectionProperties properties;
    private final IbApiClient client;
    private final IbEWrapperAdapter wrapper;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "ib-connection-maintenance");
                thread.setDaemon(true);
                return thread;
            });

    private final AssetRepository assetRepository;
    private final MarketDataCache marketDataCache;
    private final PositionPersistenceService positionPersistenceService;
    private final Map<Integer, UUID> tickerAssetMap = new ConcurrentHashMap<>();
    private final AtomicInteger nextTickerId = new AtomicInteger(1000);

    private ScheduledFuture<?> reconnectFuture;
    private ScheduledFuture<?> heartbeatFuture;
    private Thread messageThread;

    public IbConnectionManager(
            IbConnectionProperties properties,
            IbApiClient client,
            IbEWrapperAdapter wrapper,
            DomainEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            AssetRepository assetRepository,
            MarketDataCache marketDataCache,
            PositionPersistenceService positionPersistenceService) {

        this.properties = properties;
        this.client = client;
        this.wrapper = wrapper;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.assetRepository = assetRepository;
        this.marketDataCache = marketDataCache;
        this.positionPersistenceService = positionPersistenceService;
        this.wrapper.setDisconnectHandler(this::onDisconnected);
        this.wrapper.setTickPriceHandler(this::onTickPrice);
        this.wrapper.setErrorHandler(this::onError);
    }

    public synchronized void connect() {
        if (!properties.isEnabled()) {
            log.info("IB adapter is disabled");
            return;
        }
        
        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.CONNECTED || currentState == ConnectionState.CONNECTING) {
            return;
        }

        state.set(currentState == ConnectionState.RECONNECTING ? ConnectionState.RECONNECTING : ConnectionState.CONNECTING);
        wrapper.resetHandshake();
        try {
            client.connect(
                    properties.getHost(),
                    properties.getPort(),
                    properties.getClientId(),
                    wrapper);
            startMessagePump();

            boolean handshakeComplete = wrapper.awaitNextValidId(
                    properties.getConnection().getConnectionTimeoutSeconds(),
                    TimeUnit.SECONDS);
            if (!handshakeComplete) {
                throw new IbConnectionException("Timed out waiting for broker handshake");
            }

            state.set(ConnectionState.CONNECTED);
            reconnectAttempts.set(0);
            startHeartbeat();

            eventPublisher.publish(BrokerConnectedEvent.of(
                    BROKER_NAME,
                    endpoint(),
                    client.serverVersion(),
                    Instant.now()));
            log.info("Connected to IB Gateway at {}", endpoint());
            
            subscribeToMarketData();
            
        } catch (RuntimeException exception) {
            client.disconnect();
            log.warn("IB connection attempt failed: {}", exception.getMessage());
            scheduleReconnect();
        }
    }

    public synchronized void onDisconnected(String reason) {
        ConnectionState previousState = state.getAndSet(ConnectionState.DISCONNECTED);
        stopHeartbeat();
        client.disconnect();
        if (previousState == ConnectionState.DISCONNECTED) {
            return;
        }

        eventPublisher.publish(BrokerDisconnectedEvent.of(
                BROKER_NAME,
                endpoint(),
                reason,
                Instant.now(),
                reconnectAttempts.get()));
        log.warn("Disconnected from IB Gateway: {}", reason);
        scheduleReconnect();
    }
    

    private void onError(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        switch (errorCode) {
            // ── Informational / advisory codes ───────────────────────────────────────
            case 2100: case 2101: case 2102: case 2103:
            case 2104: case 2105: case 2106: case 2107:
            case 2108: case 2109: case 2110: case 2119:
            case 2150: case 2157: case 2158:
                log.debug("IBKR info [{}] id={}: {}", errorCode, id, errorMsg);
                // 2104/2106 = market data farm OK; restore connectivity if needed
                if ((errorCode == 2104 || errorCode == 2106) && state.get() == ConnectionState.DISCONNECTED) {
                    scheduleReconnect();
                }
                break;

            // ── Delayed-data / subscription advisory (paper accounts) ───────────────
            case 10089: case 10090: case 10197:
                log.debug("IBKR subscription advisory [{}] id={}: {}", errorCode, id, errorMsg);
                break;

            // ── Connection lost ──────────────────────────────────────────────────────
            case 502: case 504: case 1100: case 1300:
                log.warn("IBKR connection lost [{}]: {}", errorCode, errorMsg);
                if (state.get() == ConnectionState.CONNECTED) {
                    onDisconnected(errorMsg);
                }
                break;

            // ── Connectivity restored ────────────────────────────────────────────────
            case 1101: case 1102:
                log.info("IBKR connectivity restored [{}]: {}", errorCode, errorMsg);
                if (state.get() == ConnectionState.DISCONNECTED) {
                    scheduleReconnect();
                }
                break;

            // ── Real errors (order rejection, invalid contract, etc.) ────────────────
            default:
                if (errorCode > 0) {
                    log.warn("IBKR error [{}] id={}: {}", errorCode, id, errorMsg);
                } else {
                    // Negative / unknown codes — log at debug
                    log.debug("IBKR callback [{}] id={}: {}", errorCode, id, errorMsg);
                }
                break;
        }
    }

    public synchronized void disconnect() {
        stopHeartbeat();
        cancelReconnect();
        state.set(ConnectionState.DISCONNECTED);
        tickerAssetMap.clear();
        client.disconnect();
    }

    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED && client.isConnected();
    }

    public int submitCommand(String payload) {
        if (!isConnected()) {
            throw new IllegalStateException("Cannot submit command, IBKR not connected.");
        }
        try {
            JsonNode json = objectMapper.readTree(payload);
            String symbol = json.path("symbol").asText();
            String side = json.path("side").asText();
            BigDecimal quantity = json.path("targetQuantity").decimalValue();
            JsonNode limitPriceNode = json.get("limitPrice");
            BigDecimal limitPrice = limitPriceNode == null || limitPriceNode.isNull()
                    ? null
                    : limitPriceNode.decimalValue();
            int ibOrderId = wrapper.reserveNextOrderId();

            client.placeStockOrder(ibOrderId, symbol, side, quantity, limitPrice);
            log.info("Submitted IB order {}: {} {} {}", ibOrderId, side, quantity, symbol);
            return ibOrderId;
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IbConnectionException("Unable to submit IB command payload", exception);
        }
    }

    private void subscribeToMarketData() {
        try {
            // Request delayed (free) market data — type 3 = 15-min delayed, works on paper accounts
            // without live subscription. Switch to type 1 when live subscriptions are enabled.
            client.requestMarketDataType(3);
            log.info("Market data type set to DELAYED (type=3) for paper account");

            assetRepository.findAll().forEach(asset -> {
                if (asset.isEnabled() && asset.isEquity()) {
                    int tickerId = nextTickerId.getAndIncrement();
                    tickerAssetMap.put(tickerId, asset.getId());
                    client.requestMarketData(tickerId, asset.getSymbol(), asset.getExchange(), asset.getCurrency());
                    log.info("Requested market data for {} (tickerId: {})", asset.getSymbol(), tickerId);
                }
            });
        } catch (Exception e) {
            log.error("Failed to subscribe to market data", e);
        }
    }


    private void onTickPrice(int tickerId, double price) {
        UUID assetId = tickerAssetMap.get(tickerId);
        if (assetId != null && price > 0.0) {
            marketDataCache.putPrice(assetId, BigDecimal.valueOf(price), Instant.now());
        }
    }

    private void startMessagePump() {
        messageThread = new Thread(() -> {
            while (client.isConnected()) {
                try {
                    client.processMessages();
                } catch (RuntimeException exception) {
                    onDisconnected("Message processing failed: " + exception.getMessage());
                }
            }
        }, "ib-message-pump");
        messageThread.setDaemon(true);
        messageThread.start();
    }

    public void setOrderStatusHandler(java.util.function.BiConsumer<Integer, String> handler) {
        wrapper.setOrderStatusHandler(handler);
    }

    /**
     * Triggers IB to stream back all current open positions via position() callbacks.
     * Results are upserted into the positions table keyed by (portfolioId, assetId).
     */
    public void requestPositions() {
        if (!isConnected()) {
            log.warn("Cannot request positions — IB not connected");
            return;
        }
        wrapper.setPositionHandler(this::onPosition);
        client.requestPositions();
        log.info("Sent reqPositions to IB Gateway");
    }

    private void onPosition(String account, String symbol, String currency, double quantity, double avgCost) {
        positionPersistenceService.upsertPosition(account, symbol, currency, quantity, avgCost);
    }

    private void scheduleReconnect() {
        if (!properties.isEnabled() || scheduler.isShutdown()) {
            return;
        }

        int attempt = reconnectAttempts.incrementAndGet();
        if (attempt > properties.getConnection().getMaxReconnectAttempts()) {
            log.error("IB reconnect attempts exhausted after {} attempts", attempt - 1);
            return;
        }

        cancelReconnect();
        reconnectFuture = scheduler.schedule(
                this::connect,
                properties.getConnection().getReconnectIntervalSeconds(),
                TimeUnit.SECONDS);
    }

    private void cancelReconnect() {
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
        }
    }

    private void startHeartbeat() {
        stopHeartbeat();
        long interval = properties.getConnection().getHeartbeatIntervalSeconds();
        heartbeatFuture = scheduler.scheduleAtFixedRate(
                this::sendHeartbeat, interval, interval, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
        }
    }

    private void sendHeartbeat() {
        if (!isConnected()) {
            return;
        }
        try {
            client.requestCurrentTime();
        } catch (RuntimeException exception) {
            onDisconnected("Heartbeat failed: " + exception.getMessage());
        }
    }

    private String endpoint() {
        return properties.getHost() + ":" + properties.getPort();
    }

    @PreDestroy
    public void shutdown() {
        disconnect();
        scheduler.shutdownNow();
    }
}
