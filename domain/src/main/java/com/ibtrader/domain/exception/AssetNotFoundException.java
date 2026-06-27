package com.ibtrader.domain.exception;

import java.util.UUID;

/**
 * Domain exception thrown when an {@code Asset} cannot be located in the asset
 * repository, either by its domain UUID or by its ticker symbol.
 *
 * <p>Use the static factory methods {@link #byId(UUID)} and {@link #bySymbol(String)}
 * to create instances with appropriately formatted messages.</p>
 */
public final class AssetNotFoundException extends DomainException {

    /** Stable machine-readable error code for this exception class. */
    public static final String ERROR_CODE = "ASSET_NOT_FOUND";

    /**
     * The domain UUID that was searched, or {@code null} if the lookup was by symbol.
     */
    private final UUID assetId;

    /**
     * The ticker symbol that was searched, or {@code null} if the lookup was by UUID.
     */
    private final String symbol;

    /**
     * Private constructor — use the factory methods instead.
     *
     * @param assetId domain UUID searched (may be {@code null})
     * @param symbol  ticker symbol searched (may be {@code null})
     * @param message the formatted exception message
     */
    private AssetNotFoundException(UUID assetId, String symbol, String message) {
        super(ERROR_CODE, message);
        this.assetId = assetId;
        this.symbol  = symbol;
    }

    /**
     * Creates an {@code AssetNotFoundException} for a lookup by domain UUID.
     *
     * @param assetId the UUID that was not found; must not be {@code null}
     * @return a new {@code AssetNotFoundException} with an appropriate message
     */
    public static AssetNotFoundException byId(UUID assetId) {
        if (assetId == null) throw new IllegalArgumentException("assetId must not be null");
        return new AssetNotFoundException(assetId, null,
                "Asset not found with id: " + assetId);
    }

    /**
     * Creates an {@code AssetNotFoundException} for a lookup by ticker symbol.
     *
     * @param symbol the symbol that was not found; must not be {@code null} or blank
     * @return a new {@code AssetNotFoundException} with an appropriate message
     */
    public static AssetNotFoundException bySymbol(String symbol) {
        if (symbol == null || symbol.isBlank())
            throw new IllegalArgumentException("symbol must not be null or blank");
        return new AssetNotFoundException(null, symbol,
                "Asset not found with symbol: '" + symbol + "'");
    }

    /**
     * Returns the domain UUID that was searched, or {@code null} if the lookup was by symbol.
     *
     * @return the searched asset UUID
     */
    public UUID getAssetId() {
        return assetId;
    }

    /**
     * Returns the ticker symbol that was searched, or {@code null} if the lookup was by UUID.
     *
     * @return the searched ticker symbol
     */
    public String getSymbol() {
        return symbol;
    }
}
