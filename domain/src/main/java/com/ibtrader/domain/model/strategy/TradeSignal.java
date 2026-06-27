package com.ibtrader.domain.model.strategy;

import com.ibtrader.domain.model.order.OrderSide;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Output of the RuleEvaluationEngine. Represents a desire to trade based on a matched rule,
 * but requires validation and conflict resolution by the DecisionEngine before execution.
 */
@Getter
@Builder
@ToString
public class TradeSignal {
    private final UUID id;
    private final UUID strategyId;
    private final String symbol;
    private final OrderSide action;
    private final String quantityType;
    private final BigDecimal quantityValue;
    private final String reason;
    private final double confidence;
    private final Instant generatedAt;
}
