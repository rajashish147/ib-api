package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioSnapshotRepository {
    PortfolioSnapshot save(PortfolioSnapshot snapshot);
    Optional<PortfolioSnapshot> findById(UUID id);
    List<PortfolioSnapshot> findAll();
    void deleteById(UUID id);
}
