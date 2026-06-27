package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.EvaluationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationHistoryJpaRepository extends JpaRepository<EvaluationHistoryEntity, UUID> {
    List<EvaluationHistoryEntity> findByStrategyId(UUID strategyId);
}
