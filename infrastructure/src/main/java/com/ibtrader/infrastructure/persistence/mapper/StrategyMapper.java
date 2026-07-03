package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.infrastructure.persistence.entity.TradingStrategyEntity;
import com.ibtrader.infrastructure.persistence.entity.TradingStrategyVersionEntity;
import org.springframework.stereotype.Component;

@Component
public class StrategyMapper {

    public TradingStrategy toDomain(
            TradingStrategyEntity entity, 
            TradingStrategyVersionEntity version, 
            java.util.List<com.ibtrader.infrastructure.persistence.entity.StrategyBasketTargetEntity> targetEntities) {
        
        java.util.List<com.ibtrader.domain.model.strategy.BasketTarget> targets = targetEntities != null ? targetEntities.stream()
                .map(t -> com.ibtrader.domain.model.strategy.BasketTarget.builder()
                        .id(t.getId())
                        .symbol(t.getSymbol())
                        .assetClass(t.getAssetClass())
                        .quantity(t.getQuantity())
                        .build())
                .toList() : java.util.List.of();
                
        com.ibtrader.domain.model.strategy.StrategyState stateEnum = com.ibtrader.domain.model.strategy.StrategyState.IDLE;
        try {
            if (entity.getState() != null) {
                stateEnum = com.ibtrader.domain.model.strategy.StrategyState.valueOf(entity.getState());
            }
        } catch (IllegalArgumentException ignored) {
            // Ignored
        }

        return TradingStrategy.builder()
                .id(entity.getId())
                .versionId(version.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .enabled(entity.isEnabled())
                .cooldownMinutes(entity.getCooldownMinutes())
                .riskProfile(version.getRiskProfile())
                .executionMode(version.getExecutionMode())
                .buyThreshold(entity.getBuyThreshold())
                .sellThreshold(entity.getSellThreshold())
                .state(stateEnum)
                .targets(targets)
                .build();
    }
}
