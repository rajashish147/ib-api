package com.ibtrader.application.pipeline;

import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.model.strategy.StrategyExecutionHistory;
import com.ibtrader.domain.port.outbound.IbCommandOutboxPort;
import com.ibtrader.domain.port.outbound.StrategyExecutionHistoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxStage implements PipelineStage {

    private final IbCommandOutboxPort outboxPort;
    private final StrategyExecutionHistoryPort executionHistoryPort;

    @Override
    public void execute(PipelineContext context) {
        for (TradingStrategy strategy : context.getOrderPlans().keySet()) {
            try {
                List<OrderPlan> plans = context.getOrderPlans().get(strategy);

                boolean isAutoExecuted = "PAPER".equalsIgnoreCase(strategy.getExecutionMode())
                        || "LIVE".equalsIgnoreCase(strategy.getExecutionMode());
                if (isAutoExecuted) {
                    // Generate unique client IDs for the orders and push to outbox
                    for (OrderPlan plan : plans) {
                        log.info("Sending order plan {} to IBKR outbox for strategy {}", plan.getId(), strategy.getId());
                        outboxPort.queueOrderPlan(plan);
                    }
                    executionHistoryPort.save(StrategyExecutionHistory.create(
                            strategy.getId(), true, "Successfully queued orders to outbox"
                    ));
                } else {
                    log.info("Strategy {} is in mode {}, skipping outbox execution.",
                            strategy.getId(), strategy.getExecutionMode());
                    executionHistoryPort.save(StrategyExecutionHistory.create(
                            strategy.getId(), true, "Evaluated without orders or skipped execution mode"
                    ));
                }
            } catch (Exception e) {
                // Isolate failures per-strategy so a bad order plan for one strategy doesn't
                // prevent OTHER strategies' valid plans from reaching the outbox this cycle.
                log.error("Failed to queue orders to outbox for strategy {}: {}", strategy.getId(), e.getMessage(), e);
                executionHistoryPort.save(StrategyExecutionHistory.create(
                        strategy.getId(), false, "Failed to queue orders to outbox: " + e.getMessage()
                ));
            }
        }
    }

    @Override
    public int getOrder() {
        return 90;
    }
}
