package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.ExpressionNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpressionNodeJpaRepository extends JpaRepository<ExpressionNodeEntity, UUID> {
    List<ExpressionNodeEntity> findByStrategyVersionId(UUID strategyVersionId);
}
