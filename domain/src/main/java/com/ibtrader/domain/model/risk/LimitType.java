package com.ibtrader.domain.model.risk;

/**
 * Represents the type of a risk limit enforced by the platform's risk management system.
 *
 * <p>Risk limits are configurable guardrails that constrain the strategy's order generation
 * and position accumulation. Before any execution plan is approved, the risk engine evaluates
 * each proposed order batch against all active {@code LimitType} rules. If any limit would be
 * breached, the plan is rejected or reduced accordingly.
 *
 * <p>Each {@code LimitType} carries a human-readable description and a flag indicating whether
 * the limit value is expressed as a percentage of portfolio value ({@link #isPercentage()})
 * or as an absolute dollar amount.
 *
 * <p><b>Percentage limits</b> are stored as decimal fractions in the range {@code [0.0, 1.0]}
 * (e.g., {@code 0.05} for 5%). The sole exception is {@link #EMERGENCY_STOP_NLV}, which
 * holds an absolute dollar floor.
 *
 * <pre>{@code
 * // Usage example — risk evaluation
 * for (RiskLimit limit : riskLimits) {
 *     if (limit.getLimitType().isPercentage()) {
 *         double limitValue = limit.getValue(); // e.g. 0.20 = 20% of portfolio
 *         double exposure   = position.getMarketValue() / portfolioNlv;
 *         if (exposure > limitValue) riskEngine.reject(plan, limit);
 *     } else {
 *         // absolute dollar check
 *         if (portfolioNlv < limit.getValue()) riskEngine.emergencyStop();
 *     }
 * }
 * }</pre>
 *
 * @author IBTrader Platform
 * @version 1.0
 * @since 1.0
 */
public enum LimitType {

    /**
     * Maximum single-position size as a percentage of total portfolio value.
     *
     * <p>Prevents over-concentration in any single instrument. For example, a limit of
     * {@code 0.20} (20%) means no single position may exceed 20% of the portfolio's NLV.
     *
     * <p><b>Unit:</b> percentage (decimal fraction, e.g., {@code 0.20} = 20%)
     */
    MAX_POSITION_SIZE_PCT("Maximum single position size as % of portfolio"),

    /**
     * Maximum total futures exposure as a percentage of total portfolio value.
     *
     * <p>Caps the aggregate notional value of all open futures positions relative to the
     * portfolio's NLV, limiting leverage introduced through leveraged derivative instruments.
     *
     * <p><b>Unit:</b> percentage (decimal fraction, e.g., {@code 0.30} = 30%)
     */
    MAX_FUTURES_EXPOSURE_PCT("Maximum total futures exposure as % of portfolio"),

    /**
     * Maximum portfolio leverage ratio.
     *
     * <p>The leverage ratio is defined as the sum of all position notional values divided
     * by the portfolio's NLV. A ratio of {@code 1.0} represents no leverage (fully cash-funded).
     * Values above {@code 1.0} indicate the use of margin or derivatives amplification.
     *
     * <p><b>Unit:</b> percentage / ratio (e.g., {@code 2.0} = 2× leverage)
     */
    MAX_LEVERAGE("Maximum portfolio leverage ratio"),

    /**
     * Maximum daily loss as a percentage of the portfolio's starting value for the day.
     *
     * <p>If cumulative realised and unrealised losses since the trading day's open exceed
     * this threshold, the risk engine halts further order generation for the remainder of
     * the session.
     *
     * <p><b>Unit:</b> percentage (decimal fraction, e.g., {@code 0.02} = 2% daily loss cap)
     */
    MAX_DAILY_LOSS_PCT("Maximum daily loss as % of starting portfolio value"),

    /**
     * Maximum drawdown from the portfolio's peak value, expressed as a percentage of the peak.
     *
     * <p>Drawdown is calculated as {@code (peakNlv - currentNlv) / peakNlv}. If this ratio
     * exceeds the configured limit, the risk engine pauses the strategy until the drawdown
     * recovers or an operator overrides the pause.
     *
     * <p><b>Unit:</b> percentage (decimal fraction, e.g., {@code 0.10} = 10% drawdown cap)
     */
    MAX_DRAWDOWN_PCT("Maximum drawdown from peak as % of peak value"),

