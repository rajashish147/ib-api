package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradingStrategy;

/**
 * Factory for creating rich evaluation contexts needed by the strategy engines.
 */
public interface EvaluationContextFactory {

    /**
     * Builds an evaluation context for the given strategy and account.
     *
     * @param strategy  the strategy to evaluate
     * @param accountId the account ID representing the portfolio
     * @return a fully populated EvaluationContext
     */
    EvaluationContext create(TradingStrategy strategy, String accountId);
}
