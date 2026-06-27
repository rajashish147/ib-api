package com.ibtrader.domain.model.asset;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Aggregate root representing a tradeable financial instrument managed via
 * Interactive Brokers (IBKR). An {@code Asset} maps to a single IB contract
 * and encapsulates all contract meta-data required to place orders, evaluate
 * positions and categorise instruments by asset class.
 *
 * <p>Instances are <em>quasi-immutable</em>: mutating operations return new
 * copies (wither-style) so that the aggregate remains thread-safe between
 * reads. Optimistic locking is supported via the {@code version} field.
 *
 * <p>Use the {@link #create(String, String, String, AssetClass)} factory
 * method rather than the Lombok builder directly; the builder is exposed for
 * framework / mapping purposes only.
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Asset {

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Internal surrogate key — stable across the lifetime of the asset. */
    private final UUID id;

    // -------------------------------------------------------------------------
    // Contract meta-data
    // -------------------------------------------------------------------------

    /**
     * Human-readable ticker symbol as used by the exchange, e.g. {@code "SPY"},
     * {@code "ES"}, {@code "EUR"}.
     */
    private final String symbol;

    /**
     * Primary exchange routing code understood by IB, e.g. {@code "SMART"},
     * {@code "CME"}, {@code "NASDAQ"}.
     */
    private final String exchange;

    /**
     * ISO 4217 currency code for the instrument's native denomination,
     * e.g. {@code "USD"}, {@code "EUR"}.
     */
    private final String currency;

    /** Broad asset classification driving order logic and margin treatment. */
    private final AssetClass assetClass;

    /**
     * IB's numeric contract identifier. {@code null} until the contract has
     * been resolved against the IB TWS / Gateway API. Once set it is
     * immutable — use {@link #withIbConId(int)} to return an updated copy.
     */
    private final Integer ibConId;

    /**
     * Contract multiplier — governs notional value calculation.
     * Defaults to {@link BigDecimal#ONE} for equities; set to the relevant
     * exchange multiplier for futures (e.g. {@code 5} for Micro E-mini S&P).
     */
    private final BigDecimal multiplier;

    /**
     * Expiry date for futures and options contracts. {@code null} for
     * non-expiring instruments.
     */
    private final LocalDate expiryDate;

    /**
     * IB local symbol used to uniquely identify a futures contract,
     * e.g. {@code "MESM4"} for June 2024 Micro E-mini S&P 500.
     * {@code null} for non-futures instruments.
     */
    private final String localSymbol;

    // -------------------------------------------------------------------------
    // Lifecycle / operational flags
    // -------------------------------------------------------------------------

    /**
     * When {@code false} the asset must not be used to generate new orders or
     * rebalance plans. Existing open orders are not affected by this flag.
     */
    private final boolean enabled;

    /** Wall-clock time at which this asset was first persisted. */
    private final Instant createdAt;

    /** Wall-clock time of the most recent mutation. */
    private final Instant updatedAt;

    /**
     * Monotonically increasing counter used for optimistic concurrency control.
     * Must be incremented by the repository on every save.
     */
    private final long version;

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a new, <em>unresolved</em> asset with sensible defaults.
     *
     * <ul>
     *   <li>{@code ibConId} is left {@code null} until contract resolution.</li>
     *   <li>{@code multiplier} defaults to {@link BigDecimal#ONE}.</li>
     *   <li>{@code enabled} defaults to {@code true}.</li>
     *   <li>{@code version} starts at {@code 0}.</li>
     * </ul>
     *
     * @param symbol     exchange ticker symbol (not blank)
     * @param exchange   IB routing exchange (not blank)
     * @param currency   ISO 4217 currency code (not blank)
     * @param assetClass broad asset classification
     * @return a freshly constructed, unresolved {@code Asset}
     * @throws IllegalArgumentException if any required argument is blank
     */
    public static Asset create(
            String symbol,
            String exchange,
            String currency,
            AssetClass assetClass) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Asset symbol must not be blank");
        }
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalArgumentException("Exchange must not be blank");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be blank");
        }
        if (assetClass == null) {
            throw new IllegalArgumentException("AssetClass must not be null");
        }

        Instant now = Instant.now();
        return Asset.builder()
                .id(UUID.randomUUID())
                .symbol(symbol.toUpperCase())
                .exchange(exchange.toUpperCase())
                .currency(currency.toUpperCase())
                .assetClass(assetClass)
                .ibConId(null)
                .multiplier(BigDecimal.ONE)
                .expiryDate(null)
                .localSymbol(null)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }

    // =========================================================================
    // Business predicates
    // =========================================================================

    /**
     * Returns {@code true} when this asset represents a futures contract.
     *
     * @return {@code true} iff {@link AssetClass#FUTURES}
     */
    public boolean isFutures() {
        return AssetClass.FUTURES == assetClass;
    }

    /**
     * Returns {@code true} when this asset is an equity-like instrument
     * (common stock or exchange-traded fund).
     *
     * @return {@code true} iff {@link AssetClass#STOCK} or {@link AssetClass#ETF}
     */
    public boolean isEquity() {
        return AssetClass.STOCK == assetClass || AssetClass.ETF == assetClass;
    }

    /**
     * Returns {@code true} when the IB contract ID has been resolved and the
     * asset is ready to be used in order placement.
     *
     * @return {@code true} iff {@code ibConId != null}
     */
    public boolean isResolved() {
        return ibConId != null;
    }

    // =========================================================================
    // Wither-style mutation (returns new copies)
    // =========================================================================

    /**
     * Returns a copy of this asset with {@code enabled} set to {@code true}.
     *
     * @return enabled copy
     */
    public Asset enable() {
        return toBuilder().enabled(true).updatedAt(Instant.now()).build();
    }

    /**
     * Returns a copy of this asset with {@code enabled} set to {@code false}.
     *
     * @return disabled copy
     */
    public Asset disable() {
        return toBuilder().enabled(false).updatedAt(Instant.now()).build();
    }

    /**
     * Returns a copy of this asset with the IB contract ID set to {@code conId}.
     * Once resolved, the contract ID should be treated as immutable.
     *
     * @param conId the positive IB contract identifier
     * @return a resolved copy of this asset
     * @throws IllegalArgumentException if {@code conId} is not positive
     */
    public Asset withIbConId(int conId) {
        if (conId <= 0) {
            throw new IllegalArgumentException(
                    "IB contract ID must be a positive integer, got: " + conId);
        }
        return toBuilder().ibConId(conId).updatedAt(Instant.now()).build();
    }

    // =========================================================================
    // Derived values
    // =========================================================================

    /**
     * Returns the effective contract multiplier for notional value calculations.
     * Falls back to {@link BigDecimal#ONE} when the stored multiplier is
     * {@code null} (defensive guard — the factory always sets it).
     *
     * @return non-null multiplier, guaranteed to be >= 1
     */
    public BigDecimal effectiveMultiplier() {
        return (multiplier != null) ? multiplier : BigDecimal.ONE;
    }
}
