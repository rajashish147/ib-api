package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.MarketDataCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketDataCacheJpaRepository extends JpaRepository<MarketDataCacheEntity, UUID> {
    List<MarketDataCacheEntity> findBySymbol(String symbol);
}
