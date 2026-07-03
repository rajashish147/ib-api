package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.AllocationTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllocationTargetJpaRepository extends JpaRepository<AllocationTargetEntity, UUID> {
    List<AllocationTargetEntity> findByStrategyId(UUID strategyId);
    void deleteByStrategyId(UUID strategyId);
}
