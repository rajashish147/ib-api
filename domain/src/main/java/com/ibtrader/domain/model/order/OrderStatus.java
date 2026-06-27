package com.ibtrader.domain.model.order;

import java.util.Map;

/**
 * Represents the lifecycle status of an order within the IBTrader platform.
 *
 * <p>Order statuses are received from the IB TWS/Gateway via the
 * {@code EWrapper.orderStatus()} callback. The raw IB status strings are mapped to
 * this enum through the {@link #fromIbStatus(String)} factory method, which centralises
 * all IB-to-domain translation logic and shields the rest of the application from
 * IB-specific string literals.
 *
 * <p><b>IB API Reference:</b> {@code EWrapper.orderStatus()}, TWS API Order Status guide.
 *
 * <p><b>Status lifecycle:</b>
 * <pre>
 *   PENDING_SUBMIT ──► SUBMITTED ──► PARTIALLY_FILLED ──► FILLED       (terminal)
 *                                  └──────────────────────► CANCELLED   (terminal)
 *                                  └──────────────────────► REJECTED    (terminal)
 *                          │
 *                    PENDING_CANCEL ──────────────────────► CANCELLED   (terminal)
 *                          │
 *                       INACTIVE
 *                          │
 *                        ERROR                                           (terminal)
 * </pre>
 *
 * <pre>{@code
 * // Usage example inside EWrapper callback
 * public void orderStatus(int orderId, String status, ...) {
 *     OrderStatus orderStatus = OrderStatus.fromIbStatus(status);
 *     if (orderStatus.isTerminal()) {
 *         orderRepository.markClosed(orderId, orderStatus);
 *     }
 * }
 * }</pre>
 *
 * @author IBTrader Platform
 * @version 1.0
 * @since 1.0
 */
public enum OrderStatus {

    /**
     * The order has been created locally but has not yet been transmitted to IB.
     *
     * <p><b>IB equivalent:</b> {@code "PendingSubmit"}
     */
    PENDING_SUBMIT,

    /**
     * The order has been acknowledged by IB and is resting on the exchange (or routing).
     *
     * <p><b>IB equivalents:</b> {@code "PreSubmitted"}, {@code "Submitted"}
     */
    SUBMITTED,

    /**
     * The order has been partially executed; some but not all of the requested quantity
     * has been filled.
     *
     * <p><b>IB equivalent:</b> No direct single-word mapping; derived from {@code "Submitted"}
     * with a non-zero filled quantity reported via {@code EWrapper.orderStatus()}.
     */
    PARTIALLY_FILLED,

    /**
     * The order has been completely executed — the full requested quantity has been filled.
     *
     * <p><b>IB equivalent:</b> {@code "Filled"}
     */
    FILLED,

    /**
     * The order has been cancelled — either by the user, by the platform, or by the exchange.
     *
     * <p><b>IB equivalent:</b> {@code "Cancelled"}
     */
    CANCELLED,

    /**
     * The order was rejected by IB or the exchange before execution. A rejection is final
     * and no further state transitions will occur.
     *
     * <p><b>IB equivalent:</b> No direct single IB status; typically observed via
     * {@code EWrapper.error()} with order-related error codes (e.g., 201, 202).
     */
    REJECTED,

    /**
     * A cancellation request has been sent to IB but the exchange has not yet confirmed it.
     *
     * <p><b>IB equivalent:</b> {@code "PendingCancel"}
     */
    PENDING_CANCEL,

    /**
     * The order is in an inactive state — it has been placed but is not actively working
     * (e.g., awaiting a trigger condition or a market open).
     *
     * <p><b>IB equivalent:</b> {@code "Inactive"}
     */
    INACTIVE,

    /**
     * An unexpected or unrecoverable error occurred with this order. This is a catch-all
     * terminal status used when the IB status string cannot be mapped to a known value.
     */
    ERROR;

