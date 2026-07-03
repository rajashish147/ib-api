package com.ibtrader.api.dto;

import com.ibtrader.domain.model.strategy.TradingStrategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StrategyResponseDto(
        UUID id,
        UUID versionId,
        String name,
        String description,
        int priority,
        boolean enabled,
        int cooldownMinutes,
        String riskProfile,
        String executionMode,
        BigDecimal buyThreshold,
        BigDecimal sellThreshold,
        String state,
        List<BasketTargetDto> targets) {

    public static StrategyResponseDto from(TradingStrategy strategy) {
        List<BasketTargetDto> targetDtos = strategy.getTargets() == null
                ? List.of()
                : strategy.getTargets().stream().map(BasketTargetDto::from).toList();

        return new StrategyResponseDto(
                strategy.getId(),
                strategy.getVersionId(),
                strategy.getName(),
                strategy.getDescription(),
                strategy.getPriority(),
                strategy.isEnabled(),
                strategy.getCooldownMinutes(),
                strategy.getRiskProfile(),
                strategy.getExecutionMode(),
                strategy.getBuyThreshold(),
                strategy.getSellThreshold(),
                strategy.getState() == null ? null : strategy.getState().name(),
                targetDtos);
    }
}
