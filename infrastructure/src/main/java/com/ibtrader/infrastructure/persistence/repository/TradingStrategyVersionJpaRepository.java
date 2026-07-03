package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.TradingStrategyVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradingStrategyVersionJpaRepository extends JpaRepository<TradingStrategyVersionEntity, UUID> {
    List<TradingStrategyVersionEntity> findByStrategyId(UUID strategyId);
    Optional<TradingStrategyVersionEntity> findFirstByStrategyIdOrderByVersionNumberDesc(UUID strategyId);
}
