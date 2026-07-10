package com.ibtrader.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * REST response DTO for a single market data quote as seen from the IB market-data cache.
 *
 * <p>Prices are sourced from the in-process {@code MarketDataCache}, populated in real-time
 * by the IB tick-price callbacks.  On a paper account, prices may be delayed by up to 15 minutes.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarketDataQuoteDto(

        /** Domain UUID of the asset. */
        UUID assetId,

        /** Ticker symbol (e.g. {@code "AAPL"}). */
        String symbol,

        /** ISO currency code (e.g. {@code "USD"}). */
        String currency,

        /** Exchange (e.g. {@code "SMART"}, {@code "NASDAQ"}). */
        String exchange,

        /** Asset class (e.g. {@code "EQUITY"}, {@code "ETF"}, {@code "FUTURES"}). */
        String assetClass,

        /** Most recent cached last price, or {@code null} if no tick has been received yet. */
        BigDecimal lastPrice,

        /** Wall-clock instant at which the price was last observed, or {@code null} if none. */
        Instant priceAt,

        /**
         * {@code true} if no price has been received or the cached price is older than 60 seconds.
         * Always {@code false} on a paper / delayed-data account where 15-minute delay is expected.
         */
        boolean stale
) {}
