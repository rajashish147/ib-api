package com.ibtrader.domain.event;

import com.ibtrader.domain.model.strategy.StrategyMode;
import com.ibtrader.domain.model.strategy.TriggerType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event raised when the system successfully generates a {@code RebalancePlan}
 * for a given strategy.
 *
 * <p>This event records the key financial parameters of the generated plan so that
 * downstream consumers — such as execution orchestrators, compliance checks, and
 * operator dashboards — can make informed decisions without loading the full plan
 * aggregate from the repository.</p>
 *
 * <p>After this event is published, the plan enters {@code PENDING_EXECUTION} state
 * and awaits approval or automatic execution depending on the strategy's configured
 * {@link StrategyMode}.</p>
 */
@Getter
public final class RebalancePlanGeneratedEvent extends DomainEvent {

    /**
     * The domain identifier of the newly generated {@code RebalancePlan}.
     */
    private final UUID planId;

    /**
     * The domain identifier of the {@code StrategyInstance} that owns this plan.
     */
    private final UUID strategyId;

    /**
     * The operational mode of the strategy at plan-generation time.
     */
    private final StrategyMode mode;

    /**
     * The trigger type that caused the plan to be generated
     * (e.g. {@code THRESHOLD}, {@code SCHEDULED}, {@code MANUAL}).
     */
    private final TriggerType triggerType;

    /**
     * The portfolio's NLV at the moment the rebalance was triggered.
     */
    private final BigDecimal nlvAtTrigger;

    /**
     * The total number of line items (buy + sell legs) in the generated plan.
     */
    private final int itemCount;

    /**
     * Aggregate notional value of all buy-side plan items.
     */
    private final BigDecimal totalBuyValue;

    /**
     * Aggregate notional value of all sell-side plan items.
     */
    private final BigDecimal totalSellValue;

    /**
     * Constructs a {@code RebalancePlanGeneratedEvent} via its Lombok builder.
     *
     * @param planId           domain identifier of the rebalance plan
     * @param strategyId       domain identifier of the owning strategy
     * @param mode             operational mode of the strategy
     * @param triggerType      trigger that caused plan generation
     * @param nlvAtTrigger     portfolio NLV at trigger time
     * @param itemCount        number of line items in the plan
     * @param totalBuyValue    aggregate notional buy value
     * @param totalSellValue   aggregate notional sell value
     * @param sequenceNumber   monotonic sequence number scoped to the strategy aggregate
     */
    @Builder
    private RebalancePlanGeneratedEvent(
            UUID planId,
            UUID strategyId,
            StrategyMode mode,
            TriggerType triggerType,
            BigDecimal nlvAtTrigger,
            int itemCount,
            BigDecimal totalBuyValue,
            BigDecimal totalSellValue,
            long sequenceNumber) {

        super(strategyId, "StrategyInstance", sequenceNumber);
        this.planId          = planId;
        this.strategyId      = strategyId;
        this.mode            = mode;
        this.triggerType     = triggerType;
        this.nlvAtTrigger    = nlvAtTrigger;
        this.itemCount       = itemCount;
        this.totalBuyValue   = totalBuyValue;
        this.totalSellValue  = totalSellValue;
    }
}
