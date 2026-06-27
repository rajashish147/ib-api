package com.ibtrader.domain.model.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object representing a percentage in the range [0, 100].
 *
 * <p>Enforces valid range at construction time and provides arithmetic
 * operations needed for allocation calculations and drift comparisons.
 */
public final class Percentage {

    public static final Percentage ZERO    = new Percentage(BigDecimal.ZERO);
    public static final Percentage HUNDRED = new Percentage(BigDecimal.valueOf(100));

    private static final int SCALE = 6;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal value;

    private Percentage(BigDecimal value) {
        this.value = value.setScale(SCALE, ROUNDING);
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Creates a Percentage from a value in [0, 100].
     *
     * @throws IllegalArgumentException if value is outside [0, 100]
     */
    public static Percentage of(BigDecimal value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                "Percentage must be in [0, 100], got: " + value.toPlainString());
        }
        return new Percentage(value);
    }

    public static Percentage of(double value) {
        return of(BigDecimal.valueOf(value));
    }

    /**
     * Creates a Percentage from a fraction in [0, 1].
     * E.g., {@code fromFraction(0.40)} → 40%
     */
    public static Percentage fromFraction(BigDecimal fraction) {
        return of(fraction.multiply(BigDecimal.valueOf(100)));
    }

    public static Percentage fromFraction(double fraction) {
        return fromFraction(BigDecimal.valueOf(fraction));
    }

    public static Percentage zero() {
        return ZERO;
    }

    // ── Arithmetic ────────────────────────────────────────────────────────────

    public Percentage add(Percentage other) {
        return new Percentage(this.value.add(other.value));
    }

    public Percentage subtract(Percentage other) {
        return new Percentage(this.value.subtract(other.value));
    }

    public Percentage abs() {
        return new Percentage(this.value.abs());
    }

    // ── Comparison ────────────────────────────────────────────────────────────

    public boolean isGreaterThan(Percentage other) {
        return this.value.compareTo(other.value) > 0;
    }

    public boolean isLessThan(Percentage other) {
        return this.value.compareTo(other.value) < 0;
    }

    public boolean isGreaterThanOrEqualTo(Percentage other) {
        return this.value.compareTo(other.value) >= 0;
    }

    public boolean isZero() {
        return this.value.compareTo(BigDecimal.ZERO) == 0;
    }

    // ── Conversion ────────────────────────────────────────────────────────────

    /** Returns the fraction equivalent: 40% → 0.40 */
    public BigDecimal toFraction() {
        return value.divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
    }

    public BigDecimal getValue() {
        return value;
    }

    /** Applies this percentage to a monetary amount. E.g., 40% of $1000 = $400 */
    public Money applyTo(Money amount) {
        return amount.multiply(toFraction());
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Percentage p)) return false;
        return value.compareTo(p.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return value.setScale(2, ROUNDING).toPlainString() + "%";
    }
}
