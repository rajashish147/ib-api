package com.ibtrader.domain.engine;

import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioAnalysis;
import com.ibtrader.domain.model.risk.RiskLimit;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

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

    // Note: VariableRegistry handles pulling specific MarketData or Indicators
    // so we don't need to load the entire universe of market data into this context.
}
