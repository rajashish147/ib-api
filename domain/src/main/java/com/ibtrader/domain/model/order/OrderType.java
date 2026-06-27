package com.ibtrader.domain.model.order;

/**
 * Represents the type of an order submitted to Interactive Brokers.
 *
 * <p>Each constant maps to the corresponding IB API order type string used in
 * {@code Order.orderType}. The IB TWS/Gateway documentation defines these strings exactly;
 * they must be passed verbatim when constructing {@code Order} objects.
 *
 * <p><b>IB API Reference:</b> {@code Order.orderType}, TWS Order Types guide.
 *
 * <p><b>Price requirement summary:</b>
 * <ul>
 *   <li>{@link #MARKET} — no price fields required</li>
 *   <li>{@link #LIMIT} — requires {@code Order.lmtPrice}</li>
 *   <li>{@link #STOP} — requires {@code Order.auxPrice} (stop price)</li>
 *   <li>{@link #STOP_LIMIT} — requires both {@code Order.lmtPrice} and {@code Order.auxPrice}</li>
 *   <li>{@link #BRACKET} — platform-level composite; child orders carry their own types</li>
 *   <li>{@link #MIDPRICE} — IB-native midpoint pegging; no explicit price required</li>
 *   <li>{@link #TRAIL} — requires trailing amount/percent via {@code Order.trailingPercent} or {@code Order.auxPrice}</li>
 *   <li>{@link #TRAIL_LIMIT} — requires trailing amount and {@code Order.lmtPrice} offset</li>
 * </ul>
 *
 * <pre>{@code
 * // Usage example
 * OrderType orderType = OrderType.STOP_LIMIT;
 * order.setOrderType(orderType.getIbOrderType()); // "STP LMT"
 * if (orderType.requiresLimitPrice()) order.setLmtPrice(limitPrice);
 * if (orderType.requiresStopPrice())  order.setAuxPrice(stopPrice);
 * }</pre>
 *
 * @author IBTrader Platform
 * @version 1.0
 * @since 1.0
 */
public enum OrderType {

    /**
     * Market order — executes immediately at the best available market price.
     * No price parameters are required.
     *
     * <p><b>IB orderType:</b> {@code "MKT"}
     */
    MARKET("MKT"),

    /**
     * Limit order — executes at the specified limit price or better.
     * Requires {@code Order.lmtPrice}.
     *
     * <p><b>IB orderType:</b> {@code "LMT"}
     */
    LIMIT("LMT"),

    /**
     * Stop order — becomes a market order when the stop (trigger) price is reached.
     * Requires {@code Order.auxPrice} as the stop price.
     *
     * <p><b>IB orderType:</b> {@code "STP"}
     */
    STOP("STP"),

    /**
     * Stop-limit order — becomes a limit order when the stop price is reached.
     * Requires both {@code Order.auxPrice} (stop trigger) and {@code Order.lmtPrice}
     * (the limit price at which to execute after trigger).
     *
     * <p><b>IB orderType:</b> {@code "STP LMT"}
     */
    STOP_LIMIT("STP LMT"),

    /**
     * Bracket order — a platform-level composite order consisting of a primary entry order
     * flanked by a take-profit limit order and a stop-loss stop order.
     *
     * <p>IB does not have a native {@code "BRACKET"} order type; this constant is used
     * internally by the platform to represent the logical grouping. The individual child
     * orders are submitted separately as {@link #LIMIT} and {@link #STOP} orders linked
     * via {@code Order.parentId}.
     *
     * <p><b>IB orderType:</b> {@code "BRACKET"} (platform-internal; not sent directly to IB)
     */
    BRACKET("BRACKET"),

    /**
     * Midprice order — IB-native order type that pegs to the midpoint of the NBBO spread,
     * continuously updating as the spread moves.
     *
     * <p><b>IB orderType:</b> {@code "MIDPRICE"}
     */
    MIDPRICE("MIDPRICE"),

    /**
     * Trailing stop order — a stop order whose stop price trails the market price by a
     * specified amount or percentage. Requires {@code Order.auxPrice} (trail amount) or
     * {@code Order.trailingPercent}.
     *
     * <p><b>IB orderType:</b> {@code "TRAIL"}
     */
    TRAIL("TRAIL"),

    /**
     * Trailing stop-limit order — like a trailing stop, but converts to a limit order
     * when triggered. Requires trailing amount/percent and {@code Order.lmtPrice} offset.
     *
     * <p><b>IB orderType:</b> {@code "TRAIL LIMIT"}
     */
    TRAIL_LIMIT("TRAIL LIMIT");

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * The IB API {@code Order.orderType} string for this order type.
     */
    private final String ibOrderType;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an {@code OrderType} constant with the given IB order type string.
     *
     * @param ibOrderType the IB API {@code orderType} string; never {@code null}
     */
    OrderType(final String ibOrderType) {
        this.ibOrderType = ibOrderType;
    }

    // -------------------------------------------------------------------------
    // Accessor
    // -------------------------------------------------------------------------

    /**
     * Returns the IB API order type string for this order type.
     *
     * <p>This value should be assigned directly to {@code Order.orderType} when
     * constructing IB API {@code Order} objects (except for {@link #BRACKET}, which
     * is a platform-internal composite).
     *
     * @return the non-null IB order type string (e.g., {@code "LMT"}, {@code "STP LMT"})
     */
    public String getIbOrderType() {
        return ibOrderType;
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if orders of this type require a limit price
     * ({@code Order.lmtPrice}) to be specified.
     *
     * <p>Order types that require a limit price: {@link #LIMIT}, {@link #STOP_LIMIT},
     * {@link #TRAIL_LIMIT}.
     *
     * @return {@code true} if a limit price must be provided; {@code false} otherwise
     */
    public boolean requiresLimitPrice() {
        return switch (this) {
            case LIMIT, STOP_LIMIT, TRAIL_LIMIT -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if orders of this type require a stop (auxiliary) price
     * ({@code Order.auxPrice}) to be specified.
     *
     * <p>Order types that require a stop/auxiliary price: {@link #STOP}, {@link #STOP_LIMIT},
     * {@link #TRAIL}, {@link #TRAIL_LIMIT}.
     *
     * <p>Note: For trailing orders ({@link #TRAIL}, {@link #TRAIL_LIMIT}), the
     * {@code auxPrice} represents the trailing amount in price units. If a trailing
     * percentage is preferred, {@code Order.trailingPercent} should be used instead
     * and this field may be zero.
     *
     * @return {@code true} if a stop/auxiliary price must be provided; {@code false} otherwise
     */
    public boolean requiresStopPrice() {
        return switch (this) {
            case STOP, STOP_LIMIT, TRAIL, TRAIL_LIMIT -> true;
            default -> false;
        };
    }

    /**
     * Returns a human-readable representation including the enum name and IB order type string.
     *
     * @return string in the format {@code "STOP_LIMIT[STP LMT]"}
     */
    @Override
    public String toString() {
        return name() + "[" + ibOrderType + "]";
    }
}
