package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.PortfolioGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PortfolioGoalJpaRepository extends JpaRepository<PortfolioGoalEntity, UUID> {
    List<PortfolioGoalEntity> findByEnabled(boolean enabled);
}
