package com.ibtrader.domain.model.strategy;

/**
 * Represents the execution mode used by a strategy when calculating and placing orders.
 *
 * <p>The strategy mode determines the specific order-sizing algorithm that is applied when a
 * trigger event is detected. It is a per-strategy configuration parameter that can be changed
 * independently of the trigger type ({@link TriggerType}).
 *
 * <p>Unlike {@link StrategyType}, which classifies the overarching strategy logic model,
 * {@code StrategyMode} specifically governs <em>how much</em> to buy or sell when a signal
 * fires — the sizing and allocation methodology.
 *
 * <p>The {@link #getDescription()} method returns a detailed, human-readable explanation
 * of each mode's behaviour, suitable for configuration UIs and documentation.
 *
 * <pre>{@code
 * // Usage example
 * StrategyMode mode = StrategyMode.HYBRID;
 * log.info("Executing in '{}' mode: {}", mode.name(), mode.getDescription());
 * }</pre>
 *
 * @author IBTrader Platform
 * @version 1.0
 * @since 1.0
 * @see StrategyType
 * @see TriggerType
 */
public enum StrategyMode {

    /**
     * Full portfolio rebalance mode.
     *
     * <p>On trigger, the strategy calculates the target quantity for every configured asset
     * based on its target allocation percentage and the total portfolio NLV. Buy and sell
     * orders are generated for all assets that deviate from their targets beyond the
     * configured drift tolerance. All available cash is deployed proportionally.
     *
     * <p>This mode provides the tightest adherence to target allocations but requires
     * sufficient liquid cash to fund the full rebalance in a single pass.
     */
    FULL_REBALANCE("Rebalance entire portfolio to target allocation percentages. "
            + "Uses all available cash proportionally."),

    /**
     * Fixed dollar amount per asset mode.
     *
     * <p>On trigger, the strategy purchases or sells a pre-configured fixed dollar amount
     * for each asset, independently of the current portfolio allocation or total NLV.
     * No cross-asset proportional sizing is performed; each asset is treated in isolation.
     *
     * <p>This mode is well-suited for dollar-cost averaging strategies where consistent,
     * predictable investment increments are preferred over allocation precision.
     */
    FIXED_AMOUNT("Buy/sell a fixed dollar amount per asset, regardless of current allocation."),

    /**
     * Hybrid rebalance-with-fixed-amount-fallback mode.
     *
     * <p>On trigger, the strategy first attempts a full proportional rebalance
     * (as in {@link #FULL_REBALANCE}). If the available cash balance is insufficient
     * to fund all required buy orders at the target proportional amounts, the strategy
     * falls back to investing a fixed dollar amount per eligible asset (as in
     * {@link #FIXED_AMOUNT}).
     *
     * <p>This mode balances allocation precision with capital efficiency, ensuring that
     * some deployment always occurs even when cash is limited, while still preferring
     * accurate rebalancing when cash is plentiful.
     */
    HYBRID("Attempt full rebalance first; fall back to fixed-amount per asset "
            + "if insufficient cash for full rebalance.");

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * A detailed human-readable description of this strategy mode's behaviour.
     */
    private final String description;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a {@code StrategyMode} constant with the given description.
     *
     * @param description a non-null, non-empty human-readable description of the mode
     */
    StrategyMode(final String description) {
        this.description = description;
    }

    // -------------------------------------------------------------------------
    // Accessor
    // -------------------------------------------------------------------------

    /**
     * Returns a detailed human-readable description of this strategy mode's behaviour.
     *
     * <p>This description is suitable for display in configuration screens, reports, and
     * inline documentation, and is intentionally more verbose than the enum name alone.
     *
     * @return the non-null description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns a human-readable representation including the enum name and description.
     *
     * @return string in the format {@code "HYBRID: Attempt full rebalance first; ..."}
     */
    @Override
    public String toString() {
        return name() + ": " + description;
    }
}
