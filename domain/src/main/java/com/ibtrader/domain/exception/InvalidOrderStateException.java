package com.ibtrader.domain.exception;

import com.ibtrader.domain.model.order.OrderStatus;

import java.util.UUID;

/**
 * Domain exception thrown when an operation is attempted on an {@code Order}
 * aggregate that is not in the required status for that operation.
 *
 * <p>Examples of scenarios that raise this exception:</p>
 * <ul>
 *   <li>Attempting to cancel an order that has already been {@code FILLED}.</li>
 *   <li>Attempting to modify an order that is in {@code REJECTED} status.</li>
 *   <li>Attempting to mark an order as filled when it is in {@code CANCELLED} status.</li>
 * </ul>
 *
 * <p>When {@code requiredStatus} is {@code null} the exception message conveys only
 * the current status without a specific required status (useful for multi-status
 * preconditions).</p>
 */
public final class InvalidOrderStateException extends DomainException {

    /** Stable machine-readable error code for this exception class. */
    public static final String ERROR_CODE = "INVALID_ORDER_STATE";

    /**
     * The domain identifier of the {@code Order} aggregate that was in an invalid state.
     */
    private final UUID orderId;

    /**
     * The actual current status of the order at the time the exception was raised.
     */
    private final OrderStatus currentStatus;

    /**
     * The status that was required for the attempted operation, or {@code null} if the
     * precondition was expressed as a set of acceptable statuses.
     */
    private final OrderStatus requiredStatus;

    /**
     * Constructs an {@code InvalidOrderStateException}.
     *
     * @param orderId        the UUID of the order in an invalid state; must not be {@code null}
     * @param currentStatus  the current status of the order; must not be {@code null}
     * @param requiredStatus the status required by the attempted operation; may be {@code null}
     */
    public InvalidOrderStateException(UUID orderId, OrderStatus currentStatus, OrderStatus requiredStatus) {
        super(ERROR_CODE, buildMessage(orderId, currentStatus, requiredStatus));
        this.orderId        = orderId;
        this.currentStatus  = currentStatus;
        this.requiredStatus = requiredStatus;
    }

    /**
     * Returns the domain UUID of the order that was in an invalid state.
     *
     * @return the order UUID
     */
    public UUID getOrderId() {
        return orderId;
    }

    /**
     * Returns the actual current status of the order.
     *
     * @return the current {@link OrderStatus}
     */
    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Returns the status required by the attempted operation, or {@code null} if the
     * precondition was not expressed as a single required status.
     *
     * @return the required {@link OrderStatus}, or {@code null}
     */
    public OrderStatus getRequiredStatus() {
        return requiredStatus;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String buildMessage(UUID orderId, OrderStatus current, OrderStatus required) {
        if (required != null) {
            return String.format(
                    "Order [%s] is in status [%s] but must be in status [%s] for this operation.",
                    orderId, current, required);
        }
        return String.format(
                "Order [%s] is in status [%s] which does not permit this operation.",
                orderId, current);
    }
}
