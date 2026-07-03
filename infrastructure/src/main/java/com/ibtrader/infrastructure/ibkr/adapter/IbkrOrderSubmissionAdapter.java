package com.ibtrader.infrastructure.ibkr.adapter;

import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.order.Order;
import com.ibtrader.domain.port.outbound.OrderSubmissionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class IbkrOrderSubmissionAdapter implements OrderSubmissionPort {

    private final AtomicInteger mockOrderId = new AtomicInteger(1000);

    @Override
    public int submitOrder(Order order, Asset asset) {
        log.info("Submitting order {} for asset {}", order, asset);
        // Implement real IBKR logic or outbox insertion here
        return mockOrderId.incrementAndGet();
    }

    @Override
    public void cancelOrder(int ibOrderId) {
        log.info("Cancelling IB order ID {}", ibOrderId);
    }

    @Override
    public void modifyOrder(int ibOrderId, BigDecimal newQuantity, Money newLimitPrice) {
        log.info("Modifying IB order ID {} to qty {}, price {}", ibOrderId, newQuantity, newLimitPrice);
    }

    @Override
    public int getNextOrderId() {
        return mockOrderId.incrementAndGet();
    }
}
