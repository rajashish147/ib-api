package com.ibtrader.domain.model.strategy;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a single asset target in a trading strategy's basket.
 */
@Getter
@Builder
@ToString
public class BasketTarget {
    private final UUID id;
    private final String symbol;
    private final String assetClass;
    private final BigDecimal quantity;
}
