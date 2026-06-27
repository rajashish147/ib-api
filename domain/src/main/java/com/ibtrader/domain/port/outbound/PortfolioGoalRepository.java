package com.ibtrader.domain.port.outbound;

import java.util.List;
import java.util.Optional;

public interface PortfolioGoalRepository<T> {
    T save(T goal);
    Optional<T> findById(String id);
    List<T> findAll();
    void deleteById(String id);
}
