package com.ibtrader.domain.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class for all domain events in the IBKR trading platform.
 *
 * <p>Every domain event carries a unique {@code eventId} generated at construction time,
 * the wall-clock instant at which the event occurred, a reference to the aggregate that
 * raised the event, and a monotonically increasing {@code sequenceNumber} that callers
 * must supply to support ordered event replay and idempotency checks.</p>
 *
 * <p>Concrete subclasses should be declared as immutable value objects (Lombok
 * {@code @Value @Builder} or Java {@code record}) and must call one of the
 * protected constructors to initialise the base fields.</p>
 */
@Getter
public abstract class DomainEvent {

    /**
     * Globally unique identifier for this particular event instance.
     * Automatically assigned to {@link UUID#randomUUID()} at construction time.
     */
    private final UUID eventId;

    /**
     * Wall-clock instant at which this event was raised.
     * Automatically assigned to {@link Instant#now()} at construction time.
     */
    private final Instant occurredAt;

    /**
     * The identifier of the aggregate root that raised this event.
     */
    private final UUID aggregateId;

    /**
     * The simple class name (or logical name) of the aggregate type,
     * e.g. {@code "Portfolio"}, {@code "Order"}, {@code "StrategyInstance"}.
     */
    private final String aggregateType;

    /**
     * A monotonically increasing sequence number scoped to the aggregate.
     * Consumers can use this to detect gaps, enforce ordering, or achieve
     * idempotency when replaying events.
     */
    private final long sequenceNumber;

    /**
     * Primary constructor used by all subclasses.
     *
     * @param aggregateId    the UUID of the aggregate root that raised this event; must not be {@code null}
     * @param aggregateType  the logical type name of the aggregate; must not be {@code null} or blank
     * @param sequenceNumber monotonically increasing sequence number scoped to the aggregate
     */
    protected DomainEvent(UUID aggregateId, String aggregateType, long sequenceNumber) {
        if (aggregateId == null) {
            throw new IllegalArgumentException("aggregateId must not be null");
        }
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType must not be null or blank");
        }
        this.eventId        = UUID.randomUUID();
        this.occurredAt     = Instant.now();
        this.aggregateId    = aggregateId;
        this.aggregateType  = aggregateType;
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Convenience constructor for events where the sequence number is not
     * yet known (defaults to {@code 0}).  Prefer the three-argument constructor
     * in production use-cases.
     *
     * @param aggregateId   the UUID of the aggregate root that raised this event
     * @param aggregateType the logical type name of the aggregate
     */
    protected DomainEvent(UUID aggregateId, String aggregateType) {
        this(aggregateId, aggregateType, 0L);
    }

    /**
     * Returns a concise human-readable description of this event, useful for
     * logging and debugging.
     *
     * @return formatted string representation
     */
    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{eventId=" + eventId
                + ", aggregateId=" + aggregateId
                + ", aggregateType='" + aggregateType + '\''
                + ", sequenceNumber=" + sequenceNumber
                + ", occurredAt=" + occurredAt
                + '}';
    }
}
