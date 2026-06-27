package com.ibtrader.domain.model.risk;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a configurable risk control limit applied to the trading
 * system.
 *
 * <p>A {@code RiskLimit} encodes a single risk rule: a {@link LimitType} that
 * identifies the metric being constrained (e.g. max single-position weight,
 * maximum daily loss) and a {@code value} that specifies the threshold.
 *
 * <p>The risk engine evaluates each enabled limit against observed metrics at
 * runtime. When a limit is violated, the engine may block order submission,
 * trigger an alert, or initiate a protective sell programme, depending on the
 * orchestration layer's policy.
 *
 * <p>Instances are quasi-immutable: mutating operations
 * ({@link #withValue(BigDecimal)}, {@link #enable()}, {@link #disable()})
 * return new copies so that the entity can be shared safely across read paths.
 *
 * <p>Use the {@link #of(LimitType, BigDecimal)} factory method to create
 * instances. The Lombok builder is retained for ORM frameworks.
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class RiskLimit {

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Surrogate key unique across all risk limits in the system. */
    private final UUID id;

    // -------------------------------------------------------------------------
    // Limit definition
    // -------------------------------------------------------------------------

    /**
     * Classification of the metric being constrained (e.g.
     * {@code MAX_POSITION_WEIGHT}, {@code MAX_DAILY_LOSS}).
     */
    private final LimitType limitType;

    /**
     * Threshold value for this limit. Semantics depend on {@code limitType}:
     * <ul>
     *   <li>For percentage-based limits (e.g. max position weight) the value
     *       represents a percentage in [0, 100].</li>
     *   <li>For absolute monetary limits (e.g. max order size) the value
     *       represents a dollar amount.</li>
     * </ul>
     */
    private final BigDecimal value;

    // -------------------------------------------------------------------------
    // Operational flags
    // -------------------------------------------------------------------------

    /**
     * When {@code false} this limit is dormant and will not be evaluated by
     * the risk engine. Useful for staging new rules without activating them.
     */
    private final boolean enabled;

    /**
     * Human-readable description of what this limit protects against,
     * e.g. {@code "No single equity may exceed 25% of NLV"}.
     */
    private final String description;

    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    /** Wall-clock time of the most recent mutation to this limit. */
    private final Instant updatedAt;

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a new, enabled {@code RiskLimit} with the supplied type and
     * threshold value.
     *
     * @param type  the metric category this limit constrains (not null)
     * @param value the threshold value (not null, must be non-negative)
     * @return a new enabled risk limit with no description
     * @throws IllegalArgumentException if {@code type} is null or
     *                                  {@code value} is null / negative
     */
    public static RiskLimit of(LimitType type, BigDecimal value) {
        if (type == null) throw new IllegalArgumentException("LimitType must not be null");
        if (value == null) throw new IllegalArgumentException("value must not be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "RiskLimit value must be non-negative, got: " + value);
        }

        return RiskLimit.builder()
                .id(UUID.randomUUID())
                .limitType(type)
                .value(value)
                .enabled(true)
                .description(null)
                .updatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // Business methods
    // =========================================================================

    /**
     * Evaluates whether the supplied observed metric value violates this limit.
     *
     * <p>A violation occurs when {@code currentValue > value} (strict
     * greater-than). This covers both percentage-based and absolute limit
     * types where exceeding the threshold is the breach condition. Limits that
     * are disabled always return {@code false}.
     *
     * @param currentValue the observed metric value (not null)
     * @return {@code true} iff the limit is enabled and
     *         {@code currentValue > this.value}
     * @throws IllegalArgumentException if {@code currentValue} is null
     */
    public boolean isViolated(BigDecimal currentValue) {
        if (currentValue == null) {
            throw new IllegalArgumentException("currentValue must not be null");
        }
        return enabled && currentValue.compareTo(this.value) > 0;
    }

    // =========================================================================
    // Wither-style mutations
    // =========================================================================

    /**
     * Returns a copy of this limit with the threshold updated to
     * {@code newValue}.
     *
     * @param newValue the new threshold (not null, non-negative)
     * @return updated copy
     * @throws IllegalArgumentException if {@code newValue} is null or negative
     */
    public RiskLimit withValue(BigDecimal newValue) {
        if (newValue == null) throw new IllegalArgumentException("newValue must not be null");
        if (newValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "RiskLimit value must be non-negative, got: " + newValue);
        }
        return toBuilder().value(newValue).updatedAt(Instant.now()).build();
    }

    /**
     * Returns a copy of this limit with {@code enabled} set to {@code true}.
     *
     * @return enabled copy
     */
    public RiskLimit enable() {
        return toBuilder().enabled(true).updatedAt(Instant.now()).build();
    }

    /**
     * Returns a copy of this limit with {@code enabled} set to {@code false}.
     *
     * @return disabled copy
     */
    public RiskLimit disable() {
        return toBuilder().enabled(false).updatedAt(Instant.now()).build();
    }
}
