package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;

/**
 * Inbound port (use case) for capturing a point-in-time snapshot of a portfolio's
 * financial state.
 *
 * <p>A snapshot is taken by reading the current NLV, cash balance, and position
 * values from the IB account cache (populated via
 * {@link com.ibtrader.domain.port.outbound.AccountDataPort}), persisting them as
 * an immutable {@link PortfolioSnapshot}, and publishing a
 * {@link com.ibtrader.domain.event.PortfolioSnapshotTakenEvent}.</p>
 *
 * <p>Snapshots are used by strategy evaluation logic to detect NLV threshold
 * crossings and by analytics services to build performance charts.</p>
 */
public interface TakePortfolioSnapshotUseCase {

    /**
     * Encapsulates the parameters required to take a portfolio snapshot.
     *
     * @param accountId the IB account string (e.g. {@code "DU1234567"}); must not be blank
     */
    record Command(String accountId) {}

    /**
     * Captures and persists a portfolio snapshot for the specified account.
     *
     * @param command the snapshot command; must not be {@code null}
     * @return the persisted {@link PortfolioSnapshot}; never {@code null}
     * @throws com.ibtrader.domain.exception.DomainException if no portfolio is found
     *         for the given account ID or if account data is not available
     */
    PortfolioSnapshot execute(Command command);
}
