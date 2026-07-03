package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.strategy.RuleAction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleRepository {
    RuleAction save(RuleAction rule);
    Optional<RuleAction> findById(UUID id);
    List<RuleAction> findByStrategyId(UUID strategyId);
    List<RuleAction> findAll();
    void deleteById(UUID id);
}
