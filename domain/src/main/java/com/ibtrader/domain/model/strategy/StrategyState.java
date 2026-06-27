package com.ibtrader.domain.model.strategy;

/**
 * Represents the current execution state of a trading strategy instance.
 *
 * <p>A strategy progresses through well-defined states as it detects signals, submits
 * orders, and receives execution confirmations. The state machine drives the platform's
 * strategy coordinator, allowing it to determine what action (if any) to take next and
 * to enforce single-instance-per-strategy-at-a-time execution semantics.
 *
 * <p><b>State machine overview:</b>
 * <pre>
 *   IDLE
 *    ├──► BUY_TRIGGERED ──► BUY_EXECUTING ──► BUY_COMPLETED ──► IDLE
 *    ├──► SELL_TRIGGERED ──► SELL_EXECUTING ──► SELL_COMPLETED ──► IDLE
 *    └──► ERROR ──► RECOVERY ──► IDLE
 * </pre>
 *
 * <pre>{@code
 * // Usage example
 * StrategyState state = strategy.getState();
 * if (state.isExecuting()) {
 *     log.info("Strategy {} is currently executing an order.", strategy.getId());
 * }
 * if (state.isError()) {
 *     alertService.notify("Strategy entered error state: " + strategy.getId());
 * }
 * }</pre>
 *
 * @author IBTrader Platform
 * @version 1.0
 * @since 1.0
 */
public enum StrategyState {

    /**
     * The strategy is idle — no signal has been detected and no order is in flight.
     * This is the default resting state between cycles.
     */
    IDLE,

    /**
     * A buy signal has been detected and validated; the strategy is preparing to submit
     * buy orders but has not yet sent them to IB.
     */
    BUY_TRIGGERED,

    /**
     * Buy orders have been submitted to IB and the strategy is waiting for fill
     * confirmations. The strategy coordinator will not re-evaluate signals in this state.
     */
    BUY_EXECUTING,

    /**
     * All buy orders have been confirmed as fully or sufficiently filled.
     * The strategy coordinator will transition back to {@link #IDLE} after post-execution
     * reconciliation (e.g., position updates, plan archival).
     */
    BUY_COMPLETED,

    /**
     * A sell signal has been detected and validated; the strategy is preparing to submit
     * sell orders but has not yet sent them to IB.
     */
    SELL_TRIGGERED,

    /**
     * Sell orders have been submitted to IB and the strategy is waiting for fill
     * confirmations. The strategy coordinator will not re-evaluate signals in this state.
     */
    SELL_EXECUTING,

    /**
     * All sell orders have been confirmed as fully or sufficiently filled.
     * The strategy coordinator will transition back to {@link #IDLE} after post-execution
     * reconciliation.
     */
    SELL_COMPLETED,

    /**
     * An unrecoverable or unexpected error occurred during signal evaluation, order
     * submission, or execution confirmation. Manual review or an automated recovery
     * procedure is required before the strategy can resume.
     */
    ERROR,

    /**
     * The strategy is in a supervised recovery process following an {@link #ERROR} state.
     * Recovery may involve re-syncing positions, cancelling stale orders, and re-aligning
     * internal state with the actual account state in IB. On successful recovery the
     * strategy transitions back to {@link #IDLE}.
     */
    RECOVERY;

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the strategy is currently executing orders (i.e., orders
     * have been submitted and are awaiting confirmation from IB).
     *
     * <p>Executing states: {@link #BUY_EXECUTING}, {@link #SELL_EXECUTING}.
     *
     * @return {@code true} if orders are in-flight; {@code false} otherwise
     */
    public boolean isExecuting() {
        return switch (this) {
            case BUY_EXECUTING, SELL_EXECUTING -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if the strategy has completed an execution cycle and is
     * awaiting post-execution cleanup before returning to {@link #IDLE}.
     *
     * <p>Completed states: {@link #BUY_COMPLETED}, {@link #SELL_COMPLETED}.
     *
     * @return {@code true} if execution has just been confirmed; {@code false} otherwise
     */
    public boolean isCompleted() {
        return switch (this) {
            case BUY_COMPLETED, SELL_COMPLETED -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if the strategy is in the {@link #ERROR} state.
     *
     * <p>Strategies in this state require manual or automated intervention before they
     * can resume normal operation.
     *
     * @return {@code true} for {@link #ERROR}; {@code false} otherwise
     */
    public boolean isError() {
        return this == ERROR;
    }

    /**
     * Returns {@code true} if the current state belongs to the buy side of the lifecycle
     * — i.e., the strategy has detected a buy signal or is actively buying.
     *
     * <p>Buy-side states: {@link #BUY_TRIGGERED}, {@link #BUY_EXECUTING},
     * {@link #BUY_COMPLETED}.
     *
     * @return {@code true} for buy-side states; {@code false} otherwise
     */
    public boolean isBuySide() {
        return switch (this) {
            case BUY_TRIGGERED, BUY_EXECUTING, BUY_COMPLETED -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if the current state belongs to the sell side of the lifecycle
     * — i.e., the strategy has detected a sell signal or is actively selling.
     *
     * <p>Sell-side states: {@link #SELL_TRIGGERED}, {@link #SELL_EXECUTING},
     * {@link #SELL_COMPLETED}.
     *
     * @return {@code true} for sell-side states; {@code false} otherwise
     */
    public boolean isSellSide() {
        return switch (this) {
            case SELL_TRIGGERED, SELL_EXECUTING, SELL_COMPLETED -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if the strategy is in the {@link #IDLE} resting state,
     * meaning it is actively evaluating signals but has no open orders.
     *
     * @return {@code true} for {@link #IDLE}; {@code false} otherwise
     */
    public boolean isIdle() {
        return this == IDLE;
    }
}
