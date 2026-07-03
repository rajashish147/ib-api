package com.ibtrader.application.pipeline;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import com.ibtrader.domain.port.inbound.OrderPlanningPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPlanningStage implements PipelineStage {

    private final OrderPlanningPort orderPlanningPort;

    @Override
    public void execute(PipelineContext context) {
        for (TradingStrategy strategy : context.getEvaluationContexts().keySet()) {
            EvaluationContext evalContext = context.getEvaluationContexts().get(strategy);
            List<ValidatedTradeDecision> decisions = context.getDecisions().getOrDefault(strategy, List.of());
            
            if (decisions.isEmpty()) {
                continue;
            }

            List<OrderPlan> plans = orderPlanningPort.planOrders(decisions, evalContext);
            context.getOrderPlans().put(strategy, plans);
        }
    }

    @Override
    public int getOrder() {
        return 70;
    }
}
