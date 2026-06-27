package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.TradingStrategyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradingStrategyJpaRepository extends JpaRepository<TradingStrategyEntity, UUID> {
    Optional<TradingStrategyEntity> findByName(String name);
    List<TradingStrategyEntity> findByEnabledTrue();
    List<TradingStrategyEntity> findByEnabled(boolean enabled);
}
