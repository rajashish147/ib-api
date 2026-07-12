package com.ibtrader.domain.engine;

import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioAnalysis;
import com.ibtrader.domain.model.risk.RiskLimit;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable context encapsulating all state required for a single evaluation run
 * of a trading strategy. By aggregating all state here, expression trees and rules
 * can evaluate purely deterministically without performing side-effecting I/O.
 */
@Getter
@Builder
public class EvaluationContext {

    /** The strategy currently being evaluated. */
    private final TradingStrategy strategy;

    /** The current state of the portfolio. */
    private final Portfolio portfolio;

    /** Pre-computed deep analysis of the portfolio. */
    private final PortfolioAnalysis portfolioAnalysis;

    /** Active risk limits that might influence sizing or decisions. */
    private final List<RiskLimit> riskLimits;

    /** Wall-clock time of this evaluation run. */
    private final Instant evaluationTime;

    /** Indicates if the market is currently open. */
    private final boolean marketOpen;

    /**
     * Snapshot of live market prices keyed by asset symbol (e.g. "AAPL" → 182.50).
     * Populated from MarketDataCache at context creation time so that all engine
     * components can read prices synchronously without I/O.
     * May be empty if no prices have been received from IB yet.
     */
    @Builder.Default
    private final Map<String, BigDecimal> marketPrices = Collections.emptyMap();

    /**
     * Looks up the last known price for a symbol.
     *
     * @param symbol asset symbol (case-insensitive)
     * @return the cached price, or {@code null} if not available
     */
    public BigDecimal getMarketPrice(String symbol) {
        if (symbol == null || marketPrices == null) return null;
        return marketPrices.get(symbol.toUpperCase());
    }
}

