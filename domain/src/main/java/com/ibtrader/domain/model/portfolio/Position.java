package com.ibtrader.domain.model.portfolio;

import com.ibtrader.domain.model.common.Money;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a single open position within a {@link Portfolio}.
 *
 * <p>A position records how many units of a specific asset the account holds,
 * at what average cost they were acquired, and the current mark-to-market
 * valuation. Positive {@code quantity} denotes a long position; negative
 * quantity denotes a short position.
 *
 * <p>This entity is owned by the {@link Portfolio} aggregate root. It must not
 * be persisted or retrieved independently of its owning portfolio.
 *
 * <p>Use the {@link #create} factory to construct new instances; the Lombok
 * builder is retained for ORM / mapping frameworks.
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Position {

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Surrogate key unique across all positions in the system. */
    private final UUID id;

    /** Foreign key referencing the owning {@link Portfolio}. */
    private final UUID portfolioId;

    /** Foreign key referencing the underlying asset definition. */
    private final UUID assetId;

    /**
     * Denormalised ticker symbol retained for display purposes so that callers
     * do not need to join to the asset table for simple reporting.
     */
    private final String symbol;

    // -------------------------------------------------------------------------
    // Sizing
    // -------------------------------------------------------------------------

    /**
     * Number of units held. Positive values indicate a long position; negative
     * values indicate a short position. A value of zero means the position is
     * closed.
     */
    private BigDecimal quantity;

    // -------------------------------------------------------------------------
    // Cost / valuation
    // -------------------------------------------------------------------------

    /** Weighted-average unit cost at which the position was accumulated. */
    private Money averageCost;

    /** Most recent market price per unit as reported by IB. */
    private Money marketPrice;

    /**
     * Current market value of the entire position
     * ({@code marketPrice * quantity}).
     */
    private Money marketValue;

    /**
     * Unrealised profit-and-loss: difference between current market value and
     * the cost basis of open units.
     */
    private Money unrealizedPnL;

    /**
     * Cumulative realised profit-and-loss arising from closed lots within this
     * position since the last reset.
     */
    private Money realizedPnL;

    /** Timestamp of the last data refresh from IB. */
    private Instant lastUpdated;

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a fresh {@code Position} with zeroed P&amp;L fields. The
     * {@code marketValue} is computed as {@code marketPrice * quantity} at
     * construction time using the currency of {@code averageCost}.
     *
     * @param portfolioId owning portfolio identifier (not null)
     * @param assetId     asset identifier (not null)
     * @param symbol      ticker symbol (not blank)
     * @param quantity    initial quantity (may be zero for a placeholder)
     * @param averageCost average unit cost (not null)
     * @param marketPrice current market price per unit (not null)
     * @return a newly constructed {@code Position}
     * @throws IllegalArgumentException if any required argument is null/blank
     */
    public static Position create(
            UUID portfolioId,
            UUID assetId,
            String symbol,
            BigDecimal quantity,
            Money averageCost,
            Money marketPrice) {

        if (portfolioId == null) throw new IllegalArgumentException("portfolioId must not be null");
        if (assetId == null)    throw new IllegalArgumentException("assetId must not be null");
        if (symbol == null || symbol.isBlank())
            throw new IllegalArgumentException("symbol must not be blank");
        if (quantity == null)   throw new IllegalArgumentException("quantity must not be null");
        if (averageCost == null) throw new IllegalArgumentException("averageCost must not be null");
        if (marketPrice == null) throw new IllegalArgumentException("marketPrice must not be null");

        Money zero = Money.of(BigDecimal.ZERO, averageCost.getCurrency());
        Money computedMarketValue = marketPrice.multiply(quantity.abs());

        return Position.builder()
                .id(UUID.randomUUID())
                .portfolioId(portfolioId)
                .assetId(assetId)
                .symbol(symbol.toUpperCase())
                .quantity(quantity)
                .averageCost(averageCost)
                .marketPrice(marketPrice)
                .marketValue(computedMarketValue)
                .unrealizedPnL(zero)
                .realizedPnL(zero)
                .lastUpdated(Instant.now())
                .build();
    }

    // =========================================================================
    // Business predicates
    // =========================================================================

    /**
     * Returns {@code true} when the account holds a long (net-positive) stake
     * in this asset.
     *
     * @return {@code true} iff {@code quantity > 0}
     */
    public boolean isLong() {
        return quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns {@code true} when the account is short this asset.
     *
     * @return {@code true} iff {@code quantity < 0}
     */
    public boolean isShort() {
        return quantity != null && quantity.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Returns {@code true} when the position has been fully closed (quantity
     * has reached exactly zero).
     *
     * @return {@code true} iff {@code quantity == 0}
     */
    public boolean isClosed() {
        return quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0;
    }

    // =========================================================================
    // Derived values
    // =========================================================================

    /**
     * Computes the total cost basis of the open position.
     * For a long position this is {@code averageCost * quantity}; for a short
     * position the result is negative (reflecting the notional liability).
     *
     * @return total cost as a {@link Money} in the same currency as
     *         {@code averageCost}
     */
    public Money totalCost() {
        return averageCost.multiply(quantity);
    }

    // =========================================================================
    // Mutable update
    // =========================================================================

    /**
     * Applies a full data refresh from IB to this position. All supplied values
     * overwrite the current state and {@code lastUpdated} is set to now.
     *
     * <p>This method is intentionally <em>mutable</em> because a {@link Portfolio}
     * owns its positions and controls aggregate consistency. External code must
     * always update positions through the owning portfolio.
     *
     * @param quantity       new quantity (positive = long, negative = short)
     * @param averageCost    updated weighted-average unit cost
     * @param marketPrice    latest market price per unit
     * @param marketValue    total current market value
     * @param unrealizedPnL  current unrealised P&amp;L
     * @param realizedPnL    cumulative realised P&amp;L
     */
    public void update(
            BigDecimal quantity,
            Money averageCost,
            Money marketPrice,
            Money marketValue,
            Money unrealizedPnL,
            Money realizedPnL) {

        this.quantity = quantity;
        this.averageCost = averageCost;
        this.marketPrice = marketPrice;
        this.marketValue = marketValue;
        this.unrealizedPnL = unrealizedPnL;
        this.realizedPnL = realizedPnL;
        this.lastUpdated = Instant.now();
    }
}
