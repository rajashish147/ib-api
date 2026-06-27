package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.strategy.RebalancePlan;
import com.ibtrader.domain.model.strategy.TriggerType;

import java.util.UUID;

/**
 * Inbound port (use case) for generating a {@link RebalancePlan} for a strategy.
 *
 * <p>Plan generation involves:</p>
 * <ol>
 *   <li>Loading the strategy's {@link com.ibtrader.domain.model.strategy.AllocationTarget}s.</li>
 *   <li>Fetching current portfolio positions and NLV.</li>
 *   <li>Computing the required buy/sell quantities to bring each position in line
 *       with its target allocation.</li>
 *   <li>Persisting the generated plan in {@code PENDING_EXECUTION} status.</li>
 *   <li>Publishing a {@link com.ibtrader.domain.event.RebalancePlanGeneratedEvent}.</li>
 * </ol>
 *
 * <p>The plan is not executed automatically by this use case; execution is a
 * separate step handled by {@link ExecuteRebalancePlanUseCase}.</p>
 */
public interface GenerateRebalancePlanUseCase {

    /**
     * Encapsulates the parameters required to generate a rebalance plan.
     *
     * @param strategyId  the domain UUID of the strategy for which the plan is generated;
     *                    must not be {@code null}
     * @param accountId   the IB account string; must not be blank
     * @param triggerType the event that caused the plan to be generated; must not be {@code null}
     */
    record Command(UUID strategyId, String accountId, TriggerType triggerType) {}

    /**
     * Executes the plan-generation use case.
     *
     * @param command the generation command; must not be {@code null}
     * @return the generated and persisted {@link RebalancePlan}; never {@code null}
     * @throws com.ibtrader.domain.exception.AllocationException          if allocation targets are invalid
     * @throws com.ibtrader.domain.exception.AssetNotFoundException        if a target asset is not registered
     * @throws com.ibtrader.domain.exception.DomainException               if the strategy is not enabled
     */
    RebalancePlan execute(Command command);
}
