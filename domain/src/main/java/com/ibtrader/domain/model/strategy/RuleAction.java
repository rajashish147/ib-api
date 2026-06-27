package com.ibtrader.domain.model.strategy;

import com.ibtrader.domain.model.order.OrderSide;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The action to take when a rule's expression tree evaluates to true.
 */
@Getter
@Builder
@ToString
public class RuleAction {
    private final UUID id;
    private final OrderSide actionType; // BUY, SELL
    private final String quantityType; // SHARES, USD_AMOUNT, PCT_PORTFOLIO, etc.
    private final BigDecimal quantityValue;
    private final String symbol; // Dynamic or static symbol
}
