package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.asset.Asset;

import java.util.UUID;

/**
 * Outbound port for managing real-time and historical market-data subscriptions
 * via the Interactive Brokers TWS / IB Gateway API.
 *
 * <p>Implementations reside in the infrastructure layer and call the IB API's
 * {@code reqMktData}, {@code cancelMktData}, and {@code reqHistoricalData} methods.
 * The domain layer interacts exclusively with this interface, remaining free of
 * IB SDK dependencies.</p>
 */
public interface MarketDataPort {

    /**
     * Subscribes to real-time market data (bid, ask, last price ticks) for the
     * specified asset.
     *
     * <p>The implementation should assign a ticker ID to the subscription and
     * register it for tracking so that it can later be cancelled by
     * {@link #unsubscribeFromMarketData(int)}.  Price ticks received from IB
     * must be forwarded to the {@link MarketDataCache}.</p>
     *
     * @param asset the asset for which market data is requested; must not be {@code null}
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    void subscribeToMarketData(Asset asset);

    /**
     * Cancels a previously established real-time market-data subscription.
     *
     * @param tickerId the IB ticker ID that was returned or assigned at subscription time;
     *                 must match an active subscription
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    void unsubscribeFromMarketData(int tickerId);

    /**
     * Requests historical OHLCV bar data for the specified asset from IB.
     *
     * <p>Historical data is delivered asynchronously via the IB API's
     * {@code historicalData} and {@code historicalDataEnd} callbacks.
     * The adapter implementation must handle these callbacks and route the data
     * to the appropriate application-layer service.</p>
     *
     * @param asset    the asset for which historical data is requested; must not be {@code null}
     * @param duration the IB duration string (e.g. {@code "5 D"} for five trading days);
     *                 must not be blank
     * @param barSize  the IB bar size string (e.g. {@code "1 hour"}, {@code "1 day"});
     *                 must not be blank
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    void requestHistoricalData(Asset asset, String duration, String barSize);

    /**
     * Returns whether a real-time market-data subscription is currently active for
     * the specified asset.
     *
     * @param assetId the domain UUID of the asset to check; must not be {@code null}
     * @return {@code true} if a live subscription exists for the asset, {@code false} otherwise
     */
    boolean isSubscribed(UUID assetId);
}
