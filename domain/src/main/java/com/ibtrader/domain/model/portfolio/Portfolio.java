package com.ibtrader.domain.model.portfolio;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.common.Percentage;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregate root representing an IBKR brokerage account's portfolio state.
 *
 * <p>A {@code Portfolio} is the single source of truth for an account's
 * financial position. It aggregates real-time account value metrics streamed
 * from IB (net liquidation value, cash, margins) and maintains a live list of
 * {@link Position} entities. The aggregate enforces invariants: position
 * uniqueness per asset and monetary consistency.
 *
 * <p>Snapshots of the portfolio's state may be taken at any point via
 * {@link #takeSnapshot()} and persisted independently for historical analysis.
 *
 * <p>Thread-safety: this class is <em>not</em> thread-safe. Callers must
 * coordinate external synchronisation when sharing instances across threads.
 *
 * <p>Use the {@link #create(String)} factory method to construct new instances.
 */
@Getter
@EqualsAndHashCode(of = "id")
public class Portfolio {

    private static final String DEFAULT_CURRENCY = "USD";

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Surrogate key unique across all portfolios in the system. */
    private final UUID id;

    /** IB account identifier string, e.g. {@code "U1234567"}. */
    private final String accountId;

    // -------------------------------------------------------------------------
    // Account value metrics (updated via IB account summary callbacks)
    // -------------------------------------------------------------------------

    /** Net liquidation value — the total value of the account if liquidated now. */
    private Money netLiquidationValue;

    /** Total settled and unsettled cash in the account. */
    private Money totalCashValue;

    /** Cash available to open new positions. */
    private Money availableFunds;

    /**
     * Total buying power — typically 4× available funds for a margin account
     * on intraday trades.
     */
    private Money buyingPower;

    /**
     * Maintenance margin requirement — minimum equity needed to maintain
     * existing positions.
     */
    private Money maintenanceMargin;

    /** Initial margin requirement for current positions. */
    private Money initialMargin;

    /** Aggregate unrealised P&amp;L across all open positions. */
    private Money unrealizedPnL;

    /** Aggregate realised P&amp;L since last reset. */
    private Money realizedPnL;

    // -------------------------------------------------------------------------
    // Positions
    // -------------------------------------------------------------------------

    /**
     * Mutable list of open positions owned by this aggregate. Access must
     * always be performed through the aggregate's business methods to preserve
     * invariants.
     */
    private final List<Position> positions;

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    /** Timestamp of the most recent account-value refresh from IB. */
    private Instant lastUpdated;

    /**
     * Optimistic concurrency version — incremented by the repository on each
     * successful write.
     */
    private final long version;

    // =========================================================================
    // Private constructor (used by factory only)
    // =========================================================================

    private Portfolio(
            UUID id,
            String accountId,
            Money netLiquidationValue,
            Money totalCashValue,
            Money availableFunds,
            Money buyingPower,
            Money maintenanceMargin,
            Money initialMargin,
            Money unrealizedPnL,
            Money realizedPnL,
            List<Position> positions,
            Instant lastUpdated,
            long version) {

        this.id = id;
        this.accountId = accountId;
        this.netLiquidationValue = netLiquidationValue;
        this.totalCashValue = totalCashValue;
        this.availableFunds = availableFunds;
        this.buyingPower = buyingPower;
        this.maintenanceMargin = maintenanceMargin;
        this.initialMargin = initialMargin;
        this.unrealizedPnL = unrealizedPnL;
        this.realizedPnL = realizedPnL;
        this.positions = positions;
        this.lastUpdated = lastUpdated;
        this.version = version;
    }

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a new {@code Portfolio} for the given IB account with all
     * monetary fields initialised to zero USD. Positions list is empty.
     *
     * @param accountId IB account identifier (not blank)
     * @return a fresh, zeroed {@code Portfolio}
     * @throws IllegalArgumentException if {@code accountId} is blank
     */
    public static Portfolio create(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }

        Money zero = Money.of(BigDecimal.ZERO, DEFAULT_CURRENCY);

        return new Portfolio(
                UUID.randomUUID(),
                accountId,
                zero, zero, zero, zero, zero, zero, zero, zero,
                new ArrayList<>(),
                Instant.now(),
                0L);
    }

    /**
     * Restores a persisted portfolio without replacing its identity or version.
     */
    public static Portfolio rehydrate(
            UUID id,
            String accountId,
            Money netLiquidationValue,
            Money totalCashValue,
            Money availableFunds,
            Money buyingPower,
            Money maintenanceMargin,
            Money initialMargin,
            Money unrealizedPnL,
            Money realizedPnL,
            List<Position> positions,
            Instant lastUpdated,
            long version) {

        if (id == null || accountId == null || accountId.isBlank()
                || netLiquidationValue == null || totalCashValue == null
                || availableFunds == null || buyingPower == null
                || maintenanceMargin == null || initialMargin == null
                || unrealizedPnL == null || realizedPnL == null
                || positions == null || lastUpdated == null || version < 0) {
            throw new IllegalArgumentException("Persisted portfolio state is incomplete");
        }

        List<Position> restoredPositions = new ArrayList<>(positions);
        if (restoredPositions.stream().anyMatch(position -> !id.equals(position.getPortfolioId()))) {
            throw new IllegalArgumentException("A position belongs to a different portfolio");
        }

        return new Portfolio(
                id, accountId, netLiquidationValue, totalCashValue, availableFunds,
                buyingPower, maintenanceMargin, initialMargin, unrealizedPnL,
                realizedPnL, restoredPositions, lastUpdated, version);
    }

    // =========================================================================
    // Position management
    // =========================================================================

    /**
     * Finds the current open position for the given asset, if one exists.
     *
     * @param assetId the asset to search for
     * @return an {@link Optional} containing the position, or empty
     */
    public Optional<Position> findPosition(UUID assetId) {
        if (assetId == null) return Optional.empty();
        return positions.stream()
                .filter(p -> assetId.equals(p.getAssetId()))
                .findFirst();
    }

    /**
     * Adds {@code position} to the portfolio if no position for the same asset
     * exists; otherwise replaces the existing position in-place.
     *
     * <p>This is the canonical way for IB position update callbacks to push
     * fresh data into the aggregate.
     *
     * @param position the position data to add or replace (not null)
     * @throws IllegalArgumentException if {@code position} is null or belongs
     *                                  to a different portfolio
     */
    public void addOrUpdatePosition(Position position) {
        if (position == null) throw new IllegalArgumentException("position must not be null");
        if (!this.id.equals(position.getPortfolioId())) {
            throw new IllegalArgumentException(
                    "Position portfolioId " + position.getPortfolioId()
                    + " does not match this portfolio id " + this.id);
        }

        Iterator<Position> it = positions.iterator();
        while (it.hasNext()) {
            if (it.next().getAssetId().equals(position.getAssetId())) {
                it.remove();
                break;
            }
        }
        positions.add(position);
    }

    /**
     * Removes the position associated with the specified asset from this
     * portfolio. No-op if no such position exists.
     *
     * @param assetId asset whose position should be removed
     */
    public void removePosition(UUID assetId) {
        if (assetId == null) return;
        positions.removeIf(p -> assetId.equals(p.getAssetId()));
    }

    // =========================================================================
    // Derived values
    // =========================================================================

    /**
     * Computes the sum of {@code marketValue} across all open positions.
     * Returns a zero-USD {@link Money} when no positions exist.
     *
     * @return total aggregate market value of all positions
     */
    public Money totalPositionValue() {
        return positions.stream()
                .map(Position::getMarketValue)
                .reduce(Money.of(BigDecimal.ZERO, DEFAULT_CURRENCY), Money::add);
    }

    /**
     * Calculates the fraction of the net liquidation value that is held as
     * cash, expressed as a {@link Percentage}.
     *
     * @return cash-to-NLV percentage, or {@code 0 %} when NLV is zero
     */
    public Percentage cashPercentage() {
        if (netLiquidationValue == null
                || netLiquidationValue.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return Percentage.of(BigDecimal.ZERO);
        }
        BigDecimal ratio = totalCashValue.getAmount()
                .divide(netLiquidationValue.getAmount(), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return Percentage.of(ratio);
    }

    // =========================================================================
    // Threshold predicates
    // =========================================================================

    /**
     * Returns {@code true} when the net liquidation value has fallen below the
     * supplied buy threshold — indicating that the strategy should deploy
     * additional capital.
     *
     * @param threshold the NLV level below which a buy signal is raised
     * @return {@code true} iff {@code netLiquidationValue < threshold}
     */
    public boolean isBelowBuyThreshold(Money threshold) {
        if (threshold == null) return false;
        return netLiquidationValue.getAmount()
                .compareTo(threshold.getAmount()) < 0;
    }

    /**
     * Returns {@code true} when the net liquidation value has risen above the
     * supplied sell threshold — indicating that the strategy should reduce
     * exposure.
     *
     * @param threshold the NLV level above which a sell signal is raised
     * @return {@code true} iff {@code netLiquidationValue > threshold}
     */
    public boolean isAboveSellThreshold(Money threshold) {
        if (threshold == null) return false;
        return netLiquidationValue.getAmount()
                .compareTo(threshold.getAmount()) > 0;
    }

    // =========================================================================
    // Account value update
    // =========================================================================

    /**
     * Replaces all account-level monetary fields with the latest data from IB
     * and records the update time.
     *
     * @param nlv          new net liquidation value
     * @param cash         new total cash value
     * @param availFunds   new available funds
     * @param buyingPower  new buying power
     * @param maintMargin  new maintenance margin requirement
     * @param initMargin   new initial margin requirement
     * @param unrealizedPnL aggregate unrealised P&amp;L
     * @param realizedPnL   aggregate realised P&amp;L
     */
    public void updateAccountValues(
            Money nlv,
            Money cash,
            Money availFunds,
            Money buyingPower,
            Money maintMargin,
            Money initMargin,
            Money unrealizedPnL,
            Money realizedPnL) {

        this.netLiquidationValue = nlv;
        this.totalCashValue      = cash;
        this.availableFunds      = availFunds;
        this.buyingPower         = buyingPower;
        this.maintenanceMargin   = maintMargin;
        this.initialMargin       = initMargin;
        this.unrealizedPnL       = unrealizedPnL;
        this.realizedPnL         = realizedPnL;
        this.lastUpdated         = Instant.now();
    }

    // =========================================================================
    // Snapshot
    // =========================================================================

    /**
     * Creates an immutable {@link PortfolioSnapshot} capturing the current
     * state of this portfolio. Snapshots should be persisted separately for
     * historical analysis and auditing.
     *
     * @return a point-in-time snapshot of this portfolio
     */
    public PortfolioSnapshot takeSnapshot() {
        return PortfolioSnapshot.from(this);
    }
}
