package com.ibtrader.infrastructure.marketdata;

import com.ibtrader.domain.port.outbound.MarketDataCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class MarketDataCacheAdapter implements MarketDataCache {

    private final Map<UUID, PriceEntry> cache = new ConcurrentHashMap<>();
    
    // Market Data Aggregator logic
    private final BlockingQueue<PriceUpdate> updateQueue = new ArrayBlockingQueue<>(10000);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;
    
    // In a real system, you'd track last DB write per asset to throttle DB writes.
    private final Map<UUID, Instant> lastDbWrite = new ConcurrentHashMap<>();
    private static final Duration DB_WRITE_INTERVAL = Duration.ofMinutes(1);

    private record PriceEntry(BigDecimal price, Instant timestamp) {}
    private record PriceUpdate(UUID assetId, BigDecimal price, Instant timestamp) {}

    @PostConstruct
    public void startAggregator() {
        executorService.submit(() -> {
            while (running) {
                try {
                    PriceUpdate update = updateQueue.take();

                    // In-memory cache is already updated synchronously in putPrice().
                    // This thread only handles throttled DB persistence.
                    Instant lastWrite = lastDbWrite.getOrDefault(update.assetId(), Instant.EPOCH);
                    if (update.timestamp().minus(DB_WRITE_INTERVAL).isAfter(lastWrite)) {
                        persistToDatabase(update);
                        lastDbWrite.put(update.assetId(), update.timestamp());
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error processing market data DB persistence", e);
                }
            }
        });
    }
    
    @PreDestroy
    public void stopAggregator() {
        running = false;
        executorService.shutdownNow();
    }
    
    private void persistToDatabase(PriceUpdate update) {
        // Here we would call a JPA Repository to save the tick history.
        // For now, just log it.
        log.debug("Persisting market data to DB: {} @ {}", update.assetId(), update.price());
    }

    @Override
    public void putPrice(UUID assetId, BigDecimal price, Instant timestamp) {
        // Write to in-memory cache immediately so the strategy engine always reads the
        // latest tick synchronously during its evaluation cycle.
        cache.put(assetId, new PriceEntry(price, timestamp));

        // Enqueue for throttled DB persistence only — non-blocking best-effort.
        boolean accepted = updateQueue.offer(new PriceUpdate(assetId, price, timestamp));
        if (!accepted) {
            log.warn("Market data DB-persistence queue is full! DB write skipped for asset {}", assetId);
        }
    }

    @Override
    public Optional<BigDecimal> getPrice(UUID assetId) {
        PriceEntry entry = cache.get(assetId);
        return entry != null ? Optional.of(entry.price()) : Optional.empty();
    }

    @Override
    public Optional<Instant> getPriceTimestamp(UUID assetId) {
        PriceEntry entry = cache.get(assetId);
        return entry != null ? Optional.of(entry.timestamp()) : Optional.empty();
    }

    @Override
    public boolean hasFreshPrice(UUID assetId, Duration maxAge) {
        PriceEntry entry = cache.get(assetId);
        if (entry == null) return false;
        return Instant.now().minus(maxAge).isBefore(entry.timestamp());
    }

    @Override
    public void evict(UUID assetId) {
        cache.remove(assetId);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public java.util.Map<UUID, BigDecimal> getAllPrices() {
        java.util.Map<UUID, BigDecimal> snapshot = new java.util.HashMap<>(cache.size());
        cache.forEach((k, v) -> snapshot.put(k, v.price()));
        return java.util.Collections.unmodifiableMap(snapshot);
    }

    @Override
    public java.util.Map<UUID, Instant> getAllTimestamps() {
        java.util.Map<UUID, Instant> snapshot = new java.util.HashMap<>(cache.size());
        cache.forEach((k, v) -> snapshot.put(k, v.timestamp()));
        return java.util.Collections.unmodifiableMap(snapshot);
    }
}
