package com.ibtrader.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event raised after the position reconciliation process completes for a
 * given account.
 *
 * <p>Reconciliation compares positions reported by Interactive Brokers against those
 * stored in the local database and resolves any discrepancies by adding, updating, or
 * removing local position records.</p>
 *
 * <p>Consumers such as audit log writers, operator dashboards, and alert systems can
 * use the summary counts to detect data quality issues or unexpectedly large divergences
 * between IB and the local state.</p>
 */
@Getter
public final class PositionReconciliationCompletedEvent extends DomainEvent {

    /**
     * The IB account identifier for which reconciliation was performed
     * (e.g. {@code "DU1234567"}).
     */
    private final String accountId;

    /**
     * The number of positions reported by IB at the time of reconciliation.
     */
    private final int positionsFromIb;

    /**
     * The number of positions present in the local database before reconciliation.
     */
    private final int positionsInDb;

    /**
     * The number of new positions that were inserted into the local database during
     * this reconciliation pass.
     */
    private final int positionsAdded;

    /**
     * The number of existing local positions that were updated with fresh data from IB.
     */
    private final int positionsUpdated;

    /**
     * The number of local positions that were removed because IB no longer reports them
     * (e.g. position fully closed on the IB side).
     */
    private final int positionsRemoved;

    /**
     * The wall-clock instant at which reconciliation completed.
     */
    private final Instant reconciledAt;

    /**
     * Constructs a {@code PositionReconciliationCompletedEvent} via its Lombok builder.
     *
     * <p>A synthetic aggregate ID ({@link UUID#randomUUID()}) is used because
     * reconciliation runs are not modelled as first-class domain aggregates.</p>
     *
     * @param accountId          IB account identifier
     * @param positionsFromIb    count of positions reported by IB
     * @param positionsInDb      count of positions in the local database before reconciliation
     * @param positionsAdded     count of positions added during reconciliation
     * @param positionsUpdated   count of positions updated during reconciliation
     * @param positionsRemoved   count of positions removed during reconciliation
     * @param reconciledAt       instant at which reconciliation completed
     * @param sequenceNumber     monotonic sequence number
     */
    @Builder
    private PositionReconciliationCompletedEvent(
            String accountId,
            int positionsFromIb,
            int positionsInDb,
            int positionsAdded,
            int positionsUpdated,
            int positionsRemoved,
            Instant reconciledAt,
            long sequenceNumber) {

        super(UUID.randomUUID(), "Portfolio", sequenceNumber);
        this.accountId          = accountId;
        this.positionsFromIb    = positionsFromIb;
        this.positionsInDb      = positionsInDb;
        this.positionsAdded     = positionsAdded;
        this.positionsUpdated   = positionsUpdated;
        this.positionsRemoved   = positionsRemoved;
        this.reconciledAt       = reconciledAt;
    }
}
