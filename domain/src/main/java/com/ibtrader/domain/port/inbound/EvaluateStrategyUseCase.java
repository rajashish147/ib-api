package com.ibtrader.domain.port.inbound;

import java.util.UUID;

/**
 * Inbound port (use case) for evaluating a single {@code StrategyInstance} against
 * the current portfolio state and market conditions.
 *
 * <p>Evaluation typically includes:</p>
 * <ol>
 *   <li>Fetching the latest portfolio snapshot for the strategy's account.</li>
 *   <li>Comparing the portfolio NLV against the strategy's buy and sell thresholds.</li>
 *   <li>Publishing threshold-crossed events if applicable.</li>
 *   <li>Generating a rebalance plan if an automatic trigger is detected.</li>
 * </ol>
 *
 * <p>This use case is invoked by the scheduler on each evaluation cycle and can
 * also be triggered manually via the administration API.</p>
 */
public interface EvaluateStrategyUseCase {

    /**
     * Encapsulates the parameters required to evaluate a strategy.
     *
     * @param strategyId the domain UUID of the strategy to evaluate; must not be {@code null}
     * @param accountId  the IB account string to evaluate the strategy against; must not be blank
     */
    record Command(UUID strategyId, String accountId) {}

    /**
     * Executes the strategy evaluation use case.
     *
     * @param command the evaluation command; must not be {@code null}
     * @throws com.ibtrader.domain.exception.AssetNotFoundException if a configured
     *         allocation target references an unknown asset
     * @throws com.ibtrader.domain.exception.DomainException if the strategy is not in
     *         a valid state for evaluation
     */
    void execute(Command command);
}
