package com.ibtrader.domain.exception;

import com.ibtrader.domain.model.strategy.StrategyState;

import java.util.UUID;

/**
 * Domain exception thrown when an attempt is made to activate or start a
 * {@code StrategyInstance} that is already in an active or running state.
 *
 * <p>This exception guards the strategy lifecycle state machine: a strategy in
 * {@code ACTIVE} or {@code RUNNING} state cannot be activated again without first
 * transitioning through a valid intermediate state such as {@code PAUSED} or
 * {@code STOPPED}.</p>
 */
public final class StrategyAlreadyActiveException extends DomainException {

    /** Stable machine-readable error code for this exception class. */
    public static final String ERROR_CODE = "STRATEGY_ALREADY_ACTIVE";

    /**
     * The domain identifier of the strategy that is already active.
     */
    private final UUID strategyId;

    /**
     * The human-readable name of the strategy.
     */
    private final String strategyName;

    /**
     * The current state of the strategy at the time the exception was raised.
     */
    private final StrategyState currentState;

    /**
     * Constructs a {@code StrategyAlreadyActiveException}.
     *
     * @param strategyId   the UUID of the already-active strategy; must not be {@code null}
     * @param strategyName the human-readable name of the strategy; must not be blank
     * @param currentState the current lifecycle state of the strategy; must not be {@code null}
     */
    public StrategyAlreadyActiveException(UUID strategyId, String strategyName, StrategyState currentState) {
        super(ERROR_CODE,
              String.format("Strategy '%s' [%s] is already active (current state: %s). "
                          + "Transition to an inactive state before re-activating.",
                            strategyName, strategyId, currentState));
        this.strategyId   = strategyId;
        this.strategyName = strategyName;
        this.currentState = currentState;
    }

    /**
     * Returns the domain UUID of the strategy that is already active.
     *
     * @return the strategy UUID
     */
    public UUID getStrategyId() {
        return strategyId;
    }

    /**
     * Returns the human-readable name of the strategy.
     *
     * @return the strategy name
     */
    public String getStrategyName() {
        return strategyName;
    }

    /**
     * Returns the current lifecycle state of the strategy.
     *
     * @return the current {@link StrategyState}
     */
    public StrategyState getCurrentState() {
        return currentState;
    }
}
