package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.strategy.TradingStrategy;
import java.util.List;
import java.util.UUID;

public interface ManageStrategyUseCase {
    List<TradingStrategy> getActiveStrategies();
    List<TradingStrategy> getAllStrategies();
    TradingStrategy getStrategyById(UUID id);
    TradingStrategy createStrategy(TradingStrategy strategy);
    TradingStrategy updateStrategy(UUID id, TradingStrategy strategy);
    TradingStrategy toggleStrategy(UUID id, boolean enabled);
    void deleteStrategy(UUID id);
}
