package com.ibtrader.domain.port.inbound.provider.impl;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.port.inbound.provider.DecisionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class TechnicalIndicatorDecisionProvider implements DecisionProvider {

    @Override
    public List<TradeSignal> evaluate(EvaluationContext context) {
        log.info("Evaluating TechnicalIndicatorDecisionProvider for strategy {}", context.getStrategy().getId());
        // Placeholder for real logic interacting with IndicatorValuePort
        return Collections.emptyList();
    }
}
