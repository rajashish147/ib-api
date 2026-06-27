package com.ibtrader.domain.exception;

import java.math.BigDecimal;

/**
 * Domain exception thrown when the sum of allocation percentages across all
 * {@code AllocationTarget} entries for a strategy is invalid — typically because
 * the total exceeds 100% or contains negative values.
 *
 * <p>This exception is raised by the allocation validation service and the strategy
 * configuration use case when saving or updating allocation targets.</p>
 *
 * <p>Use the {@link #allocationExceedsHundredPercent(BigDecimal)} factory method
 * for the most common case.</p>
 */
public final class AllocationException extends DomainException {

    /** Stable machine-readable error code for this exception class. */
    public static final String ERROR_CODE = "INVALID_ALLOCATION";

    /**
     * A concise human-readable description of the specific allocation rule that was violated.
     */
    private final String reason;

    /**
     * The computed total allocation percentage that caused the violation.
     * For example, {@code 105.00} when allocations sum to 105%.
     * May be {@code null} when the error is not related to a sum exceeding 100%.
     */
    private final BigDecimal totalAllocation;

    /**
     * Constructs an {@code AllocationException}.
     *
     * @param reason          concise description of the violated allocation rule
     * @param totalAllocation the total allocation value that caused the violation (may be {@code null})
     * @param message         full human-readable exception message
     */
    private AllocationException(String reason, BigDecimal totalAllocation, String message) {
        super(ERROR_CODE, message);
        this.reason          = reason;
        this.totalAllocation = totalAllocation;
    }

    /**
     * Factory method for the case where allocation weights sum to more than 100%.
     *
     * @param actual the actual total allocation percentage (e.g. {@code 105.00}); must not be {@code null}
     * @return a new {@code AllocationException}
     */
    public static AllocationException allocationExceedsHundredPercent(BigDecimal actual) {
        if (actual == null) throw new IllegalArgumentException("actual must not be null");
        String reason = "Total allocation exceeds 100%";
        return new AllocationException(reason, actual,
                String.format("%s: actual total is %s%%. All allocation targets must sum to 100%% or less.",
                              reason, actual.toPlainString()));
    }

    /**
     * Factory method for generic allocation rule violations where the total allocation
     * is not the primary concern.
     *
     * @param reason  a concise description of the violated rule; must not be blank
     * @return a new {@code AllocationException}
     */
    public static AllocationException of(String reason) {
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("reason must not be null or blank");
        return new AllocationException(reason, null,
                "Allocation configuration is invalid: " + reason);
    }

    /**
     * Returns the concise description of the violated allocation rule.
     *
     * @return the reason string
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns the total allocation percentage that caused the violation, or {@code null}
     * if the error is not related to a percentage sum.
     *
     * @return the total allocation value, or {@code null}
     */
    public BigDecimal getTotalAllocation() {
        return totalAllocation;
    }
}
