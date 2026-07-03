package com.ibtrader.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record StrategyRequestDto(
        @NotBlank(message = "Strategy name is required")
        String name,
        String description,
        @Min(value = 0, message = "Priority must be zero or greater")
        Integer priority,
        Boolean enabled,
        @Min(value = 0, message = "Cooldown must be zero or greater")
        Integer cooldownMinutes,
        String riskProfile,
        String executionMode,
        @DecimalMin(value = "0.0", message = "Buy threshold must be zero or greater")
        BigDecimal buyThreshold,
        @DecimalMin(value = "0.0", message = "Sell threshold must be zero or greater")
        BigDecimal sellThreshold,
        @Valid
        List<@Valid BasketTargetDto> targets) {

    @AssertTrue(message = "Sell threshold must be greater than buy threshold")
    public boolean isThresholdOrderValid() {
        if (!isPositive(buyThreshold) || !isPositive(sellThreshold)) {
            return true;
        }
        return sellThreshold.compareTo(buyThreshold) > 0;
    }

    @AssertTrue(message = "Basket strategy requires at least one target asset")
    public boolean isBasketTargetsValid() {
        if (!isPositive(buyThreshold)) {
            return true;
        }
        return targets != null && !targets.isEmpty();
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
