package com.ibtrader.application.engine;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import com.ibtrader.domain.port.inbound.RiskValidationPort;

import java.util.List;

public class SimpleRiskValidationEngine implements RiskValidationPort {

    @Override
    public List<ValidatedTradeDecision> validate(
            List<ValidatedTradeDecision> decisions,
            EvaluationContext context) {

        return decisions != null ? decisions : List.of();
    }
}
