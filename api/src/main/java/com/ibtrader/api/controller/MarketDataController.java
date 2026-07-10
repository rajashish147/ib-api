package com.ibtrader.api.controller;

import com.ibtrader.api.dto.MarketDataQuoteDto;
import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.MarketDataCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller exposing live (or delayed) market data quotes sourced from the
 * in-process {@code MarketDataCache}.  The cache is populated by the IB EWrapper
 * {@code tickPrice} callback and is therefore as fresh as the IB connection allows.
 *
 * <p>On a paper-trading account, IB provides 15-minute delayed prices.
 * On a live account with market-data subscriptions, prices are real-time.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    /** Prices older than this are flagged as stale in the response. */
    private static final Duration STALENESS_THRESHOLD = Duration.ofSeconds(60);

    private final MarketDataCache marketDataCache;
    private final AssetRepository assetRepository;

    /**
     * Returns a quote for every registered asset.
     *
     * <p>Assets without a cached price (e.g. IB not connected, or subscription not yet set up)
     * are still included in the response with {@code lastPrice = null} and {@code stale = true}
     * so the frontend can render a proper "waiting for data" state.</p>
     *
     * @return HTTP 200 with a list of {@link MarketDataQuoteDto}
     */
    @GetMapping("/quotes")
    public ResponseEntity<List<MarketDataQuoteDto>> getAllQuotes() {
        Map<UUID, BigDecimal> prices     = marketDataCache.getAllPrices();
        Map<UUID, Instant>   timestamps = marketDataCache.getAllTimestamps();
        Instant              now        = Instant.now();

        List<MarketDataQuoteDto> quotes = assetRepository.findAll().stream()
                .map(asset -> buildQuote(asset, prices, timestamps, now))
                .toList();

        log.debug("Returning {} market-data quotes ({} with live prices)", quotes.size(),
                quotes.stream().filter(q -> q.lastPrice() != null).count());
        return ResponseEntity.ok(quotes);
    }

    /**
     * Returns a single quote for the given ticker symbol (case-insensitive).
     *
     * @param symbol ticker symbol (e.g. {@code "AAPL"})
     * @return HTTP 200 with a {@link MarketDataQuoteDto}, or HTTP 404 if the symbol is unknown
     */
    @GetMapping("/quotes/{symbol}")
    public ResponseEntity<MarketDataQuoteDto> getQuote(@PathVariable String symbol) {
        Optional<Asset> assetOpt = assetRepository.findBySymbol(symbol.toUpperCase());
        if (assetOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<UUID, BigDecimal> prices     = marketDataCache.getAllPrices();
        Map<UUID, Instant>   timestamps = marketDataCache.getAllTimestamps();
        Instant              now        = Instant.now();

        return ResponseEntity.ok(buildQuote(assetOpt.get(), prices, timestamps, now));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MarketDataQuoteDto buildQuote(Asset asset,
                                          Map<UUID, BigDecimal> prices,
                                          Map<UUID, Instant> timestamps,
                                          Instant now) {
        UUID       assetId   = asset.getId();
        BigDecimal lastPrice = prices.get(assetId);
        Instant    priceAt   = timestamps.get(assetId);

        boolean stale = lastPrice == null
                || priceAt == null
                || now.minus(STALENESS_THRESHOLD).isAfter(priceAt);

        return new MarketDataQuoteDto(
                assetId,
                asset.getSymbol(),
                asset.getCurrency(),
                asset.getExchange(),
                asset.getAssetClass() != null ? asset.getAssetClass().name() : null,
                lastPrice,
                priceAt,
                stale
        );
    }
}
