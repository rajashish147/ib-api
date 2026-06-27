package com.ibtrader.domain.model.strategy;

/**
 * Represents the high-level algorithmic type of a trading strategy.
 *
 * <p>The strategy type describes the overarching logic model used to determine when and
 * how to trade. It is a configuration-level classification that drives which signal
 * evaluator and order sizing algorithm are selected at runtime.
 *
 * <p>The {@link #getDescription()} method returns a concise, human-readable explanation
 * of each strategy type suitable for display in configuration UIs and audit logs.
 *
 * <pre>{@code
 * // Usage example
 * StrategyType type = StrategyType.HYBRID;
 * log.info("Activating strategy type '{}': {}", type.name(), type.getDescription());
 * }</pre>
 *
 * @author IBTrader Platform
 * @version 1.0
 * @since 1.0
 */
public enum StrategyType {

    /**
     * Portfolio threshold-triggered rebalance strategy.
     *
     * <p>Monitors the portfolio's Net Liquidation Value (NLV) against configurable buy and
     * sell thresholds. When NLV crosses a threshold boundary, the strategy triggers a full
     * or partial rebalance of the portfolio back to target allocation percentages.
     *
     * <p>This is the primary strategy type for systematic, allocation-driven portfolios.
     */
    THRESHOLD_REBALANCE("Portfolio threshold-triggered rebalance"),

    /**
     * Fixed dollar-amount-per-asset strategy.
     *
     * <p>On each trigger event, the strategy purchases or sells a fixed dollar amount of
     * each configured asset, regardless of the current portfolio allocation or NLV.
     *
     * <p>Suitable for dollar-cost-averaging (DCA) approaches where consistent periodic
     * investment amounts are more important than maintaining precise allocation ratios.
     */
    FIXED_AMOUNT("Fixed dollar amount per asset"),

    /**
     * Hybrid threshold-rebalance-with-fixed-amount-fallback strategy.
     *
     * <p>Combines elements of both {@link #THRESHOLD_REBALANCE} and {@link #FIXED_AMOUNT}.
     * When a threshold is crossed, the strategy first attempts a full proportional rebalance.
     * If the available cash is insufficient to fund a full rebalance, it falls back to
     * investing a fixed dollar amount per asset.
     *
     * <p>This type provides the precision of rebalancing with the resilience of fixed-amount
     * investing when capital is constrained.
     */
    HYBRID("Threshold-triggered with fixed-amount allocation");

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * A concise human-readable description of this strategy type's behaviour.
     */
    private final String description;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a {@code StrategyType} constant with the given description.
     *
     * @param description a non-null, non-empty human-readable description of the strategy type
     */
    StrategyType(final String description) {
        this.description = description;
    }

    // -------------------------------------------------------------------------
    // Accessor
    // -------------------------------------------------------------------------

    /**
     * Returns a concise human-readable description of this strategy type.
     *
     * <p>This description is suitable for display in configuration screens, reports, and
     * audit log entries.
     *
     * @return the non-null description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns a human-readable representation including the enum name and description.
     *
     * @return string in the format {@code "HYBRID: Threshold-triggered with fixed-amount allocation"}
     */
    @Override
    public String toString() {
        return name() + ": " + description;
    }
}