    // -------------------------------------------------------------------------
    // Static IB status mapping
    // -------------------------------------------------------------------------

    /**
     * Immutable lookup map from IB status strings to {@code OrderStatus} constants.
     * Built once at class-load time for O(1) lookup performance.
     */
    private static final Map<String, OrderStatus> IB_STATUS_MAP = Map.of(
            "PreSubmitted",  SUBMITTED,
            "Submitted",     SUBMITTED,
            "Filled",        FILLED,
            "Cancelled",     CANCELLED,
            "Inactive",      INACTIVE,
            "PendingSubmit", PENDING_SUBMIT,
            "PendingCancel", PENDING_CANCEL
    );

    // -------------------------------------------------------------------------
    // Static Factory
    // -------------------------------------------------------------------------

    /**
     * Maps an IB API order status string to the corresponding {@code OrderStatus} constant.
     *
     * <p>This is the authoritative translation layer between IB's raw status strings
     * (received via {@code EWrapper.orderStatus()}) and the domain model. All unknown or
     * unmapped status strings are treated as {@link #ERROR} to surface unexpected IB
     * behaviour for investigation.
     *
     * <p><b>Mapping table:</b>
     * <table border="1" summary="IB status to OrderStatus mapping">
     *   <tr><th>IB Status String</th><th>OrderStatus</th></tr>
     *   <tr><td>{@code "PreSubmitted"}</td><td>{@link #SUBMITTED}</td></tr>
     *   <tr><td>{@code "Submitted"}</td><td>{@link #SUBMITTED}</td></tr>
     *   <tr><td>{@code "Filled"}</td><td>{@link #FILLED}</td></tr>
     *   <tr><td>{@code "Cancelled"}</td><td>{@link #CANCELLED}</td></tr>
     *   <tr><td>{@code "Inactive"}</td><td>{@link #INACTIVE}</td></tr>
     *   <tr><td>{@code "PendingSubmit"}</td><td>{@link #PENDING_SUBMIT}</td></tr>
     *   <tr><td>{@code "PendingCancel"}</td><td>{@link #PENDING_CANCEL}</td></tr>
     *   <tr><td><em>anything else</em></td><td>{@link #ERROR}</td></tr>
     * </table>
     *
     * @param ibStatus the IB API status string from {@code EWrapper.orderStatus()};
     *                 {@code null} is treated as an unknown status and returns {@link #ERROR}
     * @return the corresponding {@code OrderStatus}; never {@code null}
     */
    public static OrderStatus fromIbStatus(final String ibStatus) {
        if (ibStatus == null) {
            return ERROR;
        }
        return IB_STATUS_MAP.getOrDefault(ibStatus, ERROR);
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this status represents a terminal state — i.e., no further
     * state transitions are expected and the order lifecycle is complete.
     *
     * <p>Terminal statuses: {@link #FILLED}, {@link #CANCELLED}, {@link #REJECTED},
     * {@link #ERROR}.
     *
     * @return {@code true} if the order has reached a terminal state
     */
    public boolean isTerminal() {
        return switch (this) {
            case FILLED, CANCELLED, REJECTED, ERROR -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if this status represents an actively-working order that is
     * resting on the exchange or in the process of being cancelled.
     *
     * <p>Active statuses: {@link #SUBMITTED}, {@link #PARTIALLY_FILLED},
     * {@link #PENDING_CANCEL}.
     *
     * @return {@code true} if the order is currently active
     */
    public boolean isActive() {
        return switch (this) {
            case SUBMITTED, PARTIALLY_FILLED, PENDING_CANCEL -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if this status represents an order that has been created locally
     * but not yet acknowledged by IB.
     *
     * <p>Pending statuses: {@link #PENDING_SUBMIT}.
     *
     * @return {@code true} for {@link #PENDING_SUBMIT}; {@code false} otherwise
     */
    public boolean isPending() {
        return this == PENDING_SUBMIT;
    }
}
