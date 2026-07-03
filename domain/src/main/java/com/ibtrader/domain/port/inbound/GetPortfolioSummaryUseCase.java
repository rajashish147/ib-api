package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.portfolio.Portfolio;

/**
 * Inbound port (use case) for retrieving a summary of the portfolio associated
 * with a given IB account.
 *
 * <p>Callers — typically REST controllers or scheduled report generators — invoke
 * {@link #execute(String)} to obtain the current portfolio aggregate, including
 * its NLV, cash balance, and positions.</p>
 */
public interface GetPortfolioSummaryUseCase {

    record Query(String accountId) {}

    /**
     * Returns the {@link Portfolio} associated with the given account identifier.
     *
     * @param query the query containing the IB account string; must not be null
     * @return the portfolio aggregate for the specified account; never {@code null}
     * @throws com.ibtrader.domain.exception.DomainException if no portfolio is found
     *         for the given account ID
     */
    Portfolio execute(Query query);
}
