package com.ibtrader.domain.model.order;

import com.ibtrader.domain.model.common.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root that tracks the full lifecycle of a single order submitted
 * (or to be submitted) to Interactive Brokers.
 *
 * <p>An {@code Order} begins in {@link OrderStatus#PENDING_SUBMIT} and
 * progresses through a well-defined set of states driven by callbacks from
 * the IB TWS / Gateway API:
 * <pre>
 *   PENDING_SUBMIT → SUBMITTED → [PARTIALLY_FILLED →] FILLED
 *                              → CANCELLED
 *                              → REJECTED
 *                              → ERROR
 * </pre>
 *
 * <p>Every state transition is recorded in {@code statusHistory} with a
 * wall-clock timestamp prefix so that the full audit trail is queryable
 * without joining to a separate event table.
 *
 * <p>Use the static factory methods
 * ({@link #createMarket}, {@link #createLimit}, {@link #createStop})
 * to create instances; the private constructor enforces mandatory invariants.
 *
 * <p>Thread-safety: this class is <em>not</em> thread-safe. Consumers must
 * coordinate external synchronisation when sharing instances across threads.
 */
@Getter
@EqualsAndHashCode(of = "id")
public class Order {

    private static final DateTimeFormatter STATUS_TS_FMT =
            DateTimeFormatter.ISO_INSTANT;

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Internal surrogate key — stable and unique across the system. */
    private final UUID id;

    /**
     * IB's numeric order identifier assigned by TWS / Gateway when the order
     * is submitted. {@code null} until {@link #assignIbOrderId(int)} is called.
     */
    private Integer ibOrderId;

    // -------------------------------------------------------------------------
    // Order context
    // -------------------------------------------------------------------------

    /** IB account identifier against which the order is placed. */
    private final String accountId;

    /** Asset (contract) UUID this order is for. */
    private final UUID assetId;

    /** Denormalised ticker symbol retained for display and logging. */
    private final String symbol;

    // -------------------------------------------------------------------------
    // Order parameters
    // -------------------------------------------------------------------------

    /** Classification of the order execution algorithm (MARKET, LIMIT, …). */
    private final OrderType orderType;

    /** Direction of the trade. */
    private final OrderSide side;

    /** Total intended quantity in contract units. */
    private final BigDecimal quantity;

    /** Quantity already filled across all partial fills. Starts at zero. */
    private BigDecimal filledQuantity;

    /**
     * Limit price for LIMIT and STOP_LIMIT orders. {@code null} for MARKET
     * and pure STOP orders.
     */
    private final Money limitPrice;

    /**
     * Stop trigger price for STOP and STOP_LIMIT orders. {@code null} for
     * MARKET and LIMIT orders.
     */
    private final Money stopPrice;

    /**
     * Volume-weighted average fill price computed across all partial fills.
     * {@code null} until the first fill is recorded via {@link #recordFill}.
     */
    private Money averageFillPrice;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Current order status. */
    private OrderStatus status;

    /**
     * Reference to the strategy or rebalance plan that originated this order,
     * stored as a free-form string tag for cross-aggregate tracing.
     */
    private final String strategyRef;

    /**
     * UUID of the {@code RebalancePlan} that generated this order.
     * {@code null} for ad-hoc / manually placed orders.
     */
    private final UUID rebalancePlanId;

    /**
     * Ordered list of status transition records. Each entry is prefixed with
     * an ISO-8601 timestamp and a colon, e.g.
     * {@code "2024-06-01T10:00:00Z : SUBMITTED"}.
     * Most-recent entry first.
     */
    private final List<String> statusHistory;

    /** Timestamp when the order was submitted to IB (ibOrderId assigned). */
    private Instant submittedAt;

    /** Timestamp of the most recent mutation. */
    private Instant lastUpdatedAt;

    /**
     * Human-readable reason for rejection or error. Populated by
     * {@link #reject(String)} and {@link #markError(String)}.
     */
    private String rejectionReason;

    /**
     * Optimistic concurrency version — incremented by the repository on each
     * successful write.
     */
    private final long version;

    // =========================================================================
    // Private constructor
    // =========================================================================

    private Order(
            UUID id,
            Integer ibOrderId,
            String accountId,
            UUID assetId,
            String symbol,
            OrderType orderType,
            OrderSide side,
            BigDecimal quantity,
            Money limitPrice,
            Money stopPrice,
            String strategyRef,
            UUID rebalancePlanId) {

        this(
                id, ibOrderId, accountId, assetId, symbol, orderType, side, quantity,
                BigDecimal.ZERO, limitPrice, stopPrice, null, OrderStatus.PENDING_SUBMIT,
                strategyRef, rebalancePlanId, new ArrayList<>(), null, Instant.now(),
                null, 0L);
        addStatusHistory("PENDING_SUBMIT");
    }

    private Order(
            UUID id,
            Integer ibOrderId,
            String accountId,
            UUID assetId,
            String symbol,
            OrderType orderType,
            OrderSide side,
            BigDecimal quantity,
            BigDecimal filledQuantity,
            Money limitPrice,
            Money stopPrice,
            Money averageFillPrice,
            OrderStatus status,
            String strategyRef,
            UUID rebalancePlanId,
            List<String> statusHistory,
            Instant submittedAt,
            Instant lastUpdatedAt,
            String rejectionReason,
            long version) {

        this.id = id;
        this.ibOrderId = ibOrderId;
        this.accountId = accountId;
        this.assetId = assetId;
        this.symbol = symbol;
        this.orderType = orderType;
        this.side = side;
        this.quantity = quantity;
        this.filledQuantity = filledQuantity;
        this.limitPrice = limitPrice;
        this.stopPrice = stopPrice;
        this.averageFillPrice = averageFillPrice;
        this.status = status;
        this.strategyRef = strategyRef;
        this.rebalancePlanId = rebalancePlanId;
        this.statusHistory = new ArrayList<>(statusHistory);
        this.submittedAt = submittedAt;
        this.lastUpdatedAt = lastUpdatedAt;
        this.rejectionReason = rejectionReason;
        this.version = version;
    }

    // =========================================================================
    // Derived field
    // =========================================================================

    /**
     * Returns the quantity of the order that has not yet been filled.
     *
     * @return {@code quantity - filledQuantity}
     */
    public BigDecimal getRemainingQuantity() {
        return quantity.subtract(filledQuantity);
    }

    // =========================================================================
    // Factories
    // =========================================================================

    /**
     * Creates a new market order that will be executed at the prevailing best
     * bid or offer when submitted to IB.
     *
     * @param accountId   IB account identifier (not blank)
     * @param assetId     asset UUID (not null)
     * @param symbol      ticker symbol (not blank)
     * @param side        {@link OrderSide#BUY} or {@link OrderSide#SELL}
     * @param quantity    number of units to trade (must be positive)
     * @param strategyRef originating strategy reference tag
     * @return a new market order in {@link OrderStatus#PENDING_SUBMIT} state
     */
    public static Order createMarket(
            String accountId,
            UUID assetId,
            String symbol,
            OrderSide side,
            BigDecimal quantity,
            String strategyRef) {

        validateCommonParams(accountId, assetId, symbol, side, quantity);
        return new Order(
                UUID.randomUUID(), null, accountId, assetId, symbol,
                OrderType.MARKET, side, quantity, null, null, strategyRef, null);
    }

    /**
     * Creates a new limit order that will be executed only when the market
     * price reaches or betters {@code limitPrice}.
     *
     * @param accountId   IB account identifier (not blank)
     * @param assetId     asset UUID (not null)
     * @param symbol      ticker symbol (not blank)
     * @param side        {@link OrderSide#BUY} or {@link OrderSide#SELL}
     * @param quantity    number of units to trade (must be positive)
     * @param limitPrice  maximum (BUY) or minimum (SELL) acceptable price
     * @param strategyRef originating strategy reference tag
     * @return a new limit order in {@link OrderStatus#PENDING_SUBMIT} state
     */
    public static Order createLimit(
            String accountId,
            UUID assetId,
            String symbol,
            OrderSide side,
            BigDecimal quantity,
            Money limitPrice,
            String strategyRef) {

        validateCommonParams(accountId, assetId, symbol, side, quantity);
        if (limitPrice == null) {
            throw new IllegalArgumentException("limitPrice must not be null for a LIMIT order");
        }
        return new Order(
                UUID.randomUUID(), null, accountId, assetId, symbol,
                OrderType.LIMIT, side, quantity, limitPrice, null, strategyRef, null);
    }

    /**
     * Creates a new stop order that converts to a market order once the market
     * price touches {@code stopPrice}.
     *
     * @param accountId   IB account identifier (not blank)
     * @param assetId     asset UUID (not null)
     * @param symbol      ticker symbol (not blank)
     * @param side        {@link OrderSide#BUY} or {@link OrderSide#SELL}
     * @param quantity    number of units to trade (must be positive)
     * @param stopPrice   trigger price at which the order becomes a market order
     * @param strategyRef originating strategy reference tag
     * @return a new stop order in {@link OrderStatus#PENDING_SUBMIT} state
     */
    public static Order createStop(
            String accountId,
            UUID assetId,
            String symbol,
            OrderSide side,
            BigDecimal quantity,
            Money stopPrice,
            String strategyRef) {

        validateCommonParams(accountId, assetId, symbol, side, quantity);
        if (stopPrice == null) {
            throw new IllegalArgumentException("stopPrice must not be null for a STOP order");
        }
        return new Order(
                UUID.randomUUID(), null, accountId, assetId, symbol,
                OrderType.STOP, side, quantity, null, stopPrice, strategyRef, null);
    }

    /**
     * Restores a previously persisted order without generating new identity,
     * timestamps, history entries, or optimistic-lock state.
     */
    public static Order rehydrate(
            UUID id,
            Integer ibOrderId,
            String accountId,
            UUID assetId,
            String symbol,
            OrderType orderType,
            OrderSide side,
            BigDecimal quantity,
            BigDecimal filledQuantity,
            Money limitPrice,
            Money stopPrice,
            Money averageFillPrice,
            OrderStatus status,
            String strategyRef,
            UUID rebalancePlanId,
            List<String> statusHistory,
            Instant submittedAt,
            Instant lastUpdatedAt,
            String rejectionReason,
            long version) {

        validateCommonParams(accountId, assetId, symbol, side, quantity);
        if (id == null || orderType == null || filledQuantity == null || status == null
                || statusHistory == null || lastUpdatedAt == null || version < 0) {
            throw new IllegalArgumentException("Persisted order state is incomplete");
        }
        if (filledQuantity.signum() < 0 || filledQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("filledQuantity must be between zero and quantity");
        }
        return new Order(
                id, ibOrderId, accountId, assetId, symbol, orderType, side, quantity,
                filledQuantity, limitPrice, stopPrice, averageFillPrice, status,
                strategyRef, rebalancePlanId, statusHistory, submittedAt, lastUpdatedAt,
                rejectionReason, version);
    }

    // =========================================================================
    // Lifecycle transitions
    // =========================================================================

    /**
     * Records the IB-assigned numeric order ID and marks the order as
     * submitted. This method must be called exactly once when IB acknowledges
     * the order.
     *
     * @param ibOrderId positive numeric order ID from IB
     * @throws IllegalStateException    if the order is not in
     *                                  {@link OrderStatus#PENDING_SUBMIT}
     * @throws IllegalArgumentException if {@code ibOrderId} is not positive
     */
    public void assignIbOrderId(int ibOrderId) {
        if (this.status != OrderStatus.PENDING_SUBMIT) {
            throw new IllegalStateException(
                    "Cannot assign IB order ID when order is in state: " + status);
        }
        if (ibOrderId <= 0) {
            throw new IllegalArgumentException(
                    "IB order ID must be positive, got: " + ibOrderId);
        }
        this.ibOrderId   = ibOrderId;
        this.status      = OrderStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
        addStatusHistory("SUBMITTED (ibOrderId=" + ibOrderId + ")");
    }

    /**
     * Records a fill event received from IB. Multiple calls accumulate partial
     * fills using a volume-weighted average price calculation. Automatically
     * transitions the order to {@link OrderStatus#PARTIALLY_FILLED} or
     * {@link OrderStatus#FILLED} depending on remaining quantity.
     *
     * @param fillQty   the quantity filled in this execution report (positive)
     * @param fillPrice the price at which this fill occurred
     * @throws IllegalStateException    if the order is already in a terminal
     *                                  state or not yet submitted
     * @throws IllegalArgumentException if {@code fillQty} is non-positive or
     *                                  exceeds the remaining quantity
     */
    public void recordFill(BigDecimal fillQty, Money fillPrice) {
        if (status == null || status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot record fill for order in terminal state: " + status);
        }
        if (status == OrderStatus.PENDING_SUBMIT) {
            throw new IllegalStateException(
                    "Cannot record fill for unsubmitted order");
        }
        if (fillQty == null || fillQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("fillQty must be positive");
        }
        if (fillQty.compareTo(getRemainingQuantity()) > 0) {
            throw new IllegalArgumentException(
                    "fillQty " + fillQty + " exceeds remaining quantity "
                    + getRemainingQuantity());
        }

        // Weighted-average fill price calculation
        if (averageFillPrice == null) {
            averageFillPrice = fillPrice;
        } else {
            BigDecimal prevTotal = averageFillPrice.getAmount().multiply(filledQuantity);
            BigDecimal newTotal  = fillPrice.getAmount().multiply(fillQty);
            BigDecimal newFilledTotal = filledQuantity.add(fillQty);
            BigDecimal wavg = prevTotal.add(newTotal)
                    .divide(newFilledTotal, 8, RoundingMode.HALF_UP);
            averageFillPrice = Money.of(wavg, fillPrice.getCurrency());
        }

        filledQuantity = filledQuantity.add(fillQty);

        boolean fullyFilled = getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0;
        status = fullyFilled ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
        lastUpdatedAt = Instant.now();
        addStatusHistory(status + " (fillQty=" + fillQty + " @ " + fillPrice.getAmount() + ")");
    }

    /**
     * Transitions the order to {@link OrderStatus#CANCELLED} state.
     * Idempotent if the order is already cancelled.
     *
     * @param reason human-readable cancellation reason
     * @throws IllegalStateException if the order is in a terminal state other
     *                               than CANCELLED
     */
    public void cancel(String reason) {
        if (status == OrderStatus.CANCELLED) return;
        if (status != null && status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot cancel order in terminal state: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.lastUpdatedAt = Instant.now();
        addStatusHistory("CANCELLED" + (reason != null ? ": " + reason : ""));
    }

    /**
     * Transitions the order to {@link OrderStatus#REJECTED} and records the
     * rejection reason provided by IB.
     *
     * @param reason IB-supplied rejection message (may be null)
     * @throws IllegalStateException if the order is already in a terminal state
     */
    public void reject(String reason) {
        if (status != null && status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot reject order in terminal state: " + status);
        }
        this.status          = OrderStatus.REJECTED;
        this.rejectionReason = reason;
        this.lastUpdatedAt   = Instant.now();
        addStatusHistory("REJECTED" + (reason != null ? ": " + reason : ""));
    }

    /**
     * Transitions the order to {@link OrderStatus#ERROR} to indicate an
     * unrecoverable processing fault.
     *
     * @param reason description of the error condition
     * @throws IllegalStateException if the order is already in a terminal state
     */
    public void markError(String reason) {
        if (status != null && status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot mark error for order in terminal state: " + status);
        }
        this.status          = OrderStatus.ERROR;
        this.rejectionReason = reason;
        this.lastUpdatedAt   = Instant.now();
        addStatusHistory("ERROR" + (reason != null ? ": " + reason : ""));
    }

    // =========================================================================
    // Predicates
    // =========================================================================

    /**
     * Returns {@code true} when this order has reached a terminal state (filled,
     * cancelled, rejected, or error).
     *
     * @return {@code true} iff {@code status.isTerminal()}
     */
    public boolean isComplete() {
        return status != null && status.isTerminal();
    }

    /**
     * Returns {@code true} when this is a buy-side order.
     *
     * @return {@code true} iff {@code side == BUY}
     */
    public boolean isBuyOrder() {
        return OrderSide.BUY == side;
    }

    /**
     * Returns {@code true} when this is a sell-side order.
     *
     * @return {@code true} iff {@code side == SELL}
     */
    public boolean isSellOrder() {
        return OrderSide.SELL == side;
    }

    // =========================================================================
    // Notional value
    // =========================================================================

    /**
     * Computes the gross notional value of this order at the supplied market
     * price. For equity orders (multiplier = 1) this equals
     * {@code quantity * currentPrice}.
     *
     * <p>The multiplier is intentionally fixed at 1 here; callers working with
     * futures contracts should scale the result externally using the contract
     * multiplier obtained from the {@code Asset} aggregate.
     *
     * @param currentPrice reference price for the notional calculation (not null)
     * @return {@code quantity * currentPrice}
     */
    public BigDecimal getNotionalValue(Money currentPrice) {
        if (currentPrice == null) {
            throw new IllegalArgumentException("currentPrice must not be null");
        }
        return quantity.multiply(currentPrice.getAmount());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Prepends a timestamped entry to {@code statusHistory}.
     *
     * @param entry description of the transition
     */
    private void addStatusHistory(String entry) {
        String timestamped = Instant.now().toString() + " : " + entry;
        statusHistory.add(0, timestamped);
    }

    /**
     * Validates the parameters shared across all factory methods.
     */
    private static void validateCommonParams(
            String accountId,
            UUID assetId,
            String symbol,
            OrderSide side,
            BigDecimal quantity) {

        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
        if (assetId == null) {
            throw new IllegalArgumentException("assetId must not be null");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
