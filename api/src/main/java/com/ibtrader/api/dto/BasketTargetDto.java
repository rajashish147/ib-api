package com.ibtrader.api.dto;

import com.ibtrader.domain.model.strategy.BasketTarget;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record BasketTargetDto(
        UUID id,
        @NotBlank(message = "Target symbol is required")
        String symbol,
        @NotBlank(message = "Target asset class is required")
        String assetClass,
        @DecimalMin(value = "0.000001", message = "Target quantity must be positive")
        BigDecimal quantity) {

    public BasketTarget toDomain() {
        return BasketTarget.builder()
                .id(id)
                .symbol(symbol == null ? null : symbol.trim().toUpperCase())
                .assetClass(assetClass)
                .quantity(quantity)
                .build();
    }

    public static BasketTargetDto from(BasketTarget target) {
        return new BasketTargetDto(
                target.getId(),
                target.getSymbol(),
                target.getAssetClass(),
                target.getQuantity());
    }
}
