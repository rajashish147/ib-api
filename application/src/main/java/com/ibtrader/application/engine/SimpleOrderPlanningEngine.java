package com.ibtrader.application.engine;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import com.ibtrader.domain.port.inbound.OrderPlanningPort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SimpleOrderPlanningEngine implements OrderPlanningPort {

    private static final String EXECUTION_POLICY_IMMEDIATE = "IMMEDIATE";

    @Override
    public List<OrderPlan> planOrders(List<ValidatedTradeDecision> decisions, EvaluationContext context) {
        if (decisions == null || decisions.isEmpty()) {
            return List.of();
        }

        return decisions.stream()
                .filter(this::isUsable)
                .map(this::toOrderPlan)
                .toList();
    }

    private boolean isUsable(ValidatedTradeDecision decision) {
        return decision != null
                && decision.getStrategyId() != null
                && decision.getSymbol() != null
                && decision.getAction() != null
                && decision.getQuantityValue() != null
                && decision.getQuantityValue().signum() > 0;
    }

    private OrderPlan toOrderPlan(ValidatedTradeDecision decision) {
        return OrderPlan.builder()
                .id(UUID.randomUUID())
                .decisionId(decision.getId())
                .strategyId(decision.getStrategyId())
                .symbol(decision.getSymbol().toUpperCase())
                .side(decision.getAction())
                .targetQuantity(decision.getQuantityValue())
                .limitPrice(null)
                .executionPolicy(EXECUTION_POLICY_IMMEDIATE)
                .policyParameters("{}")
                .plannedAt(Instant.now())
                .build();
    }
}
