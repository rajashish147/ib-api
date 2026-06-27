package com.ibtrader.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event raised when Interactive Brokers reports an execution that partially
 * fills an outstanding order.
 *
 * <p>An order may receive many partial-fill events before eventually transitioning
 * to {@code FILLED}.  Consumers should accumulate {@code filledQuantity} across
 * events to reconstruct the complete fill picture, or rely on the {@code Order}
 * aggregate's own state for an authoritative view.</p>
 */
@Getter
public final class OrderPartiallyFilledEvent extends DomainEvent {

    /**
     * The domain identifier of the {@code Order} aggregate that was partially filled.
     */
    private final UUID orderId;

    /**
     * The IB-assigned order identifier for cross-referencing TWS/Gateway executions.
     */
    private final Integer ibOrderId;

    /**
     * The ticker symbol of the asset being traded.
     */
    private final String symbol;

    /**
     * Cumulative quantity filled so far (including this fill).
     */
    private final BigDecimal filledQuantity;

    /**
     * Quantity still outstanding after this partial fill.
     */
    private final BigDecimal remainingQuantity;

    /**
     * The price at which this specific partial execution occurred.
     */
    private final BigDecimal lastFillPrice;

    /**
     * Order direction: {@code "BUY"} or {@code "SELL"}.
     */
    private final String side;

    /**
     * Constructs an {@code OrderPartiallyFilledEvent} via its Lombok builder.
     *
     * @param orderId            domain identifier of the order aggregate
     * @param ibOrderId          IB-assigned order identifier
     * @param symbol             ticker symbol of the traded asset
     * @param filledQuantity     cumulative quantity filled to date
     * @param remainingQuantity  quantity still open after this fill
     * @param lastFillPrice      price of this individual fill
     * @param side               {@code "BUY"} or {@code "SELL"}
     * @param sequenceNumber     monotonic sequence number scoped to the order aggregate
     */
    @Builder
    private OrderPartiallyFilledEvent(
            UUID orderId,
            Integer ibOrderId,
            String symbol,
            BigDecimal filledQuantity,
            BigDecimal remainingQuantity,
            BigDecimal lastFillPrice,
            String side,
            long sequenceNumber) {

        super(orderId, "Order", sequenceNumber);
        this.orderId            = orderId;
        this.ibOrderId          = ibOrderId;
        this.symbol             = symbol;
        this.filledQuantity     = filledQuantity;
        this.remainingQuantity  = remainingQuantity;
        this.lastFillPrice      = lastFillPrice;
        this.side               = side;
    }
}
