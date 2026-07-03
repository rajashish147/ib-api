package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import java.util.List;

/**
 * Port for the Decision Engine which receives raw TradeSignals 
 * and processes them into ValidatedTradeDecisions.
 */
public interface DecisionEnginePort {
    List<ValidatedTradeDecision> evaluateSignals(List<TradeSignal> signals, EvaluationContext context);
}
