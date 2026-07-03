package com.ibtrader.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.port.outbound.IbCommandOutboxPort;
import com.ibtrader.infrastructure.persistence.entity.IbCommandOutboxEntity;
import com.ibtrader.infrastructure.persistence.entity.IbCommandStatus;
import com.ibtrader.infrastructure.persistence.repository.IbCommandOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter that persists OrderPlan commands to the IB command outbox for
 * reliable, at-least-once delivery to IB Gateway.
 *
 * <p>The outbox pattern guarantees that even if the application crashes after
 * persisting an order plan, the command will be picked up and retried by the
 * {@code IbCommandOutboxProcessor} on restart.</p>
 */
@Slf4j
@Repository
@Profile("!demo")
@RequiredArgsConstructor
public class IbCommandOutboxAdapter implements IbCommandOutboxPort {

    private final IbCommandOutboxJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void queueOrderPlan(OrderPlan plan) {
        try {
            String payload = serializeOrderPlan(plan);

            IbCommandOutboxEntity outboxEntry = IbCommandOutboxEntity.builder()
                    .commandType("SUBMIT_ORDER")
                    .status(IbCommandStatus.PENDING)
                    .payload(payload)
                    .relatedOrderId(plan.getId())
                    .attemptCount(0)
                    .maxAttempts(3)
                    .nextRetryAt(Instant.now())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            jpaRepository.save(outboxEntry);

            log.info("Queued order plan to outbox: {} {} {} shares of {}",
                    plan.getExecutionPolicy(), plan.getSide(), plan.getTargetQuantity(), plan.getSymbol());

        } catch (Exception e) {
            log.error("Failed to persist order plan to outbox for symbol {}: {}", plan.getSymbol(), e.getMessage(), e);
            throw new RuntimeException("Failed to queue order plan for " + plan.getSymbol(), e);
        }
    }

    /**
     * Serializes an OrderPlan into a JSON payload for the outbox.
     */
    private String serializeOrderPlan(OrderPlan plan) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("planId", plan.getId() != null ? plan.getId().toString() : null);
        payload.put("strategyId", plan.getStrategyId() != null ? plan.getStrategyId().toString() : null);
        payload.put("symbol", plan.getSymbol());
        payload.put("side", plan.getSide() != null ? plan.getSide().name() : null);
        payload.put("targetQuantity", plan.getTargetQuantity());
        payload.put("executionPolicy", plan.getExecutionPolicy());
        payload.put("policyParameters", plan.getPolicyParameters());
        payload.put("plannedAt", plan.getPlannedAt() != null ? plan.getPlannedAt().toString() : null);
        if (plan.getLimitPrice() != null) {
            payload.put("limitPrice", plan.getLimitPrice().getAmount());
            payload.put("limitPriceCurrency", plan.getLimitPrice().getCurrency());
        }
        return objectMapper.writeValueAsString(payload);
    }
}
