package com.ibtrader.domain.engine;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Interface for dynamically resolving variables used in Expression Trees.
 * Variables can be portfolio metrics, account metrics, market data, or technical indicators.
 */
public interface VariableRegistry {

    /**
     * Resolves a variable name into a numeric value based on the current evaluation context.
     *
     * @param variableName the name of the variable (e.g., "PortfolioValue", "AAPL.Close", "SMA_50_AAPL")
     * @param context      the current evaluation context
     * @return the resolved numeric value, or empty if unresolved
     */
    Optional<BigDecimal> resolve(String variableName, EvaluationContext context);

}
