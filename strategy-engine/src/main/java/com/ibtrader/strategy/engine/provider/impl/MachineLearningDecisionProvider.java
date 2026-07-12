package com.ibtrader.strategy.engine.provider.impl;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.port.inbound.provider.DecisionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class MachineLearningDecisionProvider implements DecisionProvider {

    @Override
    public List<TradeSignal> evaluate(EvaluationContext context) {
        log.info("Evaluating MachineLearningDecisionProvider for strategy {}", context.getStrategy().getId());
        // Placeholder for external ML service call
        return Collections.emptyList();
    }
}
