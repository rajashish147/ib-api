package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.ExecutionPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutionPolicyJpaRepository extends JpaRepository<ExecutionPolicyEntity, UUID> {
    List<ExecutionPolicyEntity> findByStrategyVersionId(UUID strategyVersionId);
}
