package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import java.util.List;

/**
 * Service responsible for validating trade decisions against defined risk limits.
 */
public interface RiskValidationPort {
    List<ValidatedTradeDecision> validate(List<ValidatedTradeDecision> decisions, EvaluationContext context);
}
