package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.PositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PositionJpaRepository extends JpaRepository<PositionEntity, UUID> {
    List<PositionEntity> findByPortfolioId(UUID portfolioId);
}
