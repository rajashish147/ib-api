package com.ibtrader.domain.exception;

import com.ibtrader.domain.model.risk.LimitType;

import java.math.BigDecimal;

/**
 * Domain exception thrown when a risk management check detects that a proposed
 * action (typically an order submission) would breach a configured {@link LimitType}
 * threshold.
 *
 * <p>This exception is raised by the risk evaluation service before an order is
 * forwarded to Interactive Brokers, ensuring that the platform enforces its
 * guardrails at the domain layer regardless of IB-side risk controls.</p>
 *
 * <p>Use the {@link #of(LimitType, BigDecimal, BigDecimal)} factory method to
 * create instances.</p>
 */
public final class RiskLimitViolatedException extends DomainException {

    /** Stable machine-readable error code for this exception class. */
    public static final String ERROR_CODE = "RISK_LIMIT_VIOLATED";

    /**
     * The category of risk limit that was violated.
     */
    private final LimitType limitType;

    /**
     * The configured threshold value that was not to be exceeded.
     */
    private final BigDecimal limitValue;

    /**
     * The actual measured value that breached the configured limit.
     */
    private final BigDecimal actualValue;

    /**
     * Private constructor — use {@link #of(LimitType, BigDecimal, BigDecimal)} instead.
     *
     * @param limitType   the violated limit category
     * @param limitValue  the configured threshold
     * @param actualValue the actual measured value
     */
    private RiskLimitViolatedException(LimitType limitType, BigDecimal limitValue, BigDecimal actualValue) {
        super(ERROR_CODE,
              String.format("Risk limit violated [%s]: configured limit=%s, actual value=%s.",
                            limitType, limitValue, actualValue));
        this.limitType   = limitType;
        this.limitValue  = limitValue;
        this.actualValue = actualValue;
    }

    /**
     * Factory method for creating a {@code RiskLimitViolatedException}.
     *
     * @param limitType   the category of risk limit that was violated; must not be {@code null}
     * @param limitValue  the configured threshold value; must not be {@code null}
     * @param actualValue the actual measured value that breached the limit; must not be {@code null}
     * @return a new {@code RiskLimitViolatedException}
     */
    public static RiskLimitViolatedException of(LimitType limitType, BigDecimal limitValue, BigDecimal actualValue) {
        if (limitType == null)   throw new IllegalArgumentException("limitType must not be null");
        if (limitValue == null)  throw new IllegalArgumentException("limitValue must not be null");
        if (actualValue == null) throw new IllegalArgumentException("actualValue must not be null");
        return new RiskLimitViolatedException(limitType, limitValue, actualValue);
    }

    /**
     * Returns the category of risk limit that was violated.
     *
     * @return the {@link LimitType}
     */
    public LimitType getLimitType() {
        return limitType;
    }

    /**
     * Returns the configured threshold value that was breached.
     *
     * @return the limit value
     */
    public BigDecimal getLimitValue() {
        return limitValue;
    }

    /**
     * Returns the actual measured value that caused the violation.
     *
     * @return the actual value
     */
    public BigDecimal getActualValue() {
        return actualValue;
    }
}
