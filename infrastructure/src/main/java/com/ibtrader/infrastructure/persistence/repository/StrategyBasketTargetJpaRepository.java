package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.StrategyBasketTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StrategyBasketTargetJpaRepository extends JpaRepository<StrategyBasketTargetEntity, UUID> {
    List<StrategyBasketTargetEntity> findByStrategyId(UUID strategyId);
    void deleteByStrategyId(UUID strategyId);
}
