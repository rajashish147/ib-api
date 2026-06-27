package com.ibtrader.domain.model.strategy;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.common.Percentage;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a single asset's target portfolio allocation within a
 * {@link StrategyInstance}.
 *
 * <p>An {@code AllocationTarget} specifies what percentage of the total
 * portfolio value should be held in a specific asset. The strategy engine uses
 * the collection of allocation targets to determine required buy/sell volumes
 * during a rebalance cycle.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code targetWeight} must be in the range [0, 100].</li>
 *   <li>The sum of all active {@code AllocationTarget}s for a strategy should
 *       not exceed 100 % — this constraint is enforced at the strategy service
 *       level, not within the entity itself.</li>
 * </ul>
 *
 * <p>Use the {@link #create} factory method to construct instances. The Lombok
 * builder is retained for ORM / mapping frameworks.
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class AllocationTarget {

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Surrogate key unique across all allocation targets in the system. */
    private final UUID id;

    /** UUID of the owning {@link StrategyInstance}. */
    private final UUID strategyId;

    /** UUID of the target asset. */
    private final UUID assetId;

    /**
     * Denormalised ticker symbol retained for display / reporting so that
     * callers do not need a separate asset lookup for simple UI queries.
     */
    private final String symbol;

    // -------------------------------------------------------------------------
    // Allocation parameters
    // -------------------------------------------------------------------------

    /**
     * Target percentage of the total portfolio value that should be allocated
     * to this asset. Must be in the range [0, 100].
     */
    private final Percentage targetWeight;

    // -------------------------------------------------------------------------
    // Operational flags
    // -------------------------------------------------------------------------

    /**
     * When {@code false} this target is excluded from rebalance plan generation
     * but retained for historical reference.
     */
    private final boolean enabled;

    /** Wall-clock time of the most recent mutation. */
    private final Instant updatedAt;

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a new, enabled {@code AllocationTarget}.
     *
     * @param strategyId   owning strategy UUID (not null)
     * @param assetId      target asset UUID (not null)
     * @param symbol       ticker symbol (not blank)
     * @param targetWeight desired allocation percentage in [0, 100]
     * @return a freshly constructed, enabled {@code AllocationTarget}
     * @throws IllegalArgumentException if any argument fails validation
     */
    public static AllocationTarget create(
            UUID strategyId,
            UUID assetId,
            String symbol,
            Percentage targetWeight) {

        if (strategyId == null)   throw new IllegalArgumentException("strategyId must not be null");
        if (assetId == null)      throw new IllegalArgumentException("assetId must not be null");
        if (symbol == null || symbol.isBlank())
            throw new IllegalArgumentException("symbol must not be blank");
        if (targetWeight == null)
            throw new IllegalArgumentException("targetWeight must not be null");
        validateWeight(targetWeight);

        return AllocationTarget.builder()
                .id(UUID.randomUUID())
                .strategyId(strategyId)
                .assetId(assetId)
                .symbol(symbol.toUpperCase())
                .targetWeight(targetWeight)
                .enabled(true)
                .updatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // Business methods
    // =========================================================================

    /**
     * Computes the absolute target monetary value for this asset given the
     * supplied total portfolio value.
     *
     * <p>Example: if {@code targetWeight} is 20 % and
     * {@code totalPortfolioValue} is $100,000 then the result is $20,000.
     *
     * @param totalPortfolioValue total value of the portfolio (not null, &gt; 0)
     * @return target monetary allocation for this asset
     * @throws IllegalArgumentException if {@code totalPortfolioValue} is null
     */
    public Money targetValue(Money totalPortfolioValue) {
        if (totalPortfolioValue == null) {
            throw new IllegalArgumentException("totalPortfolioValue must not be null");
        }
        // targetWeight is stored as a percentage (e.g. 25.0 = 25%), divide by 100 to get fraction
        BigDecimal fraction = targetWeight.getValue()
                .divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal targetAmount = totalPortfolioValue.getAmount().multiply(fraction);
        return Money.of(targetAmount, totalPortfolioValue.getCurrency());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Validates that the supplied weight is within the permissible [0, 100] range.
     *
     * @param weight the percentage to validate
     * @throws IllegalArgumentException if {@code weight} is outside [0, 100]
     */
    private static void validateWeight(Percentage weight) {
        BigDecimal value = weight.getValue();
        if (value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "targetWeight must be in [0, 100], got: " + value);
        }
    }
}
