package com.ibtrader.application.pipeline;

import com.ibtrader.domain.engine.CooldownValidator;
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
    private final CooldownValidator cooldownValidator;

    @Override
    public void execute(PipelineContext context) {
        for (TradingStrategy strategy : context.getActiveStrategies()) {
            if (!cooldownValidator.canExecute(strategy)) {
                log.info("Skipping strategy {} due to cooldown.", strategy.getId());
                continue;
            }
            
            EvaluationContext evalContext = evaluationContextFactory.create(strategy, context.getAccountId());
            context.getEvaluationContexts().put(strategy, evalContext);
        }
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
