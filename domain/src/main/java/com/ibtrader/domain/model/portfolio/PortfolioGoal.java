package com.ibtrader.domain.model.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a portfolio goal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioGoal {
    private UUID id;
    private String goalType;
    private BigDecimal targetValue;
    private String targetCurrency;
    private String assetClassTarget;
    private String sectorTarget;
    private Integer priority;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
