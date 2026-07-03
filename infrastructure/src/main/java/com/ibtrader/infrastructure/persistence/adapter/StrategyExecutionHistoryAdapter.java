package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.strategy.StrategyExecutionHistory;
import com.ibtrader.domain.port.outbound.StrategyExecutionHistoryPort;
import com.ibtrader.infrastructure.persistence.entity.StrategyExecutionHistoryEntity;
import com.ibtrader.infrastructure.persistence.repository.StrategyExecutionHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StrategyExecutionHistoryAdapter implements StrategyExecutionHistoryPort {

    private final StrategyExecutionHistoryJpaRepository jpaRepository;

    @Override
    public void save(StrategyExecutionHistory history) {
        StrategyExecutionHistoryEntity entity = StrategyExecutionHistoryEntity.builder()
                .id(history.getId())
                .strategyId(history.getStrategyId())
                .executedAt(history.getExecutedAt())
                .successful(history.isSuccessful())
                .reason(history.getReason())
                .build();
        jpaRepository.save(entity);
    }

    @Override
    public Optional<StrategyExecutionHistory> findLastSuccessfulExecution(UUID strategyId) {
        return jpaRepository.findFirstByStrategyIdAndSuccessfulTrueOrderByExecutedAtDesc(strategyId)
                .map(entity -> StrategyExecutionHistory.builder()
                        .id(entity.getId())
                        .strategyId(entity.getStrategyId())
                        .executedAt(entity.getExecutedAt())
                        .successful(entity.isSuccessful())
                        .reason(entity.getReason())
                        .build());
    }
}
