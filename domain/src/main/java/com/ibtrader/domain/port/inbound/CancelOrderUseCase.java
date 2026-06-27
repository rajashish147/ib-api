package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.order.Order;

import java.util.UUID;

/**
 * Inbound port (use case) for cancelling an outstanding order with Interactive Brokers.
 *
 * <p>The use case validates that the order is in a cancellable state, sends a
 * cancellation request to IB via the
 * {@link com.ibtrader.domain.port.outbound.OrderSubmissionPort}, and marks the order
 * as {@code PENDING_CANCEL} until the IB callback confirms the cancellation.</p>
 *
 * <p>Use the inner {@link Command} record to specify which order to cancel and why.</p>
 */
public interface CancelOrderUseCase {

    /**
     * Encapsulates the parameters required to cancel an order.
     *
     * @param orderId the domain UUID of the order to cancel; must not be {@code null}
     * @param reason  a human-readable explanation of why the order is being cancelled;
     *                must not be blank
     */
    record Command(UUID orderId, String reason) {}

    /**
     * Executes the order cancellation use case.
     *
     * @param command the cancel command; must not be {@code null}
     * @return the updated {@link Order} aggregate in {@code PENDING_CANCEL} (or {@code CANCELLED}) state
     * @throws com.ibtrader.domain.exception.OrderNotFoundException    if no order with the given ID exists
     * @throws com.ibtrader.domain.exception.InvalidOrderStateException if the order is not in a
     *         cancellable state (e.g. already {@code FILLED} or {@code REJECTED})
     * @throws com.ibtrader.domain.exception.BrokerConnectionException      if IB is not reachable
     */
    Order execute(Command command);
}
