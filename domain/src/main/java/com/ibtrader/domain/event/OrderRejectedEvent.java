package com.ibtrader.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event raised when an order is rejected either by the Interactive Brokers
 * gateway, the exchange, or by the domain's own pre-submission validation.
 *
 * <p>The {@code rejectionReason} field should contain the full error message
 * returned by IB or the domain validation rule that was violated.  This event
 * moves the {@code Order} aggregate into the {@code REJECTED} terminal state.</p>
 */
@Getter
public final class OrderRejectedEvent extends DomainEvent {

    /**
     * The domain identifier of the rejected {@code Order} aggregate.
     */
    private final UUID orderId;

    /**
     * The IB-assigned order identifier if one was assigned prior to rejection;
     * may be {@code null} if the order was rejected before reaching IB.
     */
    private final Integer ibOrderId;

    /**
     * The ticker symbol of the asset for which the order was attempted.
     */
    private final String symbol;

    /**
     * Full description of the rejection reason as returned by IB or the domain validator.
     */
    private final String rejectionReason;

    /**
     * Order direction: {@code "BUY"} or {@code "SELL"}.
     */
    private final String side;

    /**
     * The quantity that was requested in the rejected order.
     */
    private final BigDecimal quantity;

    /**
     * Constructs an {@code OrderRejectedEvent} via its Lombok builder.
     *
     * @param orderId          domain identifier of the order aggregate
     * @param ibOrderId        IB-assigned order identifier (may be {@code null})
     * @param symbol           ticker symbol of the asset
     * @param rejectionReason  human-readable reason for the rejection
     * @param side             {@code "BUY"} or {@code "SELL"}
     * @param quantity         requested quantity that was rejected
     * @param sequenceNumber   monotonic sequence number scoped to the order aggregate
     */
    @Builder
    private OrderRejectedEvent(
            UUID orderId,
            Integer ibOrderId,
            String symbol,
            String rejectionReason,
            String side,
            BigDecimal quantity,
            long sequenceNumber) {

        super(orderId, "Order", sequenceNumber);
        this.orderId          = orderId;
        this.ibOrderId        = ibOrderId;
        this.symbol           = symbol;
        this.rejectionReason  = rejectionReason;
        this.side             = side;
        this.quantity         = quantity;
    }
}
