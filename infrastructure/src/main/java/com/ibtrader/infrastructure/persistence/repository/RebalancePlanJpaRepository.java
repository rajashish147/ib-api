package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.RebalancePlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RebalancePlanJpaRepository extends JpaRepository<RebalancePlanEntity, UUID> {
    
    List<RebalancePlanEntity> findByStrategyIdOrderByCreatedAtDesc(UUID strategyId);

    Optional<RebalancePlanEntity> findFirstByStrategyIdOrderByCreatedAtDesc(UUID strategyId);
}
