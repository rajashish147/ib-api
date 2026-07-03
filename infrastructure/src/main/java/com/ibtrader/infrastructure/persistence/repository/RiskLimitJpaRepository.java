package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.RiskLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskLimitJpaRepository extends JpaRepository<RiskLimitEntity, UUID> {
    Optional<RiskLimitEntity> findByLimitType(String limitType);
    List<RiskLimitEntity> findByEnabledTrue();
}
