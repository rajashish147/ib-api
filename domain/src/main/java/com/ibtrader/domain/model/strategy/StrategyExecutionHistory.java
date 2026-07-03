package com.ibtrader.domain.model.strategy;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the execution history of a Trading Strategy.
 * Used to enforce cooldown periods and limit trade frequencies.
 */
@Getter
@Builder
public class StrategyExecutionHistory {

    private final UUID id;
    private final UUID strategyId;
    private final Instant executedAt;
    private final boolean successful;
    private final String reason;

    public static StrategyExecutionHistory create(UUID strategyId, boolean successful, String reason) {
        return StrategyExecutionHistory.builder()
                .id(UUID.randomUUID())
                .strategyId(strategyId)
                .executedAt(Instant.now())
                .successful(successful)
                .reason(reason)
                .build();
    }
}
