package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.strategy.OrderPlan;

public interface IbCommandOutboxPort {

    /**
     * Queues an OrderPlan to be sent to Interactive Brokers asynchronously.
     */
    void queueOrderPlan(OrderPlan plan);
}
