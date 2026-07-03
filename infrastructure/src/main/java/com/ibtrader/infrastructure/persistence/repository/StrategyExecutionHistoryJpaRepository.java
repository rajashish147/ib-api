package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.StrategyExecutionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StrategyExecutionHistoryJpaRepository extends JpaRepository<StrategyExecutionHistoryEntity, UUID> {
    Optional<StrategyExecutionHistoryEntity> findFirstByStrategyIdAndSuccessfulTrueOrderByExecutedAtDesc(UUID strategyId);
}
