package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.WatchlistSymbolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WatchlistSymbolJpaRepository extends JpaRepository<WatchlistSymbolEntity, UUID> {
    List<WatchlistSymbolEntity> findByWatchlistId(UUID watchlistId);
}
