package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.ExpressionNode;
import com.ibtrader.domain.model.strategy.RuleAction;
import com.ibtrader.domain.model.strategy.TradeSignal;
import java.util.List;

/**
 * Evaluates a strategy's expression tree against a given evaluation context.
 */
public interface RuleEvaluationPort {
    List<TradeSignal> evaluate(EvaluationContext context, ExpressionNode rootNode, List<RuleAction> actions);
}
