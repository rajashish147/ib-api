package com.ibtrader.domain.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Broker-neutral event raised when an external trading broker becomes available.
 */
@Getter
public final class BrokerConnectedEvent extends DomainEvent {

    private final String broker;
    private final String endpoint;
    private final String serverVersion;
    private final Instant connectedAt;

    private BrokerConnectedEvent(
            String broker,
            String endpoint,
            String serverVersion,
            Instant connectedAt,
            long sequenceNumber) {

        super(UUID.nameUUIDFromBytes(("broker:" + broker + ":" + endpoint).getBytes()),
                "BrokerConnection", sequenceNumber);
        this.broker = requireText(broker, "broker");
        this.endpoint = requireText(endpoint, "endpoint");
        this.serverVersion = serverVersion;
        this.connectedAt = connectedAt == null ? Instant.now() : connectedAt;
    }

    public static BrokerConnectedEvent of(
            String broker,
            String endpoint,
            String serverVersion,
            Instant connectedAt) {

        return new BrokerConnectedEvent(broker, endpoint, serverVersion, connectedAt, 0L);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
