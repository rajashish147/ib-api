package com.ibtrader.domain.model.portfolio;

import com.ibtrader.domain.model.common.Money;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable value snapshot of a {@link Portfolio} captured at a specific point
 * in time.
 *
 * <p>Snapshots serve two primary purposes:
 * <ol>
 *   <li><em>Historical analysis</em> — trending NLV, cash, P&amp;L over time.</li>
 *   <li><em>Audit trail</em> — recording pre/post state around rebalancing
 *       events and strategy executions.</li>
 * </ol>
 *
 * <p>A snapshot is a read-only projection; it does not participate in the
 * aggregate lifecycle of {@code Portfolio} and may be persisted to a separate
 * time-series store.
 *
 * <p>Use the {@link #from(Portfolio)} factory method. The Lombok builder is
 * provided for ORM / mapping frameworks.
 */
@Getter
@Builder
@EqualsAndHashCode(of = "id")
public class PortfolioSnapshot {

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Surrogate key unique to this snapshot record. */
    private final UUID id;

    /** Foreign key referencing the source {@link Portfolio}. */
    private final UUID portfolioId;

    /** IB account identifier — denormalised for standalone query convenience. */
    private final String accountId;

    // -------------------------------------------------------------------------
    // Captured monetary state
    // -------------------------------------------------------------------------

    /** Net liquidation value at capture time. */
    private final Money netLiquidationValue;

    /** Total cash value at capture time. */
    private final Money totalCashValue;

    /** Available funds at capture time. */
    private final Money availableFunds;

    /** Buying power at capture time. */
    private final Money buyingPower;

    /** Maintenance margin requirement at capture time. */
    private final Money maintenanceMargin;

    /** Aggregate unrealised P&amp;L at capture time. */
    private final Money unrealizedPnL;

    /** Aggregate realised P&amp;L at capture time. */
    private final Money realizedPnL;

    // -------------------------------------------------------------------------
    // Position summary
    // -------------------------------------------------------------------------

    /** Number of open positions at capture time (for quick cardinality checks). */
    private final int positionCount;

    // -------------------------------------------------------------------------
    // Timestamp
    // -------------------------------------------------------------------------

    /** Wall-clock time at which this snapshot was taken. */
    private final Instant capturedAt;

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a new {@code PortfolioSnapshot} from the current state of the
     * supplied {@link Portfolio}.
     *
     * @param portfolio the portfolio to snapshot (not null)
     * @return an immutable snapshot of the portfolio's current state
     * @throws IllegalArgumentException if {@code portfolio} is null
     */
    public static PortfolioSnapshot from(Portfolio portfolio) {
        if (portfolio == null) {
            throw new IllegalArgumentException("portfolio must not be null");
        }

        return PortfolioSnapshot.builder()
                .id(UUID.randomUUID())
                .portfolioId(portfolio.getId())
                .accountId(portfolio.getAccountId())
                .netLiquidationValue(portfolio.getNetLiquidationValue())
                .totalCashValue(portfolio.getTotalCashValue())
                .availableFunds(portfolio.getAvailableFunds())
                .buyingPower(portfolio.getBuyingPower())
                .maintenanceMargin(portfolio.getMaintenanceMargin())
                .unrealizedPnL(portfolio.getUnrealizedPnL())
                .realizedPnL(portfolio.getRealizedPnL())
                .positionCount(portfolio.getPositions().size())
                .capturedAt(Instant.now())
                .build();
    }
}
