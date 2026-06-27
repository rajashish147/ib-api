package com.ibtrader.domain.model.strategy;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.common.Percentage;
import com.ibtrader.domain.model.order.OrderSide;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Entity representing a single asset's trade instruction within a
 * {@link RebalancePlan}.
 *
 * <p>Each {@code RebalancePlanItem} captures the drift between the asset's
 * current and target portfolio weight, and the resulting trade (quantity and
 * estimated value) needed to rebalance. The {@link OrderSide} is automatically
 * derived from the sign of {@code quantityDelta}: positive delta means BUY;
 * negative means SELL.
 *
 * <p>Use the {@link #create} factory method which calculates {@code drift},
 * {@code side}, and {@code estimatedValue} automatically. The Lombok builder
 * is retained for ORM frameworks.
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class RebalancePlanItem {

    /** Minimum tradeable delta to avoid micro-trades (0.001 units). */
    private static final BigDecimal MIN_TRADE_THRESHOLD = new BigDecimal("0.001");

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Surrogate key unique across all plan items. */
    private final UUID id;

    /** UUID of the owning {@link RebalancePlan}. */
    private final UUID planId;

    // -------------------------------------------------------------------------
    // Asset reference
    // -------------------------------------------------------------------------

    /** UUID of the asset to be traded. */
    private final UUID assetId;

    /** Denormalised ticker symbol for display. */
    private final String symbol;

    // -------------------------------------------------------------------------
    // Allocation drift
    // -------------------------------------------------------------------------

    /**
     * Current actual weight of this asset in the portfolio at the time the
     * plan was generated.
     */
    private final Percentage currentWeight;

    /** Target weight as specified by the corresponding {@link AllocationTarget}. */
    private final Percentage targetWeight;

    /**
     * Absolute drift: {@code |currentWeight - targetWeight|}.
     * Computed automatically by the factory.
     */
    private final Percentage drift;

    // -------------------------------------------------------------------------
    // Trade sizing
    // -------------------------------------------------------------------------

    /** Current number of units held by the portfolio. */
    private final BigDecimal currentQuantity;

    /** Target number of units after rebalancing. */
    private final BigDecimal targetQuantity;

    /**
     * Net quantity change: {@code targetQuantity - currentQuantity}.
     * Positive = buy additional units; negative = sell units.
     * Computed automatically by the factory.
     */
    private final BigDecimal quantityDelta;

    /**
     * Trade direction automatically derived from the sign of
     * {@code quantityDelta}. {@link OrderSide#BUY} when delta &gt; 0;
     * {@link OrderSide#SELL} when delta &lt; 0.
     */
    private final OrderSide side;

    /** Reference price used to estimate the cost / proceeds of the trade. */
    private final Money estimatedPrice;

    /**
     * Gross estimated monetary value of the trade:
     * {@code |quantityDelta| * estimatedPrice}.
     * Computed automatically by the factory.
     */
    private final Money estimatedValue;

    // -------------------------------------------------------------------------
    // Order linkage
    // -------------------------------------------------------------------------

    /**
     * UUID of the {@link com.ibtrader.domain.model.order.Order} placed for
     * this item. {@code null} until the order is submitted via
     * {@link #assignOrder(UUID)}.
     */
    private UUID orderId;

    /**
     * Flag indicating whether an order has been placed for this item.
     * Derived from {@code orderId != null} but stored explicitly to support
     * query filtering without null checks.
     */
    private boolean orderPlaced;

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a new {@code RebalancePlanItem}, automatically computing the
     * drift, order side, and estimated value from the supplied inputs.
     *
     * @param planId          owning plan UUID (not null)
     * @param assetId         target asset UUID (not null)
     * @param symbol          ticker symbol (not blank)
     * @param currentWeight   current portfolio weight percentage (not null)
     * @param targetWeight    desired portfolio weight percentage (not null)
     * @param currentQuantity units currently held (not null)
     * @param targetQuantity  units required after rebalance (not null)
     * @param estimatedPrice  reference market price per unit (not null)
     * @return a fully-computed {@code RebalancePlanItem} with no order assigned
     * @throws IllegalArgumentException for invalid arguments
     */
    public static RebalancePlanItem create(
            UUID planId,
            UUID assetId,
            String symbol,
            Percentage currentWeight,
            Percentage targetWeight,
            BigDecimal currentQuantity,
            BigDecimal targetQuantity,
            Money estimatedPrice) {

        if (planId == null)          throw new IllegalArgumentException("planId must not be null");
        if (assetId == null)         throw new IllegalArgumentException("assetId must not be null");
        if (symbol == null || symbol.isBlank())
            throw new IllegalArgumentException("symbol must not be blank");
        if (currentWeight == null)   throw new IllegalArgumentException("currentWeight must not be null");
        if (targetWeight == null)    throw new IllegalArgumentException("targetWeight must not be null");
        if (currentQuantity == null) throw new IllegalArgumentException("currentQuantity must not be null");
        if (targetQuantity == null)  throw new IllegalArgumentException("targetQuantity must not be null");
        if (estimatedPrice == null)  throw new IllegalArgumentException("estimatedPrice must not be null");

        // Compute drift = |currentWeight - targetWeight|
        BigDecimal rawDrift = currentWeight.getValue()
                .subtract(targetWeight.getValue())
                .abs()
                .setScale(4, RoundingMode.HALF_UP);
        Percentage drift = Percentage.of(rawDrift);

        // Compute quantityDelta = targetQuantity - currentQuantity
        BigDecimal quantityDelta = targetQuantity.subtract(currentQuantity)
                .setScale(8, RoundingMode.HALF_UP);

        // Derive order side from sign of delta
        OrderSide side = quantityDelta.compareTo(BigDecimal.ZERO) >= 0
                ? OrderSide.BUY
                : OrderSide.SELL;

        // Compute estimated value = |quantityDelta| * estimatedPrice
        BigDecimal absQtyDelta = quantityDelta.abs();
        Money estimatedValue = Money.of(
                absQtyDelta.multiply(estimatedPrice.getAmount())
                        .setScale(2, RoundingMode.HALF_UP),
                estimatedPrice.getCurrency());

        return RebalancePlanItem.builder()
                .id(UUID.randomUUID())
                .planId(planId)
                .assetId(assetId)
                .symbol(symbol.toUpperCase())
                .currentWeight(currentWeight)
                .targetWeight(targetWeight)
                .drift(drift)
                .currentQuantity(currentQuantity)
                .targetQuantity(targetQuantity)
                .quantityDelta(quantityDelta)
                .side(side)
                .estimatedPrice(estimatedPrice)
                .estimatedValue(estimatedValue)
                .orderId(null)
                .orderPlaced(false)
                .build();
    }

    // =========================================================================
    // Order linkage
    // =========================================================================

    /**
     * Links a placed order to this plan item. Sets {@code orderId} and
     * flips {@code orderPlaced} to {@code true}.
     *
     * @param orderId UUID of the submitted order (not null)
     * @throws IllegalArgumentException if {@code orderId} is null
     * @throws IllegalStateException    if an order has already been assigned
     */
    public void assignOrder(UUID orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        if (this.orderPlaced) {
            throw new IllegalStateException(
                    "An order has already been assigned to this plan item: " + this.orderId);
        }
        this.orderId      = orderId;
        this.orderPlaced  = true;
    }

    // =========================================================================
    // Business predicate
    // =========================================================================

    /**
     * Returns {@code true} when the absolute quantity delta is large enough to
     * warrant placing an order.  Deltas smaller than {@code 0.001} units are
     * treated as rounding noise and suppressed to avoid micro-trades.
     *
     * @return {@code true} iff {@code |quantityDelta| >= 0.001}
     */
    public boolean requiresTrade() {
        return quantityDelta != null
                && quantityDelta.abs().compareTo(MIN_TRADE_THRESHOLD) >= 0;
    }
}
