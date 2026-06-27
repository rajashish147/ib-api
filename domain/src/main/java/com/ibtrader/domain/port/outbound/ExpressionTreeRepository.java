package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.strategy.ExpressionNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpressionTreeRepository {
    ExpressionNode save(ExpressionNode node);
    Optional<ExpressionNode> findById(UUID id);
    Optional<ExpressionNode> findByStrategyId(UUID strategyId);
    List<ExpressionNode> findAll();
    void deleteById(UUID id);
}
