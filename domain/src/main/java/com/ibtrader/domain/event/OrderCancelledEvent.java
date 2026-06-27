package com.ibtrader.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event raised when an order is cancelled, either by an explicit cancellation
 * request from the application layer or as a result of an Interactive Brokers
 * exchange/TWS notification.
 *
 * <p>The {@code reason} field should carry a human-readable explanation suitable for
 * display in audit dashboards and operator alert channels.</p>
 */
@Getter
public final class OrderCancelledEvent extends DomainEvent {

    /**
     * The domain identifier of the cancelled {@code Order} aggregate.
     */
    private final UUID orderId;

    /**
     * The IB-assigned order identifier of the cancelled order.
     */
    private final Integer ibOrderId;

    /**
     * The ticker symbol of the asset for which the order was placed.
     */
    private final String symbol;

    /**
     * A human-readable description of why the order was cancelled
     * (e.g. {@code "User-requested cancellation"}, {@code "Risk limit violated"}).
     */
    private final String reason;

    /**
     * Constructs an {@code OrderCancelledEvent} via its Lombok builder.
     *
     * @param orderId        domain identifier of the order aggregate
     * @param ibOrderId      IB-assigned order identifier
     * @param symbol         ticker symbol of the asset
     * @param reason         human-readable cancellation reason
     * @param sequenceNumber monotonic sequence number scoped to the order aggregate
     */
    @Builder
    private OrderCancelledEvent(
            UUID orderId,
            Integer ibOrderId,
            String symbol,
            String reason,
            long sequenceNumber) {

        super(orderId, "Order", sequenceNumber);
        this.orderId   = orderId;
        this.ibOrderId = ibOrderId;
        this.symbol    = symbol;
        this.reason    = reason;
    }
}
