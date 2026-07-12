package com.ibtrader.application.factory;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioAnalysis;
import com.ibtrader.domain.model.strategy.BasketTarget;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.inbound.EvaluationContextFactory;
import com.ibtrader.domain.port.inbound.PortfolioAnalysisPort;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.MarketDataCache;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.domain.port.outbound.RiskLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of the EvaluationContextFactory.
 * Responsible for gathering all necessary data (portfolio, analysis, risk limits,
 * and live market prices) to build a complete EvaluationContext for a given strategy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationContextFactoryImpl implements EvaluationContextFactory {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAnalysisPort portfolioAnalysisPort;
    private final RiskLimitRepository riskLimitRepository;
    private final AssetRepository assetRepository;
    private final MarketDataCache marketDataCache;

    @Override
    public EvaluationContext create(TradingStrategy strategy, String accountId) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findByAccountId(accountId);

        if (portfolioOpt.isEmpty()) {
            throw new IllegalArgumentException("Portfolio not found for account: " + accountId);
        }

        Portfolio portfolio = portfolioOpt.get();
        EvaluationContext tempContext = EvaluationContext.builder().portfolio(portfolio).build();
        PortfolioAnalysis analysis = portfolioAnalysisPort.analyzePortfolio(tempContext);

        // Determine if market is open (mocked for now, would typically use a MarketSchedulePort)
        boolean marketOpen = true;

        var riskLimits = riskLimitRepository.findAll();

        // Resolve live market prices for all basket targets of this strategy.
        // Prices are read from the in-memory MarketDataCache (populated by IB tick callbacks).
        Map<String, BigDecimal> marketPrices = resolveMarketPrices(strategy);

        return EvaluationContext.builder()
                .strategy(strategy)
                .portfolio(portfolio)
                .portfolioAnalysis(analysis)
                .riskLimits(riskLimits)
                .evaluationTime(Instant.now())
                .marketOpen(marketOpen)
                .marketPrices(marketPrices)
                .build();
    }

    /**
     * Resolves the latest cached price for every basket target symbol in the strategy.
     * Logs a warning for any symbol whose asset is not registered or has no cached price.
     *
     * @param strategy the strategy whose basket targets will be resolved
     * @return symbol → price map (upper-case keys); never null
     */
    private Map<String, BigDecimal> resolveMarketPrices(TradingStrategy strategy) {
        Map<String, BigDecimal> prices = new HashMap<>();

        if (strategy.getTargets() == null || strategy.getTargets().isEmpty()) {
            return prices;
        }

        for (BasketTarget target : strategy.getTargets()) {
            String symbol = target.getSymbol().toUpperCase();
            assetRepository.findBySymbol(symbol).ifPresentOrElse(
                asset -> marketDataCache.getPrice(asset.getId()).ifPresentOrElse(
                    price -> prices.put(symbol, price),
                    () -> log.warn("No cached price for symbol {} (assetId={}). Strategy {} may not trigger correctly.",
                            symbol, asset.getId(), strategy.getId())
                ),
                () -> log.warn("Symbol {} from strategy {} is not registered as an asset.",
                        symbol, strategy.getId())
            );
        }

        return prices;
    }
}