    /**
     * Maximum single-asset concentration as a percentage of total portfolio value.
     *
     * <p>Similar to {@link #MAX_POSITION_SIZE_PCT}, but evaluated across all open positions
     * in the same underlying asset (e.g., combining spot, futures, and options on the same
     * ticker). Prevents hidden concentration built up through multiple instrument types.
     *
     * <p><b>Unit:</b> percentage (decimal fraction, e.g., {@code 0.25} = 25%)
     */
    MAX_CONCENTRATION_PCT("Maximum single asset concentration as % of portfolio"),

    /**
     * Maximum single-sector concentration as a percentage of total portfolio value.
     *
     * <p>Ensures that no single GICS or platform-defined sector exceeds the configured
     * proportion of the portfolio, enforcing sector diversification constraints.
     *
     * <p><b>Unit:</b> percentage (decimal fraction, e.g., {@code 0.40} = 40%)
     */
    MAX_SECTOR_CONCENTRATION_PCT("Maximum single sector concentration as % of portfolio"),

    /**
     * Emergency stop trigger — halts all trading if the portfolio's NLV falls below this
     * absolute dollar floor.
     *
     * <p>This is a hard stop designed to prevent catastrophic capital destruction. When the
     * portfolio NLV drops below the configured dollar amount, the platform immediately
     * cancels all open orders and suspends the strategy until manually resumed by an operator.
     *
     * <p><b>Unit:</b> absolute dollar amount (e.g., {@code 50000.00} = $50,000 floor)
     * <p><b>Note:</b> This is the only {@code LimitType} for which {@link #isPercentage()}
     * returns {@code false}.
     */
    EMERGENCY_STOP_NLV("Emergency stop if NLV falls below this absolute dollar amount");

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * A human-readable description of this limit type, explaining what portfolio dimension
     * it constrains and how the limit value is interpreted.
     */
    private final String description;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a {@code LimitType} constant with the given description.
     *
     * @param description a non-null, non-empty human-readable description of the limit
     */
    LimitType(final String description) {
        this.description = description;
    }

    // -------------------------------------------------------------------------
    // Accessor
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable description of this limit type.
     *
     * <p>The description explains what portfolio dimension is being constrained and how
     * the limit value should be interpreted (percentage vs. absolute dollar amount).
     *
     * @return the non-null description string
     */
    public String getDescription() {
        return description;
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this limit type's value is expressed as a percentage
     * (decimal fraction) of the portfolio's NLV or peak value.
     *
     * <p>All limit types are percentage-based <em>except</em> {@link #EMERGENCY_STOP_NLV},
     * which holds an absolute dollar floor.
     *
     * <p><b>Percentage-based limits</b> store values in the range {@code [0.0, 1.0]} where
     * {@code 1.0} = 100% of portfolio. For example:
     * <ul>
     *   <li>{@link #MAX_POSITION_SIZE_PCT} with value {@code 0.20} = 20% max position size</li>
     *   <li>{@link #MAX_DAILY_LOSS_PCT} with value {@code 0.02} = 2% daily loss cap</li>
     * </ul>
     *
     * <p>Exception: {@link #MAX_LEVERAGE} stores a ratio (e.g., {@code 2.0} = 2× leverage)
     * rather than a strict 0–1 fraction, but is still considered a proportional/percentage
     * type rather than an absolute dollar amount.
     *
     * @return {@code true} for all constants except {@link #EMERGENCY_STOP_NLV};
     *         {@code false} for {@link #EMERGENCY_STOP_NLV}
     */
    public boolean isPercentage() {
        return this != EMERGENCY_STOP_NLV;
    }

    /**
     * Returns a human-readable representation including the enum name, description,
     * and whether the value is a percentage.
     *
     * @return string in the format
     *         {@code "MAX_POSITION_SIZE_PCT[Maximum single position size as % of portfolio, pct=true]"}
     */
    @Override
    public String toString() {
        return name() + "[" + description + ", pct=" + isPercentage() + "]";
    }
}
