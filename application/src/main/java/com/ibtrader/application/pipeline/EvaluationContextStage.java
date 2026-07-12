package com.ibtrader.application.pipeline;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.inbound.EvaluationContextFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationContextStage implements PipelineStage {

    private final EvaluationContextFactory evaluationContextFactory;

    // NOTE: Cooldown filtering is already performed by ActiveStrategiesStage using
    // StrategyExecutionHistoryPort. Doing it again here with a different repository
    // (EvaluationHistoryRepository) caused strategies to be incorrectly skipped when
    // the two history tables were out of sync.

    @Override
    public void execute(PipelineContext context) {
        for (TradingStrategy strategy : context.getActiveStrategies()) {
            try {
                EvaluationContext evalContext = evaluationContextFactory.create(strategy, context.getAccountId());
                context.getEvaluationContexts().put(strategy, evalContext);
            } catch (Exception e) {
                log.warn("Failed to build EvaluationContext for strategy {}: {}", strategy.getId(), e.getMessage());
            }
        }
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
