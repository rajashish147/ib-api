package com.ibtrader.domain.model.strategy;

/**
 * Represents the type of event that triggers a strategy execution cycle.
 *
 * <p>A {@code TriggerType} identifies <em>why</em> a strategy was activated — the root
 * cause of the signal that initiated order evaluation and placement. It is recorded on
 * every execution plan and order batch for auditability and post-trade analysis.
 *
 * <p>The trigger type is distinct from the strategy mode ({@link StrategyMode}), which
 * governs <em>how</em> orders are sized after a trigger fires, and from the strategy type
 * ({@link StrategyType}), which governs the overarching logic model.
 *
 * <p>The {@link #getDescription()} method provides a concise, human-readable explanation
 * of each trigger type suitable for audit logs, notification messages, and UI display.
 *
 * <pre>{@code
 * // Usage example — recording the trigger on an execution plan
 * TriggerType trigger = TriggerType.BUY_THRESHOLD;
 * ExecutionPlan plan = ExecutionPlan.builder()
 *         .triggerType(trigger)
 *         .triggerDescription(trigger.getDescription())
 *         .build();
 * }</pre>
 *
 * @author IBTrader Platform
 * @version 1.0
 * @since 1.0
 * @see StrategyMode
 * @see StrategyType
 */
public enum TriggerType {

    /**
     * Triggered because the portfolio's Net Liquidation Value (NLV) crossed below the
     * configured buy threshold.
     *
     * <p>This typically indicates that the portfolio has declined in value (or received
     * new cash) to a point where buying additional assets is warranted to bring allocation
     * back toward target levels.
     */
    BUY_THRESHOLD("Portfolio NLV crossed below buy threshold"),

    /**
     * Triggered because the portfolio's Net Liquidation Value (NLV) crossed above the
     * configured sell threshold.
     *
     * <p>This typically indicates that the portfolio has grown to a point where trimming
     * positions is warranted to reduce risk exposure or lock in gains.
     */
    SELL_THRESHOLD("Portfolio NLV crossed above sell threshold"),

    /**
     * Triggered by a scheduled time-based event (e.g., a daily, weekly, or monthly
     * rebalance schedule configured via cron expression or calendar).
     *
     * <p>Scheduled triggers fire regardless of current NLV or threshold levels, ensuring
     * that the portfolio is periodically reviewed and rebalanced.
     */
    SCHEDULED("Scheduled rebalance"),

    /**
     * Triggered manually by an authorised user via the platform's management interface
     * or API.
     *
     * <p>Manual triggers provide an override mechanism for operators who need to force an
     * immediate rebalance outside of normal scheduled or threshold-driven cycles (e.g.,
     * in response to market events or operational requirements).
     */
    MANUAL("Manually triggered by user");

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * A concise human-readable description of what caused this trigger type to fire.
     */
    private final String description;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a {@code TriggerType} constant with the given description.
     *
     * @param description a non-null, non-empty human-readable description of the trigger event
     */
    TriggerType(final String description) {
        this.description = description;
    }

    // -------------------------------------------------------------------------
    // Accessor
    // -------------------------------------------------------------------------

    /**
     * Returns a concise human-readable description of this trigger type.
     *
     * <p>Suitable for inclusion in audit logs, notification messages, execution plan
     * records, and UI display.
     *
     * @return the non-null description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns a human-readable representation including the enum name and description.
     *
     * @return string in the format {@code "BUY_THRESHOLD: Portfolio NLV crossed below buy threshold"}
     */
    @Override
    public String toString() {
        return name() + ": " + description;
    }
}
