package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import java.util.List;

/**
 * Translates Risk-Validated Trade Decisions into executable Order Plans.
 */
public interface OrderPlanningPort {
    List<OrderPlan> planOrders(List<ValidatedTradeDecision> decisions, EvaluationContext context);
}
