package com.ibtrader.domain.port.outbound;

import java.util.List;
import java.util.Optional;

public interface TradeSignalRepository<T> {
    T save(T signal);
    Optional<T> findById(String id);
    List<T> findAll();
    void deleteById(String id);
}
