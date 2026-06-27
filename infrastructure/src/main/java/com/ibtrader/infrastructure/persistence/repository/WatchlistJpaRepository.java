package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.WatchlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WatchlistJpaRepository extends JpaRepository<WatchlistEntity, UUID> {
    List<WatchlistEntity> findByEnabledTrue();
    List<WatchlistEntity> findByEnabled(boolean enabled);
}
