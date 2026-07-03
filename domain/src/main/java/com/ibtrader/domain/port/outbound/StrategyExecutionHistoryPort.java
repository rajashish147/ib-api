package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.strategy.StrategyExecutionHistory;

import java.util.Optional;
import java.util.UUID;

public interface StrategyExecutionHistoryPort {
    void save(StrategyExecutionHistory history);
    Optional<StrategyExecutionHistory> findLastSuccessfulExecution(UUID strategyId);
}
