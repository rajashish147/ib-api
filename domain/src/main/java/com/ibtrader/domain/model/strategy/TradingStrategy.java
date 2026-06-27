package com.ibtrader.domain.model.strategy;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Represents a Trading Strategy configuration from the database.
 * Strategies have versions and contain execution metadata.
 */
@Getter
@Builder
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

}
