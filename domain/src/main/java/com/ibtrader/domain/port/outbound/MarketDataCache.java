package com.ibtrader.domain.port.outbound;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for a fast, in-process market price cache.
 *
 * <p>The cache is populated by the IB market-data adapter as it receives real-time
 * price ticks.  Domain services (e.g. the rebalance-plan generator) read prices
 * from this cache rather than making synchronous calls to IB, which would introduce
 * latency and rate-limit concerns.</p>
 *
 * <p>Implementations may use any in-memory or distributed cache technology
 * (e.g. Caffeine, Redis) as long as they honour the freshness semantics described
 * by {@link #hasFreshPrice(UUID, Duration)}.</p>
 */
public interface MarketDataCache {

    /**
     * Stores or replaces the cached price for the specified asset.
     *
     * @param assetId   the domain UUID of the asset; must not be {@code null}
     * @param price     the latest market price; must not be {@code null}
     * @param timestamp the wall-clock instant at which the price was observed; must not be {@code null}
     */
    void putPrice(UUID assetId, BigDecimal price, Instant timestamp);

    /**
     * Retrieves the most recently cached price for the specified asset.
     *
     * @param assetId the domain UUID of the asset; must not be {@code null}
     * @return an {@link Optional} containing the cached price if present, or
     *         {@link Optional#empty()} if no price has been cached for the asset
     */
    Optional<BigDecimal> getPrice(UUID assetId);

    /**
     * Retrieves the timestamp of the most recently cached price for the specified asset.
     *
     * @param assetId the domain UUID of the asset; must not be {@code null}
     * @return an {@link Optional} containing the observation timestamp if a price is cached, or
     *         {@link Optional#empty()} if no price has been cached for the asset
     */
    Optional<Instant> getPriceTimestamp(UUID assetId);

    /**
     * Checks whether a price is cached for the asset <em>and</em> is newer than the
     * specified maximum age.
     *
     * <p>Domain services should use this method before consuming cached prices to
     * guard against stale data causing incorrect rebalancing decisions.</p>
     *
     * @param assetId the domain UUID of the asset; must not be {@code null}
     * @param maxAge  the maximum acceptable age of the cached price; must not be {@code null}
     * @return {@code true} if a price exists and
     *         {@code (Instant.now() - priceTimestamp) <= maxAge}, otherwise {@code false}
     */
    boolean hasFreshPrice(UUID assetId, Duration maxAge);

    /**
     * Removes the cached price entry for the specified asset.
     *
     * <p>Called when the market-data subscription for an asset is cancelled or when
     * the asset is disabled.</p>
     *
     * @param assetId the domain UUID of the asset to evict; must not be {@code null}
     */
    void evict(UUID assetId);

    /**
     * Clears all cached price entries.
     *
     * <p>Intended for use during system shutdown or integration testing teardown.</p>
     */
    void clear();
}
