package com.ibtrader.domain.engine;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Interface representing a provider for a technical indicator or derived analytic.
 */
public interface IndicatorProvider {
    
    /**
     * @return the name of the indicator (e.g., "SMA", "RSI")
     */
    String getIndicatorName();

    /**
     * Calculates the indicator for the given symbol and parameters.
     *
     * @param symbol the asset symbol
     * @param parameters configuration JSON string (e.g., period length)
     * @param context current evaluation context
     * @return the calculated value, or empty if calculation fails (e.g., lack of data)
     */
    Optional<BigDecimal> calculate(String symbol, String parameters, EvaluationContext context);
}
