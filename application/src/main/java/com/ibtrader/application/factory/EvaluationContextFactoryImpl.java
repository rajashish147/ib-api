package com.ibtrader.application.factory;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioAnalysis;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.inbound.EvaluationContextFactory;
import com.ibtrader.domain.port.inbound.PortfolioAnalysisPort;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.domain.port.outbound.RiskLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Default implementation of the EvaluationContextFactory.
 * Responsible for gathering all necessary data (portfolio, analysis, risk limits)
 * to build a complete EvaluationContext for a given strategy.
 */
@Service
@RequiredArgsConstructor
public class EvaluationContextFactoryImpl implements EvaluationContextFactory {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAnalysisPort portfolioAnalysisPort;
    private final RiskLimitRepository riskLimitRepository;

    @Override
    public EvaluationContext create(TradingStrategy strategy, String accountId) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findByAccountId(accountId);
        
        if (portfolioOpt.isEmpty()) {
            throw new IllegalArgumentException("Portfolio not found for account: " + accountId);
        }
        
        Portfolio portfolio = portfolioOpt.get();
        
        // Build a temporary context just to satisfy the PortfolioAnalysisPort interface,
        // or directly compute the analysis if we refactor PortfolioAnalysisPort.
        // For now, use a temp context.
        EvaluationContext tempContext = EvaluationContext.builder().portfolio(portfolio).build();
        PortfolioAnalysis analysis = portfolioAnalysisPort.analyzePortfolio(tempContext);

        // Determine if market is open (mocked for now, would typically use a MarketSchedulePort)
        boolean marketOpen = true;

        var riskLimits = riskLimitRepository.findAll();

        return EvaluationContext.builder()
                .strategy(strategy)
                .portfolio(portfolio)
                .portfolioAnalysis(analysis)
                .riskLimits(riskLimits)
                .evaluationTime(Instant.now())
                .marketOpen(marketOpen)
                .build();
    }
}
