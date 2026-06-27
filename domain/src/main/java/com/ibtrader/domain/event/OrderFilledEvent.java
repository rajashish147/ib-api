package com.ibtrader.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event raised when an order is completely and fully filled by Interactive Brokers.
 *
 * <p>This event signals that the entire requested quantity has been executed and
 * the {@code Order} aggregate has transitioned to the {@code FILLED} terminal state.
 * Position repositories, P&amp;L calculators, and audit log services should listen
 * to this event.</p>
 */
@Getter
public final class OrderFilledEvent extends DomainEvent {

    /**
     * The domain identifier of the fully-filled {@code Order} aggregate.
     */
    private final UUID orderId;

    /**
     * The IB-assigned order identifier for cross-referencing TWS/Gateway executions.
     */
    private final Integer ibOrderId;

    /**
     * The ticker symbol of the asset that was traded (e.g. {@code "MSFT"}).
     */
    private final String symbol;

    /**
     * Total quantity filled across all execution reports for this order.
     */
    private final BigDecimal filledQuantity;

    /**
     * Volume-weighted average fill price across all partial fills.
     */
    private final BigDecimal averageFillPrice;

    /**
     * Order direction: {@code "BUY"} or {@code "SELL"}.
     */
    private final String side;

    /**
     * Constructs an {@code OrderFilledEvent} via its Lombok builder.
     *
     * @param orderId          domain identifier of the order aggregate
     * @param ibOrderId        IB-assigned order identifier
     * @param symbol           ticker symbol of the traded asset
     * @param filledQuantity   total quantity executed
     * @param averageFillPrice VWAP of all fills
     * @param side             {@code "BUY"} or {@code "SELL"}
     * @param sequenceNumber   monotonic sequence number scoped to the order aggregate
     */
    @Builder
    private OrderFilledEvent(
            UUID orderId,
            Integer ibOrderId,
            String symbol,
            BigDecimal filledQuantity,
            BigDecimal averageFillPrice,
            String side,
            long sequenceNumber) {

        super(orderId, "Order", sequenceNumber);
        this.orderId           = orderId;
        this.ibOrderId         = ibOrderId;
        this.symbol            = symbol;
        this.filledQuantity    = filledQuantity;
        this.averageFillPrice  = averageFillPrice;
        this.side              = side;
    }
}
