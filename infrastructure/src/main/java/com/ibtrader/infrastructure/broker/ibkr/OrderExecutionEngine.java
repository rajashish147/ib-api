package com.ibtrader.infrastructure.broker.ibkr;

import com.ibtrader.domain.port.outbound.IbCommandOutboxPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.order.Order;
import com.ibtrader.domain.model.order.OrderSide;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.OrderRepository;
import jakarta.annotation.PostConstruct;

import java.util.UUID;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExecutionEngine {

    private final IbCommandOutboxPort ibCommandOutboxPort;
    private final IbConnectionManager ibConnectionManager;
    private final com.ibtrader.infrastructure.persistence.repository.IbCommandOutboxJpaRepository jpaRepository;
    
    private final AssetRepository assetRepository;
    private final OrderRepository orderRepository;
    private final IbConnectionProperties ibConnectionProperties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        ibConnectionManager.setOrderStatusHandler(this::handleOrderStatus);
    }

    @Scheduled(fixedDelayString = "${ibtrader.outbox.delay:5000}")
    public void processPendingCommands() {
        if (!ibConnectionManager.isConnected()) {
            log.trace("Skipping outbox processing because IBKR is not connected.");
            return;
        }

        List<com.ibtrader.infrastructure.persistence.entity.IbCommandOutboxEntity> pending = 
                jpaRepository.findByStatusAndNextRetryAtLessThanEqual(
                com.ibtrader.infrastructure.persistence.entity.IbCommandStatus.PENDING, java.time.Instant.now());

        for (com.ibtrader.infrastructure.persistence.entity.IbCommandOutboxEntity entity : pending) {
            try {
                if ("SUBMIT_ORDER".equals(entity.getCommandType())) {
                    JsonNode payload = objectMapper.readTree(entity.getPayload());
                    String symbol = payload.path("symbol").asText();
                    String sideStr = payload.path("side").asText();
                    OrderSide side = OrderSide.valueOf(sideStr);
                    java.math.BigDecimal qty = new java.math.BigDecimal(payload.path("targetQuantity").asText());
                    java.math.BigDecimal limitPriceVal = payload.hasNonNull("limitPrice") 
                            ? new java.math.BigDecimal(payload.path("limitPrice").asText()) 
                            : null;
                    
                    String accountId = ibConnectionProperties.getAccountId();
                    if (accountId == null || accountId.isEmpty()) accountId = "UNKNOWN";

                    UUID assetId = assetRepository.findBySymbol(symbol).map(a -> a.getId()).orElse(UUID.randomUUID());
                    String strategyId = payload.path("strategyId").asText(null);

                    Order order;
                    if (limitPriceVal != null) {
                        order = Order.createLimit(accountId, assetId, symbol, side, qty, 
                                Money.of(limitPriceVal, "USD"), strategyId);
                    } else {
                        order = Order.createMarket(accountId, assetId, symbol, side, qty, strategyId);
                    }
                    
                    int ibOrderId = ibConnectionManager.submitCommand(entity.getPayload());
                    order.assignIbOrderId(ibOrderId);
                    orderRepository.save(order);
                    
                    entity.setStatus(com.ibtrader.infrastructure.persistence.entity.IbCommandStatus.SENT);
                    entity.setUpdatedAt(java.time.Instant.now());
                } else {
                    log.warn("Unknown command type: {}", entity.getCommandType());
                    entity.setStatus(com.ibtrader.infrastructure.persistence.entity.IbCommandStatus.FAILED);
                }
            } catch (Exception e) {
                log.error("Failed to process outbox command {}: {}", entity.getId(), e.getMessage());
                entity.setAttemptCount(entity.getAttemptCount() + 1);
                if (entity.getAttemptCount() >= entity.getMaxAttempts()) {
                    entity.setStatus(com.ibtrader.infrastructure.persistence.entity.IbCommandStatus.FAILED);
                } else {
                    entity.setNextRetryAt(java.time.Instant.now().plusSeconds((long) Math.pow(2, entity.getAttemptCount()) * 5));
                }
            }
            jpaRepository.save(entity);
        }
    }

    private void handleOrderStatus(int ibOrderId, String status) {
        log.info("Order status update - ibOrderId: {}, status: {}", ibOrderId, status);
        orderRepository.findByIbOrderId(ibOrderId).ifPresent(order -> {
            switch (status) {
                case "Filled":
                    Money limitPrice = order.getLimitPrice();
                    order.recordFill(order.getQuantity(), 
                            limitPrice != null ? limitPrice : Money.of(java.math.BigDecimal.ZERO, "USD"));
                    break;
                case "Cancelled":
                    order.cancel("Cancelled by broker");
                    break;
                case "Inactive":
                case "ApiCancelled":
                    order.reject("Broker rejected or cancelled: " + status);
                    break;
                case "Submitted":
                case "PreSubmitted":
                case "PendingSubmit":
                    break;
                default:
                    log.debug("Unhandled order status: {}", status);
            }
            orderRepository.save(order);
        });
    }
}
