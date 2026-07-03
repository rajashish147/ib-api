package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.portfolio.PortfolioAnalysis;

/**
 * Analyzes the portfolio relative to strategy targets to 
 * determine if rebalancing is required.
 */
public interface PortfolioAnalysisPort {
    PortfolioAnalysis analyzePortfolio(EvaluationContext context);
}
