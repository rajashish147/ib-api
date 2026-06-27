package com.ibtrader.domain.exception;

/**
 * Domain exception thrown when the position reconciliation process detects an
 * unresolvable discrepancy between positions reported by Interactive Brokers and
 * those stored in the local database.
 *
 * <p>A reconciliation failure may occur when:</p>
 * <ul>
 *   <li>IB data is unavailable or incomplete during the reconciliation window.</li>
 *   <li>The number of discrepancies exceeds a configured safety threshold.</li>
 *   <li>An unexpected error prevents writing reconciled data back to the repository.</li>
 * </ul>
 *
 * <p>When this exception is thrown, the reconciliation run is aborted and the
 * {@code PositionReconciliationCompletedEvent} should <em>not</em> be published.
 * Operator intervention may be required to investigate the discrepancy.</p>
 */
public final class PositionReconciliationException extends DomainException {

    /** Stable machine-readable error code for this exception class. */
    public static final String ERROR_CODE = "POSITION_RECONCILIATION_FAILED";

    /**
     * The IB account identifier for which reconciliation failed.
     */
    private final String accountId;

    /**
     * The number of position discrepancies detected between IB and the local database.
     * May be {@code 0} if the failure occurred before discrepancy counting.
     */
    private final int discrepancyCount;

    /**
     * Constructs a {@code PositionReconciliationException} without a root cause.
     *
     * @param accountId        the IB account for which reconciliation failed
     * @param discrepancyCount the number of position discrepancies detected
     * @param message          a human-readable description of the failure
     */
    public PositionReconciliationException(String accountId, int discrepancyCount, String message) {
        super(ERROR_CODE, message);
        this.accountId        = accountId;
        this.discrepancyCount = discrepancyCount;
    }

    /**
     * Constructs a {@code PositionReconciliationException} wrapping a root cause.
     *
     * @param accountId        the IB account for which reconciliation failed
     * @param discrepancyCount the number of position discrepancies detected
     * @param message          a human-readable description of the failure
     * @param cause            the underlying exception that triggered the failure
     */
    public PositionReconciliationException(String accountId, int discrepancyCount,
                                           String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
        this.accountId        = accountId;
        this.discrepancyCount = discrepancyCount;
    }

    /**
     * Factory method for creating a {@code PositionReconciliationException} with a
     * formatted message.
     *
     * @param accountId        the IB account for which reconciliation failed
     * @param discrepancyCount the number of position discrepancies detected
     * @return a new {@code PositionReconciliationException}
     */
    public static PositionReconciliationException of(String accountId, int discrepancyCount) {
        return new PositionReconciliationException(accountId, discrepancyCount,
                String.format("Position reconciliation failed for account '%s': %d discrepancy(ies) detected.",
                              accountId, discrepancyCount));
    }

    /**
     * Returns the IB account identifier for which reconciliation failed.
     *
     * @return the account ID string
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Returns the number of position discrepancies detected between IB and the
     * local database.
     *
     * @return the discrepancy count
     */
    public int getDiscrepancyCount() {
        return discrepancyCount;
    }
}
