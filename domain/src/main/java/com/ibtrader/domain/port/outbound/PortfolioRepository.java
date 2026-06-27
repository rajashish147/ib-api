package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.portfolio.Portfolio;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port (repository) for {@link Portfolio} aggregate persistence.
 *
 * <p>Implementations are provided by the infrastructure layer (e.g. a JPA-backed
 * adapter).  The domain layer depends only on this interface, keeping it free of
 * any persistence-framework concerns.</p>
 */
public interface PortfolioRepository {

    /**
     * Retrieves the portfolio associated with the given IB account identifier.
     *
     * @param accountId the IB account string (e.g. {@code "DU1234567"}); must not be blank
     * @return an {@link Optional} containing the portfolio if found, or
     *         {@link Optional#empty()} if no portfolio is associated with the account
     */
    Optional<Portfolio> findByAccountId(String accountId);

    /**
     * Persists a new or updated {@link Portfolio} aggregate.
     *
     * <p>If the portfolio does not yet exist in the store it is inserted; otherwise
     * the existing record is replaced with the supplied state (upsert semantics).</p>
     *
     * @param portfolio the portfolio aggregate to persist; must not be {@code null}
     * @return the persisted portfolio, which may contain infrastructure-generated
     *         fields (e.g. version counters, audit timestamps)
     */
    Portfolio save(Portfolio portfolio);

    /**
     * Removes the portfolio with the given identifier from the store.
     *
     * <p>This is a hard-delete operation.  Callers should ensure that all associated
     * positions and snapshots are removed first (or that cascade rules handle them).</p>
     *
     * @param id the domain UUID of the portfolio to delete; must not be {@code null}
     */
    void delete(UUID id);
}
