package com.ibtrader.domain.exception;

import com.ibtrader.domain.model.common.Money;

/**
 * Domain exception thrown when an operation requires more funds than are currently
 * available in the account.
 *
 * <p>This exception is raised by the order submission and rebalance-plan execution
 * services when the computed order value exceeds the account's available cash
 * balance or buying power.</p>
 *
 * <p>Use the {@link #of(Money, Money)} factory method to create instances; the
 * message is automatically formatted from the provided {@link Money} values.</p>
 */
public final class InsufficientFundsException extends DomainException {

    /** Stable machine-readable error code for this exception class. */
    public static final String ERROR_CODE = "INSUFFICIENT_FUNDS";

    /**
     * The amount of funds required to complete the operation.
     */
    private final Money required;

    /**
     * The amount of funds actually available at the time of the attempt.
     */
    private final Money available;

    /**
     * Private constructor — use {@link #of(Money, Money)} instead.
     *
     * @param required  funds required by the operation
     * @param available funds currently available
     */
    private InsufficientFundsException(Money required, Money available) {
        super(ERROR_CODE,
              String.format("Insufficient funds: required %s but only %s is available.",
                            required, available));
        this.required  = required;
        this.available = available;
    }

    /**
     * Factory method that creates an {@code InsufficientFundsException} with a
     * formatted message derived from the required and available {@link Money} amounts.
     *
     * @param required  the funds required to complete the operation; must not be {@code null}
     * @param available the funds currently available in the account; must not be {@code null}
     * @return a new {@code InsufficientFundsException}
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public static InsufficientFundsException of(Money required, Money available) {
        if (required == null) throw new IllegalArgumentException("required must not be null");
        if (available == null) throw new IllegalArgumentException("available must not be null");
        return new InsufficientFundsException(required, available);
    }

    /**
     * Returns the funds that were required by the operation.
     *
     * @return the required {@link Money} amount
     */
    public Money getRequired() {
        return required;
    }

    /**
     * Returns the funds that were available at the time of the attempt.
     *
     * @return the available {@link Money} amount
     */
    public Money getAvailable() {
        return available;
    }
}
