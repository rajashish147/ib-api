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
            MarketDataCache marketDataCache) {

        this.properties = properties;
        this.client = client;
        this.wrapper = wrapper;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.assetRepository = assetRepository;
        this.marketDataCache = marketDataCache;
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
        log.error("IBKR Error {} [{}]: {}", errorCode, id, errorMsg);
        
        switch (errorCode) {
            case 502: // Couldn't connect to TWS. Confirm that "Enable ActiveX and Socket Clients" is enabled
            case 504: // Not connected
            case 1100: // Connectivity between IB and TWS has been lost.
            case 2110: // Connectivity between TWS and server is broken.
                log.warn("Connection lost/broken due to error code {}", errorCode);
                if (state.get() == ConnectionState.CONNECTED) {
                    onDisconnected(errorMsg);
                }
                break;
            case 1101: // Connectivity between IB and TWS has been restored
            case 2104: // Market data farm connection is OK
                log.info("Connectivity restored (code {})", errorCode);
                if (state.get() == ConnectionState.DISCONNECTED) {
                    scheduleReconnect(); // trigger immediate reconnect
                }
                break;
            default:
                // Other business errors (order rejections, etc.) are handled by specific handlers
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
