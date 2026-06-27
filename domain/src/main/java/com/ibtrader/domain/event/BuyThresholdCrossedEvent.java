package com.ibtrader.domain.event;

import com.ibtrader.domain.model.strategy.StrategyMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event raised when the portfolio's Net Liquidation Value (NLV) falls at or
 * below the configured buy threshold for a given strategy, signalling that the
 * strategy should initiate purchasing activity.
 *
 * <p>Typical consumers include the rebalance orchestrator and the strategy evaluation
 * service, which will inspect the current {@link StrategyMode} and decide whether to
 * generate and execute a buy-side rebalance plan.</p>
 */
@Getter
public final class BuyThresholdCrossedEvent extends DomainEvent {

    /**
     * The domain identifier of the {@code StrategyInstance} whose buy threshold was crossed.
     */
    private final UUID strategyId;

    /**
     * The human-readable name of the strategy for logging and alerting purposes.
     */
    private final String strategyName;

    /**
     * The current NLV of the portfolio at the moment the threshold was detected.
     */
    private final BigDecimal currentNlv;

    /**
     * The buy threshold value configured on the strategy.  The event is raised when
     * {@code currentNlv} falls to or below this value.
     */
    private final BigDecimal buyThreshold;

    /**
     * The operational mode of the strategy at the time of the crossing
     * (e.g. {@code PAPER}, {@code LIVE}).
     */
    private final StrategyMode strategyMode;

    /**
     * Constructs a {@code BuyThresholdCrossedEvent} via its Lombok builder.
     *
     * @param strategyId     domain identifier of the strategy aggregate
     * @param strategyName   human-readable strategy name
     * @param currentNlv     portfolio NLV at detection time
     * @param buyThreshold   configured lower NLV trigger value
     * @param strategyMode   operational mode of the strategy
     * @param sequenceNumber monotonic sequence number scoped to the strategy aggregate
     */
    @Builder
    private BuyThresholdCrossedEvent(
            UUID strategyId,
            String strategyName,
            BigDecimal currentNlv,
            BigDecimal buyThreshold,
            StrategyMode strategyMode,
            long sequenceNumber) {

        super(strategyId, "StrategyInstance", sequenceNumber);
        this.strategyId    = strategyId;
        this.strategyName  = strategyName;
        this.currentNlv    = currentNlv;
        this.buyThreshold  = buyThreshold;
        this.strategyMode  = strategyMode;
    }
}
