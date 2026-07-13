package com.ibtrader.application.pipeline;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import com.ibtrader.domain.port.inbound.DecisionEnginePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionStage implements PipelineStage {

    private final DecisionEnginePort decisionEnginePort;

    @Override
    public void execute(PipelineContext context) {
        for (TradingStrategy strategy : context.getEvaluationContexts().keySet()) {
            try {
                EvaluationContext evalContext = context.getEvaluationContexts().get(strategy);
                List<TradeSignal> signals = context.getTradeSignals().getOrDefault(strategy, List.of());

                if (signals.isEmpty()) {
                    continue;
                }

                List<ValidatedTradeDecision> decisions = decisionEnginePort.evaluateSignals(signals, evalContext);
                context.getDecisions().put(strategy, decisions);
            } catch (Exception e) {
                // Isolate failures per-strategy so one bad signal doesn't abort decision
                // evaluation for every other strategy in this cycle.
                log.warn("Failed to evaluate decisions for strategy {}: {}", strategy.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
