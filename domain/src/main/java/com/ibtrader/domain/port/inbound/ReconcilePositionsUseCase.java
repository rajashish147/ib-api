package com.ibtrader.domain.port.inbound;

/**
 * Inbound port (use case) for reconciling local position records against the
 * positions reported by Interactive Brokers for a given account.
 *
 * <p>Reconciliation is a critical operational process that ensures the local
 * database reflects the true state of positions held at the broker.  It is
 * typically triggered:</p>
 * <ul>
 *   <li>Automatically after the IB connection is re-established following a
 *       disconnection.</li>
 *   <li>On a scheduled basis (e.g. once per trading day at market open).</li>
 *   <li>Manually by an operator via the administration API.</li>
 * </ul>
 *
 * <p>On completion, a
 * {@link com.ibtrader.domain.event.PositionReconciliationCompletedEvent} is
 * published with a summary of the changes made.</p>
 */
public interface ReconcilePositionsUseCase {

    /**
     * Encapsulates the parameters required to trigger position reconciliation.
     *
     * @param accountId the IB account string to reconcile (e.g. {@code "DU1234567"});
     *                  must not be blank
     */
    record Command(String accountId) {}

    /**
     * Executes the position reconciliation use case for the specified account.
     *
     * @param command the reconciliation command; must not be {@code null}
     * @throws com.ibtrader.domain.exception.PositionReconciliationException if the
     *         reconciliation process encounters an unresolvable discrepancy
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if IB is not
     *         reachable when position data is requested
     */
    void execute(Command command);
}
