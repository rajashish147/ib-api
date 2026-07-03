package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.PortfolioSnapshotEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioSnapshotJpaRepository extends JpaRepository<PortfolioSnapshotEntity, UUID> {
    Optional<PortfolioSnapshotEntity> findFirstByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);
    Optional<PortfolioSnapshotEntity> findFirstByAccountIdOrderByCreatedAtDesc(String accountId);
    List<PortfolioSnapshotEntity> findByAccountIdOrderByCapturedAtDesc(String accountId, Pageable pageable);
}
