package com.ibtrader.domain.model.asset;

/**
 * Represents the asset class (security type) of a tradeable instrument.
 *
 * <p>Each constant maps to the corresponding Interactive Brokers API security type string
 * ({@code Contract.secType}), used when constructing {@code Contract} objects for order
 * placement, market data subscriptions, and position queries.
 *
 * <p><b>IB API Reference:</b> {@code EClient.reqContractDetails}, {@code Contract.secType}
 *
 * <p>Note that both {@link #STOCK} and {@link #ETF} share the IB security type {@code "STK"}.
 * The distinction between a plain equity and an ETF is maintained internally by this platform
 * and must be resolved via contract metadata (e.g., category, fund type) rather than the raw
 * IB {@code secType} field.
 *
 * <pre>{@code
 * // Usage example
 * AssetClass assetClass = AssetClass.FUTURES;
 * String ibSecType = assetClass.getIbSecType(); // "FUT"
 * boolean needsExpiry = assetClass.requiresExpiry(); // true
 * }</pre>
 *
 * @author IBTrader Platform
 * @version 1.0
 * @since 1.0
 */
public enum AssetClass {

    /**
     * Common equity / ordinary share listed on a stock exchange.
     *
     * <p><b>IB secType:</b> {@code "STK"}
     */
    STOCK("STK"),

    /**
     * Exchange-Traded Fund — a basket instrument traded like a stock on an exchange.
     *
     * <p>Shares the same IB security type as {@link #STOCK} ({@code "STK"}). The platform
     * distinguishes ETFs from plain equities via contract metadata.
     *
     * <p><b>IB secType:</b> {@code "STK"}
     */
    ETF("STK"),

    /**
     * Futures contract — an exchange-traded derivative obligating delivery of an underlying
     * asset at a specified future date.
     *
     * <p><b>IB secType:</b> {@code "FUT"}
     */
    FUTURES("FUT"),

    /**
     * Foreign exchange spot pair traded in the inter-bank / OTC FOREX market via IB.
     *
     * <p><b>IB secType:</b> {@code "CASH"}
     */
    FOREX("CASH"),

    /**
     * Options contract — a derivative granting the right (but not the obligation) to buy or
     * sell the underlying asset at a specified strike price on or before expiry.
     *
     * <p><b>IB secType:</b> {@code "OPT"}
     */
    OPTIONS("OPT"),

    /**
     * Market index — a statistical composite used for benchmarking and market-data retrieval.
     * Indices are generally non-tradeable directly; positions are taken via index futures or ETFs.
     *
     * <p><b>IB secType:</b> {@code "IND"}
     */
    INDEX("IND");

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * The IB API {@code secType} string corresponding to this asset class.
     * Passed directly to {@code Contract.secType} when building IB API contracts.
     */
    private final String ibSecType;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an {@code AssetClass} constant with the given IB API security type string.
     *
     * @param ibSecType the IB API {@code secType} identifier; never {@code null}
     */
    AssetClass(final String ibSecType) {
        this.ibSecType = ibSecType;
    }

    // -------------------------------------------------------------------------
    // Accessor
    // -------------------------------------------------------------------------

    /**
     * Returns the IB API {@code secType} string for this asset class.
     *
     * <p>This value is suitable for direct assignment to {@code Contract.secType} when
     * constructing IB API {@code Contract} objects.
     *
     * @return the non-null IB security type string (e.g., {@code "STK"}, {@code "FUT"})
     */
    public String getIbSecType() {
        return ibSecType;
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this asset class is a derivative instrument.
     *
     * <p>Derivatives ({@link #FUTURES} and {@link #OPTIONS}) derive their value from an
     * underlying asset and carry additional contract parameters such as expiry date and
     * (for options) strike price and right.
     *
     * @return {@code true} for {@link #FUTURES} and {@link #OPTIONS}; {@code false} otherwise
     */
    public boolean isDerivative() {
        return switch (this) {
            case FUTURES, OPTIONS -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if contracts of this asset class require an expiry date
     * ({@code Contract.lastTradeDateOrContractMonth}) to be specified.
     *
     * <p>Both {@link #FUTURES} and {@link #OPTIONS} are time-limited instruments whose
     * contracts expire on a defined date; all other asset classes have no expiry requirement.
     *
     * @return {@code true} for {@link #FUTURES} and {@link #OPTIONS}; {@code false} otherwise
     */
    public boolean requiresExpiry() {
        return switch (this) {
            case FUTURES, OPTIONS -> true;
            default -> false;
        };
    }

    /**
     * Returns a human-readable representation including the enum name and the IB secType.
     *
     * @return string in the format {@code "STOCK[STK]"}
     */
    @Override
    public String toString() {
        return name() + "[" + ibSecType + "]";
    }
}
