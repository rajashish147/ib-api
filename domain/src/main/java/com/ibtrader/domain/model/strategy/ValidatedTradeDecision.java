package com.ibtrader.domain.model.strategy;

import com.ibtrader.domain.model.order.OrderSide;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Output of the DecisionEngine. Represents a confirmed desire to trade after 
 * evaluating signals for conflicts and cooldowns. Next step is the Risk Engine.
 */
@Getter
@Builder
@ToString
public class ValidatedTradeDecision {
    private final UUID id;
    private final UUID sourceSignalId;
    private final UUID strategyId;
    private final String symbol;
    private final OrderSide action;
    private final String quantityType;
    private final BigDecimal quantityValue;
    private final Instant decisionTime;
}
