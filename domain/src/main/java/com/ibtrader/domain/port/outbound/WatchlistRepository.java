package com.ibtrader.domain.port.outbound;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository<T> {
    T save(T watchlist);
    Optional<T> findById(String id);
    List<T> findAll();
    void deleteById(String id);
}
