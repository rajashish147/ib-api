package com.ibtrader.domain.exception;

/**
 * Abstract base class for all domain-specific exceptions in the IBKR trading platform.
 *
 * <p>All business-rule violations, invariant breaches, and domain-logic errors should
 * be represented by concrete subclasses of {@code DomainException}.  Infrastructure or
 * integration failures should be wrapped by subclasses where appropriate, carrying the
 * original cause via the two-argument constructor.</p>
 *
 * <p>Every domain exception carries an {@code errorCode} — a stable, machine-readable
 * string that consumers (REST controllers, event handlers, client SDKs) can use for
 * programmatic error handling without parsing the human-readable message.</p>
 *
 * <p>Naming convention for error codes: {@code DOMAIN_CONCEPT_REASON}, e.g.
 * {@code ORDER_NOT_FOUND}, {@code RISK_LIMIT_VIOLATED}.</p>
 */
public abstract class DomainException extends RuntimeException {

    /**
     * A stable, machine-readable error code that uniquely identifies the class of
     * domain error.  Should follow the {@code SCREAMING_SNAKE_CASE} convention.
     */
    private final String errorCode;

    /**
     * Constructs a {@code DomainException} with the supplied error code and message.
     *
     * @param errorCode a stable, machine-readable identifier for this class of error;
     *                  must not be {@code null} or blank
     * @param message   a human-readable description of the error
     */
    protected DomainException(String errorCode, String message) {
        super(message);
        validateErrorCode(errorCode);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a {@code DomainException} with the supplied error code, message, and
     * a wrapped root cause.
     *
     * @param errorCode a stable, machine-readable identifier for this class of error;
     *                  must not be {@code null} or blank
     * @param message   a human-readable description of the error
     * @param cause     the underlying exception that triggered this domain error;
     *                  may be {@code null}
     */
    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        validateErrorCode(errorCode);
        this.errorCode = errorCode;
    }

    /**
     * Convenience constructor that derives the error code from the simple class name.
     * Intended for rapid subclass prototyping; production subclasses should prefer the
     * explicit {@code errorCode} constructors for stability.
     *
     * @param message a human-readable description of the error
     */
    protected DomainException(String message) {
        super(message);
        this.errorCode = getClass().getSimpleName()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
    }

    /**
     * Returns the stable, machine-readable error code identifying this class of error.
     *
     * @return the error code; never {@code null}
     */
    public String getErrorCode() {
        return errorCode;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void validateErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode must not be null or blank");
        }
    }
}
