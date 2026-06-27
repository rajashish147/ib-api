package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.order.Order;

import java.math.BigDecimal;

/**
 * Outbound port for submitting, cancelling, and modifying orders via the
 * Interactive Brokers TWS / IB Gateway API.
 *
 * <p>Implementations of this port reside in the infrastructure layer and depend on
 * the IB Java API client.  The domain layer is completely isolated from IB-specific
 * concerns through this interface.</p>
 *
 * <p>All methods may throw {@link com.ibtrader.domain.exception.BrokerConnectionException}
 * if the IB connection is not available at the time of the call.</p>
 */
public interface OrderSubmissionPort {

    /**
     * Submits an order to Interactive Brokers for the specified asset.
     *
     * <p>The implementation constructs the IB {@code Contract} and {@code Order}
     * objects from the domain {@link Order} and {@link Asset} aggregates, calls
     * {@code placeOrder} on the EClient, and returns the IB-assigned order ID.</p>
     *
     * @param order the domain order aggregate to submit; must not be {@code null}
     * @param asset the asset to trade; must not be {@code null}
     * @return the IB-assigned order identifier (a positive integer)
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    int submitOrder(Order order, Asset asset);

    /**
     * Sends a cancellation request for the order identified by {@code ibOrderId}.
     *
     * <p>Cancellation in IB is asynchronous: this method sends the cancel request,
     * but the actual cancellation is confirmed via an {@code orderStatus} callback.
     * The domain layer should not mark the order as cancelled until the callback
     * is received.</p>
     *
     * @param ibOrderId the IB-assigned order identifier to cancel; must be positive
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    void cancelOrder(int ibOrderId);

    /**
     * Sends a modify request for an existing live order.
     *
     * <p>IB order modification is performed by calling {@code placeOrder} with the
     * same order ID but with updated parameters.  Not all order attributes can be
     * modified after submission; the IB API documentation defines the modifiable set.</p>
     *
     * @param ibOrderId     the IB-assigned order identifier; must be positive
     * @param newQuantity   the new total quantity for the order; must be positive
     * @param newLimitPrice the new limit price; must not be {@code null} for limit orders
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    void modifyOrder(int ibOrderId, BigDecimal newQuantity, Money newLimitPrice);

    /**
     * Requests the next valid order ID from Interactive Brokers.
     *
     * <p>IB requires that each submitted order carries a unique, monotonically
     * increasing order ID.  This method synchronously requests the next available
     * ID from the IB server and must be called before constructing each new order.</p>
     *
     * @return the next valid IB order ID (a positive integer)
     * @throws com.ibtrader.domain.exception.BrokerConnectionException if no active IB
     *         connection is available
     */
    int getNextOrderId();
}
