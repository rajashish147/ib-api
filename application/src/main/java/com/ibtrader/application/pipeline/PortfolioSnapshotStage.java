package com.ibtrader.application.pipeline;

import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.domain.port.outbound.PortfolioSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioSnapshotStage implements PipelineStage {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    @Override
    public void execute(PipelineContext context) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findByAccountId(context.getAccountId());
        if (portfolioOpt.isEmpty()) {
            throw new IllegalStateException("No portfolio found for account " + context.getAccountId());
        }

        Portfolio portfolio = portfolioOpt.get();
        context.setPortfolio(portfolio);

        // Snapshot current state
        snapshotRepository.save(PortfolioSnapshot.from(portfolio));
        log.debug("Snapshot taken for portfolio: {}", portfolio.getId());
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
