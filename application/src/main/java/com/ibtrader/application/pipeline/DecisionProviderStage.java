package com.ibtrader.application.pipeline;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.inbound.provider.DecisionProviderRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionProviderStage implements PipelineStage {

    private final DecisionProviderRegistry registry;

    @Override
    public void execute(PipelineContext context) {
        for (TradingStrategy strategy : context.getEvaluationContexts().keySet()) {
            try {
                EvaluationContext evalContext = context.getEvaluationContexts().get(strategy);

                List<TradeSignal> combinedSignals = new ArrayList<>();
                // Assuming the registry has a method to evaluate all providers
                // For now we will just accumulate signals
                combinedSignals.addAll(registry.evaluateProviders(evalContext));

                context.getTradeSignals().put(strategy, combinedSignals);
            } catch (Exception e) {
                // Isolate failures per-strategy so one bad decision provider doesn't
                // abort signal generation for every other strategy in this cycle.
                log.warn("Failed to evaluate decision providers for strategy {}: {}",
                        strategy.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
