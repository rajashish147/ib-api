package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.ExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutionJpaRepository extends JpaRepository<ExecutionEntity, UUID> {
    List<ExecutionEntity> findByOrderId(UUID orderId);
    Optional<ExecutionEntity> findByExecId(String execId);
}
