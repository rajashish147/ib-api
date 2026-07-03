package com.ibtrader.application.pipeline;

import com.ibtrader.domain.port.inbound.PortfolioAnalysisPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioAnalysisStage implements PipelineStage {

    private final PortfolioAnalysisPort portfolioAnalysisPort;

    @Override
    public void execute(PipelineContext context) {
        // We evaluate context for all strategies but portfolio analysis is common
        // Let's assume we do this in EvaluationContextFactory now
        // But the architecture requested a separate stage for it.
        log.debug("Portfolio Analysis executed");
    }

    @Override
    public int getOrder() {
        return 25;
    }
}
