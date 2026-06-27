package com.ibtrader.domain.event;

import com.ibtrader.domain.model.strategy.StrategyState;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event raised whenever a {@code StrategyInstance} transitions from one
 * {@link StrategyState} to another.
 *
 * <p>Valid transitions are enforced by the domain aggregate; this event merely
 * communicates that a transition has already taken place.  Consumers such as
 * monitoring dashboards, audit writers, and conditional alert systems can use
 * {@code fromState} and {@code toState} to implement targeted reactions (e.g.
 * page an operator when a strategy transitions to {@code ERROR}).</p>
 */
@Getter
public final class StrategyStateTransitionedEvent extends DomainEvent {

    /**
     * The domain identifier of the {@code StrategyInstance} aggregate.
     */
    private final UUID strategyId;

    /**
     * The human-readable name of the strategy.
     */
    private final String strategyName;

    /**
     * The state the strategy was in before the transition.
     */
    private final StrategyState fromState;

    /**
     * The state the strategy entered as a result of the transition.
     */
    private final StrategyState toState;

    /**
     * A human-readable explanation of why the transition occurred, for audit and
     * operational visibility (e.g. {@code "User requested deactivation"},
     * {@code "Risk limit breached"}).
     */
    private final String reason;

    /**
     * Constructs a {@code StrategyStateTransitionedEvent} via its Lombok builder.
     *
     * @param strategyId     domain identifier of the strategy aggregate
     * @param strategyName   human-readable strategy name
     * @param fromState      state prior to transition
     * @param toState        state after transition
     * @param reason         human-readable reason for the transition
     * @param sequenceNumber monotonic sequence number scoped to the strategy aggregate
     */
    @Builder
    private StrategyStateTransitionedEvent(
            UUID strategyId,
            String strategyName,
            StrategyState fromState,
            StrategyState toState,
            String reason,
            long sequenceNumber) {

        super(strategyId, "StrategyInstance", sequenceNumber);
        this.strategyId    = strategyId;
        this.strategyName  = strategyName;
        this.fromState     = fromState;
        this.toState       = toState;
        this.reason        = reason;
    }
}
