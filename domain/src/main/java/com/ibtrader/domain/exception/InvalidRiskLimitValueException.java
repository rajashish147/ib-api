package com.ibtrader.domain.exception;

import com.ibtrader.domain.model.risk.LimitType;

import java.math.BigDecimal;

/**
 * Exception thrown when a risk limit value is out of the allowed range for its type.
 */
public class InvalidRiskLimitValueException extends DomainException {

    public InvalidRiskLimitValueException(LimitType limitType, BigDecimal value) {
        super("INVALID_RISK_LIMIT_VALUE", String.format("Value %s is out of range for limit type %s.", value, limitType.name()));
    }
}
