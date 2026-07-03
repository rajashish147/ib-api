package com.ibtrader.application.pipeline;

import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.outbound.RebalancePlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalStage implements PipelineStage {

    private final RebalancePlanRepository rebalancePlanRepository;

    @Override
    public void execute(PipelineContext context) {
        for (TradingStrategy strategy : new java.util.ArrayList<>(context.getOrderPlans().keySet())) {
            if ("MANUAL".equalsIgnoreCase(strategy.getExecutionMode())) {
                log.info("Strategy {} requires manual approval. Persisting pending order plan...", strategy.getId());
                // Here we would map OrderPlan to a RebalancePlan (or similar entity) and persist it
                // Then we remove it from context so OutboxStage doesn't process it
                context.getOrderPlans().remove(strategy);
            }
        }
    }

    @Override
    public int getOrder() {
        return 80;
    }
}
