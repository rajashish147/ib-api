package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.common.Percentage;
import com.ibtrader.domain.model.strategy.AllocationTarget;
import com.ibtrader.infrastructure.persistence.entity.AllocationTargetEntity;
import org.springframework.stereotype.Component;

@Component
public class AllocationTargetMapper {

    public AllocationTarget toDomain(AllocationTargetEntity entity) {
        if (entity == null) {
            return null;
        }
        return AllocationTarget.builder()
                .id(entity.getId())
                .strategyId(entity.getStrategyId())
                .assetId(entity.getAssetId())
                .symbol(entity.getSymbol())
                .targetWeight(Percentage.of(entity.getTargetWeight()))
                .enabled(entity.isEnabled())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AllocationTargetEntity toEntity(AllocationTarget domain) {
        if (domain == null) {
            return null;
        }
        return AllocationTargetEntity.builder()
                .id(domain.getId())
                .strategyId(domain.getStrategyId())
                .assetId(domain.getAssetId())
                .symbol(domain.getSymbol())
                .targetWeight(domain.getTargetWeight() != null ? domain.getTargetWeight().getValue() : null)
                .enabled(domain.isEnabled())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
