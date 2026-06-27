package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.strategy.TradingStrategy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StrategyRepository {
    TradingStrategy save(TradingStrategy strategy);
    Optional<TradingStrategy> findById(UUID id);
    List<TradingStrategy> findActiveStrategies();
    List<TradingStrategy> findAll();
    void deleteById(UUID id);
}
