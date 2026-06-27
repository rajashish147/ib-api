package com.ibtrader.domain.port.outbound;

/**
 * Outbound port for requesting account-level data updates from the Interactive
 * Brokers TWS / IB Gateway API.
 *
 * <p>This port abstracts IB's account subscription model — where live account
 * data (NLV, cash, buying power) and position data are delivered asynchronously
 * via callbacks rather than synchronous responses.  The domain layer uses this
 * port to trigger data pulls and query synchronisation status.</p>
 */
public interface AccountDataPort {

    /**
     * Subscribes to account updates for the specified IB account.
     *
     * <p>Once subscribed, IB will push account-summary values (NLV, cash balance,
     * margin, etc.) via the {@code updateAccountValue} callback at regular intervals
     * and whenever values change.  The infrastructure adapter is responsible for
     * routing these callbacks to the appropriate domain services.</p>
     *
     * @param accountId the IB account string to subscribe to (e.g. {@code "DU1234567"});
     *                  must not be blank
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    void requestAccountUpdates(String accountId);

    /**
     * Requests a snapshot of all current positions across the connected account(s).
     *
     * <p>IB will deliver position data via {@code position} callbacks followed by a
     * {@code positionEnd} callback.  The infrastructure adapter must route the
     * position data to the reconciliation service.</p>
     *
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    void requestPositions();

    /**
     * Requests a list of all executions for the current trading session.
     *
     * <p>IB will deliver execution data via {@code execDetails} callbacks followed by
     * an {@code execDetailsEnd} callback.  The infrastructure adapter must route
     * execution details to the order-fill processing service.</p>
     *
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    void requestExecutions();

    /**
     * Indicates whether an active account-update subscription is currently in place.
     *
     * <p>Callers can use this to decide whether to call
     * {@link #requestAccountUpdates(String)} before reading account values from the
     * cache, or to detect a subscription gap after a reconnection.</p>
     *
     * @return {@code true} if account-update callbacks are being actively received,
     *         {@code false} otherwise
     */
    boolean isAccountSyncActive();
}
