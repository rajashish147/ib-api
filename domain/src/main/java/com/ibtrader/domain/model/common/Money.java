package com.ibtrader.domain.model.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object representing a monetary amount with an explicit currency.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Immutable — all operations return new instances.</li>
 *   <li>Currency-safe — arithmetic operations reject mismatched currencies.</li>
 *   <li>Fixed 4-decimal precision for internal storage; display can be rounded.</li>
 *   <li>No use of {@link java.util.Currency} enum to allow non-ISO codes (e.g., IB's "BASE").</li>
 * </ul>
 */
public final class Money {

    public static final int STORAGE_SCALE = 4;
    public static final int DISPLAY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        this.amount = amount.setScale(STORAGE_SCALE, ROUNDING);
        this.currency = currency.toUpperCase();
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money of(long amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, "USD");
    }

    public static Money usd(double amount) {
        return new Money(BigDecimal.valueOf(amount), "USD");
    }

    public static Money usd(long amount) {
        return new Money(BigDecimal.valueOf(amount), "USD");
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money zeroUsd() {
        return zero("USD");
    }

    // ── Arithmetic ────────────────────────────────────────────────────────────

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor).setScale(STORAGE_SCALE, ROUNDING), this.currency);
    }

    public Money multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    public Money multiply(long factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    public Money divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return new Money(this.amount.divide(divisor, STORAGE_SCALE, ROUNDING), this.currency);
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    // ── Comparison ────────────────────────────────────────────────────────────

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isLessThanOrEqualTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    // ── Derived calculations ──────────────────────────────────────────────────

    /**
     * Returns what percentage this amount is of {@code total}.
     * Returns {@link Percentage#zero()} if total is zero to avoid division-by-zero.
     */
    public Percentage percentageOf(Money total) {
        requireSameCurrency(total);
        if (total.isZero()) {
            return Percentage.zero();
        }
        BigDecimal pct = this.amount
            .divide(total.amount, 8, ROUNDING)
            .multiply(BigDecimal.valueOf(100));
        return Percentage.of(pct);
    }

    /**
     * Returns the absolute difference between this and other as a percentage of other.
     * Used for drift calculations.
     */
    public Percentage driftFrom(Money target) {
        return this.subtract(target).abs().percentageOf(target);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getDisplayAmount() {
        return amount.setScale(DISPLAY_SCALE, ROUNDING);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency));
        }
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        // Use compareTo to treat 10.0000 == 10.00
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency + " " + amount.toPlainString();
    }
}
