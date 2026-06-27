package com.ibtrader.domain.engine;

import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioAnalysis;
import lombok.RequiredArgsConstructor;
import java.util.logging.Logger;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Concrete implementation of the VariableRegistry.
 * Contains the logic to extract specific metrics dynamically from the context.
 */
@RequiredArgsConstructor
public class DefaultVariableRegistry implements VariableRegistry {

    private static final Logger LOG = Logger.getLogger(DefaultVariableRegistry.class.getName());

    @Override
    public Optional<BigDecimal> resolve(String variableName, EvaluationContext context) {
        PortfolioAnalysis analysis = context.getPortfolioAnalysis();
        Portfolio portfolio = context.getPortfolio();

        return switch (variableName.toUpperCase()) {
            case "PORTFOLIOVALUE" -> Optional.ofNullable(analysis.getTotalPortfolioValue()).map(m -> m.getAmount());
            case "NETLIQUIDATION" -> Optional.ofNullable(analysis.getNetLiquidationValue()).map(m -> m.getAmount());
            case "CASH" -> Optional.ofNullable(analysis.getCashBalance()).map(m -> m.getAmount());
            case "BUYINGPOWER" -> Optional.ofNullable(analysis.getAvailableBuyingPower()).map(m -> m.getAmount());
            case "MARGINUSAGE" -> Optional.ofNullable(analysis.getMarginUsage()).map(p -> p.getValue());
            case "CASHPERCENTAGE" -> Optional.ofNullable(analysis.getAvailableCashPercentage()).map(p -> p.getValue());
            case "UNREALIZEDPNL" -> Optional.ofNullable(analysis.getUnrealizedPnL()).map(m -> m.getAmount());
            case "REALIZEDPNL" -> Optional.ofNullable(analysis.getRealizedPnL()).map(m -> m.getAmount());
            case "LEVERAGE" -> Optional.ofNullable(analysis.getLeverage());
            case "CONCENTRATION" -> Optional.ofNullable(analysis.getConcentrationRisk()).map(p -> p.getValue());
            case "RISKSCORE" -> Optional.ofNullable(analysis.getRiskScore());
            case "DRAWDOWN" -> Optional.ofNullable(analysis.getDrawdownFromPeak()).map(p -> p.getValue());
            default -> resolveDynamic(variableName, context);
        };
    }

    private Optional<BigDecimal> resolveDynamic(String variableName, EvaluationContext context) {
        // Here we handle Market Data (e.g., AAPL.Close) or Technical Indicators (SMA_50_AAPL).
        // This is where IndicatorProviders or MarketDataPorts would be injected and queried.
        // For phase 1, log a warning and return empty to indicate it's not yet supported.
        LOG.warning(String.format("Dynamic resolution of variable '%s' is not implemented yet.", variableName));
        return Optional.empty();
    }
}
