package com.ibtrader.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for registering a new asset via {@code POST /api/v1/assets}.
 */
public record AssetRequestDto(
        @NotBlank(message = "Symbol is required")
        String symbol,
        @NotBlank(message = "Exchange is required")
        String exchange,
        @NotBlank(message = "Currency is required")
        String currency,
        @NotNull(message = "Asset class is required")
        String assetClass,
        @DecimalMin(value = "0.0", inclusive = false, message = "Multiplier must be positive")
        BigDecimal multiplier) {
}
