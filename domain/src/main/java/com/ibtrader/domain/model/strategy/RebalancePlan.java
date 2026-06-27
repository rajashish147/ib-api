package com.ibtrader.domain.model.strategy;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.order.OrderSide;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root representing a fully specified rebalancing plan generated
 * by the strategy engine for a single rebalance cycle.
 *
 * <p>A {@code RebalancePlan} is created when a {@link StrategyInstance} raises
 * a buy or sell trigger and the strategy engine calculates the set of trades
 * needed to bring the portfolio back to its target allocations. The plan
 * transitions through the following states:
 *
 * <pre>
 *   DRAFT → APPROVED → EXECUTING → COMPLETED
 *                    ↘ CANCELLED
 *                    ↘ FAILED
 * </pre>
 *
 * <p>Only an {@link PlanStatus#APPROVED} plan may be executed. Plans in other
 * states must not generate new orders.
 *
 * <p>Use the {@link #create} factory to construct instances.
 *
 * <p>Thread-safety: this class is <em>not</em> thread-safe.
 */
@Getter
@EqualsAndHashCode(of = "id")
public class RebalancePlan {

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Surrogate key unique across all rebalance plans in the system. */
    private final UUID id;

    /** UUID of the {@link StrategyInstance} that generated this plan. */
    private final UUID strategyId;

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /** The event type that initiated this rebalance cycle. */
    private final TriggerType triggerType;

    /** Execution mode (matches the owning strategy's mode at time of creation). */
    private final StrategyMode mode;

    /** Portfolio NLV captured at the moment the plan was generated. */
    private final Money portfolioNlvAtTrigger;

    /** Cash available for deployment — used to size buy orders. */
    private final Money availableBudget;

    // -------------------------------------------------------------------------
    // Status & items
    // -------------------------------------------------------------------------

    /** Current plan lifecycle status. */
    private PlanStatus status;

    /**
     * Ordered list of individual trade line items. Each item specifies one
     * asset's current vs. target allocation and the resulting order to place.
     */
    private final List<RebalancePlanItem> items;

    /** Optional free-form notes added by the strategy engine or operator. */
    private String notes;

    // -------------------------------------------------------------------------
    // Timestamps
    // -------------------------------------------------------------------------

    /** Wall-clock time when this plan was created. */
    private final Instant createdAt;

    /** Wall-clock time when execution of orders started. Null until {@link #markExecuting()}. */
    private Instant executedAt;

    /** Wall-clock time when the plan reached a terminal state. Null until terminal. */
    private Instant completedAt;

    // -------------------------------------------------------------------------
    // Optimistic locking
    // -------------------------------------------------------------------------

    /**
     * Optimistic concurrency version — incremented by the repository on each
     * successful write.
     */
    private final long version;

    // =========================================================================
    // Private constructor
    // =========================================================================

    private RebalancePlan(
            UUID id,
            UUID strategyId,
            TriggerType triggerType,
            StrategyMode mode,
            Money portfolioNlvAtTrigger,
            Money availableBudget,
            PlanStatus status,
            List<RebalancePlanItem> items,
            String notes,
            Instant createdAt,
            Instant executedAt,
            Instant completedAt,
            long version) {

        this.id                    = id;
        this.strategyId            = strategyId;
        this.triggerType           = triggerType;
        this.mode                  = mode;
        this.portfolioNlvAtTrigger = portfolioNlvAtTrigger;
        this.availableBudget       = availableBudget;
        this.status                = status;
        this.items                 = items;
        this.notes                 = notes;
        this.createdAt             = createdAt;
        this.executedAt            = executedAt;
        this.completedAt           = completedAt;
        this.version               = version;
    }

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a new {@code RebalancePlan} in {@link PlanStatus#DRAFT} state.
     * Items must be added separately via {@link #addItem(RebalancePlanItem)};
     * the plan must then be {@link #approve() approved} before it can be
     * executed.
     *
     * @param strategyId  owning strategy UUID (not null)
     * @param triggerType initiating trigger type (not null)
     * @param mode        execution mode for this rebalance (not null)
     * @param nlv         portfolio NLV captured at trigger time (not null)
     * @param budget      cash budget available for buy orders (not null)
     * @return a new draft rebalance plan with an empty item list
     * @throws IllegalArgumentException if any required argument is null
     */
    public static RebalancePlan create(
            UUID strategyId,
            TriggerType triggerType,
            StrategyMode mode,
            Money nlv,
            Money budget) {

        if (strategyId == null)   throw new IllegalArgumentException("strategyId must not be null");
        if (triggerType == null)  throw new IllegalArgumentException("triggerType must not be null");
        if (mode == null)         throw new IllegalArgumentException("mode must not be null");
        if (nlv == null)          throw new IllegalArgumentException("nlv must not be null");
        if (budget == null)       throw new IllegalArgumentException("budget must not be null");

        return new RebalancePlan(
                UUID.randomUUID(),
                strategyId,
                triggerType,
                mode,
                nlv,
                budget,
                PlanStatus.DRAFT,
                new ArrayList<>(),
                null,
                Instant.now(),
                null,
                null,
                0L);
    }

    // =========================================================================
    // Item management
    // =========================================================================

    /**
     * Appends a {@link RebalancePlanItem} to this plan. Items may only be
     * added while the plan is in {@link PlanStatus#DRAFT} state.
     *
     * @param item the plan item to add (not null)
     * @throws IllegalArgumentException if {@code item} is null
     * @throws IllegalStateException    if the plan is not in DRAFT state
     */
    public void addItem(RebalancePlanItem item) {
        if (item == null) throw new IllegalArgumentException("item must not be null");
        if (status != PlanStatus.DRAFT) {
            throw new IllegalStateException(
                    "Items may only be added to plans in DRAFT state, current: " + status);
        }
        items.add(item);
    }

    // =========================================================================
    // Derived values
    // =========================================================================

    /**
     * Sums the estimated values of all BUY-side plan items.
     *
     * @return total estimated capital to be deployed for buys; zero if none
     */
    public Money totalBuyValue() {
        return items.stream()
                .filter(i -> OrderSide.BUY == i.getSide())
                .map(RebalancePlanItem::getEstimatedValue)
                .reduce(Money.of(BigDecimal.ZERO, availableBudget.getCurrency()), Money::add);
    }

    /**
     * Sums the estimated values of all SELL-side plan items.
     *
     * @return total estimated proceeds from sells; zero if none
     */
    public Money totalSellValue() {
        return items.stream()
                .filter(i -> OrderSide.SELL == i.getSide())
                .map(RebalancePlanItem::getEstimatedValue)
                .reduce(Money.of(BigDecimal.ZERO, availableBudget.getCurrency()), Money::add);
    }

    /**
     * Returns {@code true} when this plan is ready for order generation.
     * A plan is executable iff it is in {@link PlanStatus#APPROVED} status
     * and contains at least one item.
     *
     * @return {@code true} iff {@code status == APPROVED && !items.isEmpty()}
     */
    public boolean isExecutable() {
        return status == PlanStatus.APPROVED && !items.isEmpty();
    }

    // =========================================================================
    // Status transitions
    // =========================================================================

    /**
     * Approves this plan, making it eligible for execution.
     *
     * @throws IllegalStateException if the plan is not in DRAFT state
     */
    public void approve() {
        requireStatus(PlanStatus.DRAFT, "approve");
        this.status = PlanStatus.APPROVED;
    }

    /**
     * Marks this plan as actively executing (orders are being submitted).
     *
     * @throws IllegalStateException if the plan is not in APPROVED state
     */
    public void markExecuting() {
        requireStatus(PlanStatus.APPROVED, "markExecuting");
        this.status      = PlanStatus.EXECUTING;
        this.executedAt  = Instant.now();
    }

    /**
     * Marks this plan as successfully completed.
     *
     * @throws IllegalStateException if the plan is not in EXECUTING state
     */
    public void complete() {
        requireStatus(PlanStatus.EXECUTING, "complete");
        this.status      = PlanStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Marks this plan as failed. Accepts a reason string for diagnostics.
     *
     * @param reason human-readable failure description (may be null)
     * @throws IllegalStateException if the plan is already in a terminal state
     */
    public void fail(String reason) {
        if (status == PlanStatus.COMPLETED
                || status == PlanStatus.CANCELLED
                || status == PlanStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot fail a plan in terminal state: " + status);
        }
        this.status      = PlanStatus.FAILED;
        this.notes       = (reason != null) ? reason : notes;
        this.completedAt = Instant.now();
    }

    /**
     * Cancels this plan before or during execution. Idempotent if already
     * cancelled.
     *
     * @throws IllegalStateException if the plan is in COMPLETED or FAILED state
     */
    public void cancel() {
        if (status == PlanStatus.CANCELLED) return;
        if (status == PlanStatus.COMPLETED || status == PlanStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot cancel a plan in terminal state: " + status);
        }
        this.status      = PlanStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Guards a status transition by asserting the plan is in the expected state.
     *
     * @param expected required current status
     * @param action   name of the calling method (for error messages)
     * @throws IllegalStateException if the plan is not in {@code expected} state
     */
    private void requireStatus(PlanStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " plan in state " + status
                    + "; expected: " + expected);
        }
    }
}
