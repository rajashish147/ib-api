package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.order.Order;
import com.ibtrader.domain.model.order.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port (repository) for {@link Order} aggregate persistence.
 *
 * <p>Provides lookup by domain UUID, IB order identifier, status, and the
 * owning rebalance plan, as well as paginated access to the full order history.</p>
 */
public interface OrderRepository {

    /**
     * Retrieves an order by its domain UUID.
     *
     * @param id the domain UUID of the order; must not be {@code null}
     * @return an {@link Optional} containing the order if found, or
     *         {@link Optional#empty()} if no order with that ID exists
     */
    Optional<Order> findById(UUID id);

    /**
     * Retrieves an order by its IB-assigned order identifier.
     *
     * <p>This lookup is used when processing execution reports and order status
     * callbacks received from the IB TWS/Gateway API.</p>
     *
     * @param ibOrderId the IB order identifier (assigned by TWS upon submission)
     * @return an {@link Optional} containing the matching order if found, or
     *         {@link Optional#empty()} if no order matches
     */
    Optional<Order> findByIbOrderId(int ibOrderId);

    /**
     * Retrieves all orders with the specified {@link OrderStatus}.
     *
     * @param status the status to filter by; must not be {@code null}
     * @return a (possibly empty) list of orders with the given status; never {@code null}
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Retrieves all orders that were generated as part of a specific rebalance plan.
     *
     * @param planId the domain UUID of the {@code RebalancePlan}; must not be {@code null}
     * @return a (possibly empty) list of orders associated with the plan; never {@code null}
     */
    List<Order> findByRebalancePlanId(UUID planId);

    /**
     * Retrieves all currently open orders — those in {@code SUBMITTED} or
     * {@code PARTIALLY_FILLED} status.
     *
     * <p>This method is used by the execution monitor to poll for outstanding orders
     * and reconcile their status against IB callbacks.</p>
     *
     * @return a (possibly empty) list of open orders; never {@code null}
     */
    List<Order> findOpenOrders();

    /**
     * Persists a new or updated {@link Order} aggregate.
     *
     * @param order the order to persist; must not be {@code null}
     * @return the persisted order, potentially with infrastructure-enriched fields
     */
    Order save(Order order);

    /**
     * Returns a page of all orders in reverse chronological order.
     *
     * @param page zero-based page index
     * @param size the maximum number of orders to return per page; must be positive
     * @return the requested page of orders; never {@code null}
     */
    List<Order> findAll(int page, int size);

    /**
     * Retrieves all orders that involve a specific asset.
     *
     * @param assetId the domain UUID of the asset; must not be {@code null}
     * @return a (possibly empty) list of orders for the asset; never {@code null}
     */
    List<Order> findByAssetId(UUID assetId);
}
