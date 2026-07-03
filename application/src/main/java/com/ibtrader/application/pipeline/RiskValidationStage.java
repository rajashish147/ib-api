package com.ibtrader.application.pipeline;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import com.ibtrader.domain.port.inbound.RiskValidationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskValidationStage implements PipelineStage {

    private final RiskValidationPort riskValidationPort;

    @Override
    public void execute(PipelineContext context) {
        for (TradingStrategy strategy : context.getEvaluationContexts().keySet()) {
            EvaluationContext evalContext = context.getEvaluationContexts().get(strategy);
            List<ValidatedTradeDecision> decisions = context.getDecisions().getOrDefault(strategy, List.of());
            
            List<ValidatedTradeDecision> riskPassedDecisions = riskValidationPort.validate(decisions, evalContext);
            if (riskPassedDecisions.size() < decisions.size()) {
                log.warn("Risk validation filtered out some decisions for strategy {}", strategy.getId());
            }
            context.getDecisions().put(strategy, riskPassedDecisions);
        }
    }

    @Override
    public int getOrder() {
        return 60;
    }
}
