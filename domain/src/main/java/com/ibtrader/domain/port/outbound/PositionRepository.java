package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.portfolio.Position;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port (repository) for {@link Position} aggregate persistence.
 *
 * <p>Positions are owned by a {@link com.ibtrader.domain.model.portfolio.Portfolio}
 * and are uniquely identified within a portfolio by their asset.  All query
 * methods are scoped to a portfolio to prevent cross-account data leakage.</p>
 */
public interface PositionRepository {

    /**
     * Retrieves all positions held within the specified portfolio.
     *
     * @param portfolioId the domain UUID of the owning portfolio; must not be {@code null}
     * @return a (possibly empty) list of positions; never {@code null}
     */
    List<Position> findByPortfolioId(UUID portfolioId);

    /**
     * Retrieves the position for a specific asset within a portfolio.
     *
     * @param portfolioId the domain UUID of the owning portfolio; must not be {@code null}
     * @param assetId     the domain UUID of the asset; must not be {@code null}
     * @return an {@link Optional} containing the position if held, or
     *         {@link Optional#empty()} if the portfolio has no position in the asset
     */
    Optional<Position> findByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId);

    /**
     * Persists a new or updated {@link Position}.
     *
     * <p>Upsert semantics: if the position (portfolio + asset combination) already
     * exists it is updated; otherwise a new record is inserted.</p>
     *
     * @param position the position to persist; must not be {@code null}
     * @return the persisted position, potentially with infrastructure-enriched fields
     */
    Position save(Position position);

    /**
     * Removes the position for a specific asset from a portfolio.
     *
     * <p>Intended for use during reconciliation when IB reports that a previously
     * held position has been fully closed.</p>
     *
     * @param portfolioId the domain UUID of the owning portfolio; must not be {@code null}
     * @param assetId     the domain UUID of the asset to remove; must not be {@code null}
     */
    void deleteByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId);

    /**
     * Removes all positions belonging to the specified portfolio.
     *
     * <p>This bulk-delete is provided for portfolio teardown and full reconciliation
     * scenarios where all positions must be re-created from IB data.</p>
     *
     * @param portfolioId the domain UUID of the portfolio whose positions should be removed;
     *                    must not be {@code null}
     */
    void deleteAllByPortfolioId(UUID portfolioId);
}
