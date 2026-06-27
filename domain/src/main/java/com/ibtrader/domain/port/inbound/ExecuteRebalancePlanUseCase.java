package com.ibtrader.domain.port.inbound;

import java.util.UUID;

/**
 * Inbound port (use case) for executing a previously generated {@code RebalancePlan}.
 *
 * <p>Execution involves iterating over the plan's line items and submitting the
 * corresponding orders to Interactive Brokers.  Sell-side orders are submitted before
 * buy-side orders to release liquidity.  Each submitted order is persisted and
 * tracked for fill callbacks.</p>
 *
 * <p>The plan is transitioned to {@code EXECUTING} status at the start and to
 * {@code COMPLETED} or {@code PARTIALLY_EXECUTED} at the end, depending on the
 * success rate of individual order submissions.</p>
 */
public interface ExecuteRebalancePlanUseCase {

    /**
     * Encapsulates the parameters required to execute a rebalance plan.
     *
     * @param planId the domain UUID of the {@code RebalancePlan} to execute;
     *               must not be {@code null}
     */
    record Command(UUID planId) {}

    /**
     * Executes the rebalance plan identified in the command.
     *
     * @param command the execution command; must not be {@code null}
     * @throws com.ibtrader.domain.exception.OrderNotFoundException    if the plan cannot be found
     * @throws com.ibtrader.domain.exception.RiskLimitViolatedException if a risk limit is breached
     *         for one of the plan's orders
     * @throws com.ibtrader.domain.exception.BrokerConnectionException      if IB is not reachable
     */
    void execute(Command command);
}
