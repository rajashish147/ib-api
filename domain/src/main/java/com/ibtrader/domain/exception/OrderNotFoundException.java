package com.ibtrader.domain.exception;

import java.util.UUID;

/**
 * Domain exception thrown when an {@code Order} cannot be found in the order
 * repository, either by its domain UUID or by its IB-assigned order identifier.
 *
 * <p>Use the static factory methods {@link #byId(UUID)} and {@link #byIbOrderId(int)}
 * to create instances with appropriately formatted messages.</p>
 */
public final class OrderNotFoundException extends DomainException {

    /** Stable machine-readable error code for this exception class. */
    public static final String ERROR_CODE = "ORDER_NOT_FOUND";

    /**
     * The domain UUID that was searched, or {@code null} if the lookup was by IB order ID.
     */
    private final UUID orderId;

    /**
     * The IB order identifier that was searched, or {@code null} if the lookup was by UUID.
     */
    private final Integer ibOrderId;

    /**
     * Private constructor — use the factory methods instead.
     *
     * @param orderId   domain UUID searched (may be {@code null})
     * @param ibOrderId IB order identifier searched (may be {@code null})
     * @param message   the formatted exception message
     */
    private OrderNotFoundException(UUID orderId, Integer ibOrderId, String message) {
        super(ERROR_CODE, message);
        this.orderId   = orderId;
        this.ibOrderId = ibOrderId;
    }

    /**
     * Creates an {@code OrderNotFoundException} for a lookup by domain UUID.
     *
     * @param orderId the domain UUID that was not found; must not be {@code null}
     * @return a new {@code OrderNotFoundException}
     */
    public static OrderNotFoundException byId(UUID orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        return new OrderNotFoundException(orderId, null,
                "Order not found with id: " + orderId);
    }

    /**
     * Creates an {@code OrderNotFoundException} for a lookup by IB-assigned order ID.
     *
     * @param ibOrderId the IB order identifier that was not found
     * @return a new {@code OrderNotFoundException}
     */
    public static OrderNotFoundException byIbOrderId(int ibOrderId) {
        return new OrderNotFoundException(null, ibOrderId,
                "Order not found with IB order id: " + ibOrderId);
    }

    /**
     * Returns the domain UUID that was searched, or {@code null} if the lookup was by IB order ID.
     *
     * @return the searched domain order UUID
     */
    public UUID getOrderId() {
        return orderId;
    }

    /**
     * Returns the IB order identifier that was searched, or {@code null} if the lookup was by UUID.
     *
     * @return the searched IB order identifier
     */
    public Integer getIbOrderId() {
        return ibOrderId;
    }
}
