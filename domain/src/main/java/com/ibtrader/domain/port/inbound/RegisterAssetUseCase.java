package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.asset.AssetClass;

import java.math.BigDecimal;

/**
 * Inbound port (use case) for registering a new tradeable asset in the platform.
 *
 * <p>Registration validates that the symbol is not already present, optionally
 * resolves the IB contract identifier (conId) from the broker if not supplied,
 * persists the {@link Asset} aggregate, and enables it for trading and
 * market-data subscriptions.</p>
 *
 * <p>Use the inner {@link Command} record to supply asset metadata.</p>
 */
public interface RegisterAssetUseCase {

    /**
     * Encapsulates the parameters required to register a new asset.
     *
     * @param symbol      the ticker symbol of the asset (e.g. {@code "AAPL"});
     *                    must not be blank; will be stored in upper-case
     * @param exchange    the primary exchange on which the asset trades
     *                    (e.g. {@code "NASDAQ"}, {@code "NYSE"}); must not be blank
     * @param currency    the trading currency (e.g. {@code "USD"}); must not be blank
     * @param assetClass  the {@link AssetClass} of the asset (e.g. {@code STK},
     *                    {@code OPT}); must not be {@code null}
     * @param multiplier  the contract multiplier; {@code 1} for equities, may differ
     *                    for derivatives; must be positive
     */
    record Command(
            String symbol,
            String exchange,
            String currency,
            AssetClass assetClass,
            BigDecimal multiplier
    ) {}

    /**
     * Executes the asset registration use case.
     *
     * @param command the registration command; must not be {@code null}
     * @return the persisted {@link Asset} aggregate; never {@code null}
     * @throws com.ibtrader.domain.exception.DomainException if an asset with the
     *         same symbol is already registered
     */
    Asset execute(Command command);
}
