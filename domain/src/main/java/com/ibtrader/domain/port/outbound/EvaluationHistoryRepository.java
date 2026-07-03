package com.ibtrader.domain.port.outbound;

import java.util.List;
import java.util.Optional;

public interface EvaluationHistoryRepository<T> {
    T save(T history);
    Optional<T> findById(String id);
    List<T> findAll();
    void deleteById(String id);
    Optional<T> findLastByStrategyId(java.util.UUID strategyId);
}
