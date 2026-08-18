package com.ibtrader.application.engine;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.ExpressionNode;
import com.ibtrader.domain.model.strategy.RuleAction;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.port.inbound.RuleEvaluationPort;

import java.util.List;

public class SimpleRuleEvaluationEngine implements RuleEvaluationPort {

    public SimpleRuleEvaluationEngine() {
    }

    @Override
    public List<TradeSignal> evaluate(EvaluationContext context, ExpressionNode rootNode, List<RuleAction> actions) {
        return List.of();
    }
}
