package com.ibtrader.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event raised when an order is successfully submitted to Interactive Brokers
 * and an IB order ID has been assigned.
 *
 * <p>This event marks the transition of an {@code Order} aggregate from a
 * {@code PENDING} state to {@code SUBMITTED}.  Consumers such as audit log writers,
 * risk monitors, and execution trackers should subscribe to this event.</p>
 *
 * <p>{@code limitPrice} is {@code null} for market orders and must be present for
 * limit orders.</p>
 */
@Getter
public final class OrderSubmittedEvent extends DomainEvent {

    /**
     * The domain identifier of the {@code Order} aggregate.
     */
    private final UUID orderId;

    /**
     * The IB-assigned order identifier returned by the TWS/Gateway after submission.
     */
    private final Integer ibOrderId;

    /**
     * The IB account string to which the order was submitted (e.g. {@code "DU1234567"}).
     */
    private final String accountId;

    /**
     * The domain identifier of the {@code Asset} being traded.
     */
    private final UUID assetId;

    /**
     * The ticker symbol of the asset (e.g. {@code "AAPL"}).
     */
    private final String symbol;

    /**
     * Order direction: {@code "BUY"} or {@code "SELL"}.
     */
    private final String side;

    /**
     * Order type string as understood by IB (e.g. {@code "LMT"}, {@code "MKT"}).
     */
    private final String orderType;

    /**
     * The requested order quantity in shares (or contract units).
     */
    private final BigDecimal quantity;

    /**
     * The limit price for limit orders; {@code null} for market orders.
     */
    private final BigDecimal limitPrice;

    /**
     * An opaque reference back to the strategy or rebalance plan that generated
     * this order, for traceability (e.g. {@code "strategy:uuid"} or {@code "plan:uuid"}).
     */
    private final String strategyRef;

    /**
     * Constructs an {@code OrderSubmittedEvent} via its Lombok builder.
     *
     * @param orderId        domain identifier of the order aggregate
     * @param ibOrderId      IB-assigned order ID (must not be {@code null})
     * @param accountId      IB account string
     * @param assetId        domain identifier of the asset being traded
     * @param symbol         ticker symbol
     * @param side           {@code "BUY"} or {@code "SELL"}
     * @param orderType      IB order type string
     * @param quantity       order quantity
     * @param limitPrice     limit price; {@code null} for market orders
     * @param strategyRef    opaque strategy / plan reference string
     * @param sequenceNumber monotonic sequence number scoped to the order aggregate
     */
    @Builder
    private OrderSubmittedEvent(
            UUID orderId,
            Integer ibOrderId,
            String accountId,
            UUID assetId,
            String symbol,
            String side,
            String orderType,
            BigDecimal quantity,
            BigDecimal limitPrice,
            String strategyRef,
            long sequenceNumber) {

        super(orderId, "Order", sequenceNumber);
        this.orderId      = orderId;
        this.ibOrderId    = ibOrderId;
        this.accountId    = accountId;
        this.assetId      = assetId;
        this.symbol       = symbol;
        this.side         = side;
        this.orderType    = orderType;
        this.quantity     = quantity;
        this.limitPrice   = limitPrice;
        this.strategyRef  = strategyRef;
    }
}
