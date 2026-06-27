package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.portfolio.Position;

import java.util.List;

/**
 * Inbound port (use case) for retrieving the current open positions held by a
 * specific IB account.
 *
 * <p>The positions returned are sourced from the local database rather than
 * being fetched on-demand from IB, so callers should ensure that reconciliation
 * has been run recently to reflect the current IB state.</p>
 */
public interface GetPositionsUseCase {

    /**
     * Returns all currently open {@link Position}s for the specified account.
     *
     * @param accountId the IB account string (e.g. {@code "DU1234567"}); must not be blank
     * @return a (possibly empty) list of positions; never {@code null}
     * @throws com.ibtrader.domain.exception.DomainException if no portfolio is found
     *         for the given account ID
     */
    List<Position> execute(String accountId);
}
