package com.ibtrader.domain.model.strategy;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a Trading Strategy configuration from the database.
 * Strategies have versions and contain execution metadata.
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class TradingStrategy {

    private final UUID id;
    private final UUID versionId;
    private final String name;
    private final String description;
    private final int priority;
    private final boolean enabled;
    private final int cooldownMinutes;
    private final String riskProfile;
    
    // LIVE, PAPER, BACKTEST, SIMULATION, DISABLED
    private final String executionMode;
    
    private final BigDecimal buyThreshold;
    private final BigDecimal sellThreshold;
    private final StrategyState state;
    private final java.util.List<BasketTarget> targets;
    
    // Helper to transition state safely
    public TradingStrategy withState(StrategyState newState) {
        return TradingStrategy.builder()
                .id(id).versionId(versionId).name(name).description(description)
                .priority(priority).enabled(enabled).cooldownMinutes(cooldownMinutes)
                .riskProfile(riskProfile).executionMode(executionMode)
                .buyThreshold(buyThreshold).sellThreshold(sellThreshold)
                .state(newState).targets(targets).build();
    }

}
