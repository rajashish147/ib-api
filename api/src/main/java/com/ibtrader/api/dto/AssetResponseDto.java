package com.ibtrader.api.dto;

import com.ibtrader.domain.model.asset.Asset;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read model for the {@code /api/v1/assets} endpoints.
 */
public record AssetResponseDto(
        UUID id,
        String symbol,
        String exchange,
        String currency,
        String assetClass,
        Integer ibConId,
        BigDecimal multiplier,
        boolean enabled,
        boolean resolved) {

    public static AssetResponseDto from(Asset asset) {
        return new AssetResponseDto(
                asset.getId(),
                asset.getSymbol(),
                asset.getExchange(),
                asset.getCurrency(),
                asset.getAssetClass() != null ? asset.getAssetClass().name() : null,
                asset.getIbConId(),
                asset.getMultiplier(),
                asset.isEnabled(),
                asset.isResolved());
    }
}
