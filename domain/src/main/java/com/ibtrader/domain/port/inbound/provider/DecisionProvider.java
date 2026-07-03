package com.ibtrader.domain.port.inbound.provider;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradeSignal;

import java.util.List;

public interface DecisionProvider {
    List<TradeSignal> evaluate(EvaluationContext context);
}
