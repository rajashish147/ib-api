package com.ibtrader.domain.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Broker-neutral event raised when an external trading broker becomes unavailable.
 */
@Getter
public final class BrokerDisconnectedEvent extends DomainEvent {

    private final String broker;
    private final String endpoint;
    private final String reason;
    private final Instant disconnectedAt;
    private final int reconnectAttemptNumber;

    private BrokerDisconnectedEvent(
            String broker,
            String endpoint,
            String reason,
            Instant disconnectedAt,
            int reconnectAttemptNumber,
            long sequenceNumber) {

        super(UUID.nameUUIDFromBytes(("broker:" + broker + ":" + endpoint).getBytes()),
                "BrokerConnection", sequenceNumber);
        this.broker = requireText(broker, "broker");
        this.endpoint = requireText(endpoint, "endpoint");
        this.reason = reason;
        this.disconnectedAt = disconnectedAt == null ? Instant.now() : disconnectedAt;
        this.reconnectAttemptNumber = reconnectAttemptNumber;
    }

    public static BrokerDisconnectedEvent of(
            String broker,
            String endpoint,
            String reason,
            Instant disconnectedAt,
            int reconnectAttemptNumber) {

        return new BrokerDisconnectedEvent(
                broker, endpoint, reason, disconnectedAt, reconnectAttemptNumber, 0L);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
