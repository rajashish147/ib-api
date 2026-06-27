package com.ibtrader.domain.model.order;

/**
 * Represents the side (direction) of an order — whether it is a buy or a sell.
 *
 * <p>Each constant maps to the corresponding IB API action string used in {@code Order.action},
 * and carries a numeric multiplier that reflects the directional sign convention used
 * throughout the platform (positive = long/buy, negative = short/sell).
 *
 * <p><b>IB API Reference:</b> {@code Order.action} — accepts {@code "BUY"} or {@code "SELL"}.
 *
 * <pre>{@code
 * // Usage example
 * OrderSide side = OrderSide.BUY;
 * order.setAction(side.getIbAction());   // "BUY"
 * double signedQty = side.getMultiplier() * quantity; // positive quantity
 *
 * // Get the opposite side for pairing (e.g., closing trade)
 * OrderSide closingSide = side.opposite(); // SELL
 * }</pre>
 *
 * @author IBTrader Platform
 * @version 1.0
 * @since 1.0
 */
public enum OrderSide {

    /**
     * A buy order — acquires a long position in the instrument.
     *
     * <p><b>IB action:</b> {@code "BUY"}
     * <p><b>Multiplier:</b> {@code +1}
     */
    BUY("BUY", 1),

    /**
     * A sell order — reduces or closes a long position, or opens a short position.
     *
     * <p><b>IB action:</b> {@code "SELL"}
     * <p><b>Multiplier:</b> {@code -1}
     */
    SELL("SELL", -1);

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * The IB API {@code Order.action} string for this order side.
     */
    private final String ibAction;

    /**
     * Directional sign multiplier: {@code +1} for BUY, {@code -1} for SELL.
     * Used to compute signed quantities, PnL, and position deltas.
     */
    private final int multiplier;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an {@code OrderSide} constant with the given IB action string and
     * directional multiplier.
     *
     * @param ibAction   the IB API {@code Order.action} string; never {@code null}
     * @param multiplier {@code +1} for BUY, {@code -1} for SELL
     */
    OrderSide(final String ibAction, final int multiplier) {
        this.ibAction = ibAction;
        this.multiplier = multiplier;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the IB API action string for this order side.
     *
     * <p>This value should be assigned directly to {@code Order.action} when
     * constructing IB API {@code Order} objects.
     *
     * @return the non-null IB action string ({@code "BUY"} or {@code "SELL"})
     */
    public String getIbAction() {
        return ibAction;
    }

    /**
     * Returns the directional sign multiplier for this order side.
     *
     * <p>The multiplier follows the standard trading sign convention:
     * <ul>
     *   <li>{@code +1} — buy / long / increasing position</li>
     *   <li>{@code -1} — sell / short / decreasing position</li>
     * </ul>
     *
     * <p>Multiply an unsigned quantity by this value to obtain a signed quantity
     * suitable for portfolio position and PnL calculations.
     *
     * @return {@code 1} for {@link #BUY} or {@code -1} for {@link #SELL}
     */
    public int getMultiplier() {
        return multiplier;
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Returns the opposite order side.
     *
     * <p>Useful when constructing paired orders (e.g., a closing order for an existing
     * position, or the sell leg of a bracket order).
     *
     * <ul>
     *   <li>{@link #BUY}{@code .opposite()} → {@link #SELL}</li>
     *   <li>{@link #SELL}{@code .opposite()} → {@link #BUY}</li>
     * </ul>
     *
     * @return the opposing {@code OrderSide}; never {@code null}
     */
    public OrderSide opposite() {
        return switch (this) {
            case BUY  -> SELL;
            case SELL -> BUY;
        };
    }

    /**
     * Returns {@code true} if this side is {@link #BUY}.
     *
     * @return {@code true} for {@link #BUY}; {@code false} for {@link #SELL}
     */
    public boolean isBuy() {
        return this == BUY;
    }

    /**
     * Returns {@code true} if this side is {@link #SELL}.
     *
     * @return {@code true} for {@link #SELL}; {@code false} for {@link #BUY}
     */
    public boolean isSell() {
        return this == SELL;
    }

    /**
     * Returns a human-readable representation including the enum name and IB action string.
     *
     * @return string in the format {@code "BUY[BUY, +1]"}
     */
    @Override
    public String toString() {
        return name() + "[" + ibAction + ", " + (multiplier > 0 ? "+" : "") + multiplier + "]";
    }
}
