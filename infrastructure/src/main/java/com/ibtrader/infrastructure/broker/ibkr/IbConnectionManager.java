package com.ibtrader.infrastructure.broker.ibkr;

import com.ibtrader.domain.event.BrokerConnectedEvent;
import com.ibtrader.domain.event.BrokerDisconnectedEvent;
import com.ibtrader.domain.port.outbound.DomainEventPublisher;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "ib-connection-maintenance");
                thread.setDaemon(true);
                return thread;
            });

    private ScheduledFuture<?> reconnectFuture;
    private ScheduledFuture<?> heartbeatFuture;
    private Thread messageThread;

    public IbConnectionManager(
            IbConnectionProperties properties,
            IbApiClient client,
            IbEWrapperAdapter wrapper,
            DomainEventPublisher eventPublisher) {

        this.properties = properties;
        this.client = client;
        this.wrapper = wrapper;
        this.eventPublisher = eventPublisher;
        this.wrapper.setDisconnectHandler(this::onDisconnected);
    }

    public synchronized void connect() {
        if (!properties.isEnabled()) {
            log.info("IB adapter is disabled");
            return;
        }
        if (connected.get() || connecting.get()) {
            return;
        }

        connecting.set(true);
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

            connected.set(true);
            connecting.set(false);
            reconnectAttempts.set(0);
            startHeartbeat();

            eventPublisher.publish(BrokerConnectedEvent.of(
                    BROKER_NAME,
                    endpoint(),
                    client.serverVersion(),
                    Instant.now()));
            log.info("Connected to IB Gateway at {}", endpoint());
        } catch (RuntimeException exception) {
            connecting.set(false);
            client.disconnect();
            log.warn("IB connection attempt failed: {}", exception.getMessage());
            scheduleReconnect();
        }
    }

    public synchronized void onDisconnected(String reason) {
        boolean wasActive = connected.getAndSet(false) || connecting.getAndSet(false);
        stopHeartbeat();
        client.disconnect();
        if (!wasActive) {
            return;
        }

        eventPublisher.publish(BrokerDisconnectedEvent.of(
                BROKER_NAME,
                endpoint(),
                reason,
                Instant.now(),
                reconnectAttempts.get()));
        scheduleReconnect();
    }

    public synchronized void disconnect() {
        stopHeartbeat();
        cancelReconnect();
        connected.set(false);
        connecting.set(false);
        client.disconnect();
    }

    public boolean isConnected() {
        return connected.get() && client.isConnected();
    }

    public void submitCommand(String payload) {
        if (!isConnected()) {
            throw new IllegalStateException("Cannot submit command, IBKR not connected.");
        }
        // TODO: Map payload to EClientSocket methods (e.g., placeOrder, reqMktData)
        log.info("Mock submitting command to IBKR: {}", payload);
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
