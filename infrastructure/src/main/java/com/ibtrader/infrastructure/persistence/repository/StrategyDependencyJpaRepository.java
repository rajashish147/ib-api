package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.StrategyDependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StrategyDependencyJpaRepository extends JpaRepository<StrategyDependencyEntity, UUID> {
    List<StrategyDependencyEntity> findByParentStrategyId(UUID parentStrategyId);
}
