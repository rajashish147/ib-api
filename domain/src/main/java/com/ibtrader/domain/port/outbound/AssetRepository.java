package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.asset.Asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port (repository) for {@link Asset} aggregate persistence.
 *
 * <p>Provides lookup by domain UUID, ticker symbol, and IB contract identifier,
 * as well as bulk retrieval of enabled and all assets.</p>
 */
public interface AssetRepository {

    /**
     * Retrieves an asset by its domain UUID.
     *
     * @param id the domain UUID of the asset; must not be {@code null}
     * @return an {@link Optional} containing the asset if found, or
     *         {@link Optional#empty()} if no asset with that ID exists
     */
    Optional<Asset> findById(UUID id);

    /**
     * Retrieves an asset by its ticker symbol (case-sensitive).
     *
     * <p>Symbols are expected to be stored and queried in upper-case.</p>
     *
     * @param symbol the ticker symbol (e.g. {@code "AAPL"}); must not be blank
     * @return an {@link Optional} containing the matching asset if found, or
     *         {@link Optional#empty()} if no asset with that symbol exists
     */
    Optional<Asset> findBySymbol(String symbol);

    /**
     * Retrieves an asset by its Interactive Brokers contract identifier (conId).
     *
     * <p>The IB conId is a stable, globally unique identifier for a contract across
     * all IB exchanges and is the most reliable way to uniquely resolve an instrument.</p>
     *
     * @param conId the IB contract identifier
     * @return an {@link Optional} containing the matching asset if found, or
     *         {@link Optional#empty()} if no asset with that conId is registered
     */
    Optional<Asset> findByIbConId(int conId);

    /**
     * Retrieves all assets that are enabled for trading.
     *
     * <p>An enabled asset is eligible for order submission and market-data
     * subscription.  Disabled assets are excluded from automatic processing.</p>
     *
     * @return a (possibly empty) list of enabled assets; never {@code null}
     */
    List<Asset> findAllEnabled();

    /**
     * Retrieves all registered assets regardless of their enabled flag.
     *
     * @return a (possibly empty) list of all assets; never {@code null}
     */
    List<Asset> findAll();

    /**
     * Persists a new or updated {@link Asset} aggregate.
     *
     * @param asset the asset to persist; must not be {@code null}
     * @return the persisted asset, potentially with infrastructure-enriched fields
     */
    Asset save(Asset asset);

    /**
     * Removes the asset with the given identifier from the store.
     *
     * <p>Callers should ensure there are no active positions or pending orders
     * referencing this asset before deletion.</p>
     *
     * @param id the domain UUID of the asset to delete; must not be {@code null}
     */
    void deleteById(UUID id);
}
