package com.ibtrader.domain.event;

import com.ibtrader.domain.model.risk.LimitType;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event raised when a risk limit is created or updated.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class RiskLimitUpdatedEvent extends DomainEvent {

    LimitType limitType;
    BigDecimal newValue;
    boolean enabled;

    public RiskLimitUpdatedEvent(UUID aggregateId, LimitType limitType, BigDecimal newValue, boolean enabled) {
        super(aggregateId, "RiskLimit");
        this.limitType = limitType;
        this.newValue = newValue;
        this.enabled = enabled;
    }
}
