package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.RuleActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleActionJpaRepository extends JpaRepository<RuleActionEntity, UUID> {
    List<RuleActionEntity> findByStrategyVersionId(UUID strategyVersionId);
}
