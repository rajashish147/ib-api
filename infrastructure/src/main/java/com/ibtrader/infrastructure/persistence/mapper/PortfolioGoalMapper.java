package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.portfolio.PortfolioGoal;
import com.ibtrader.infrastructure.persistence.entity.PortfolioGoalEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper component for PortfolioGoal domain model and PortfolioGoalEntity.
 */
@Component
public class PortfolioGoalMapper {

    public PortfolioGoal toDomain(PortfolioGoalEntity entity) {
        if (entity == null) {
            return null;
        }
        return PortfolioGoal.builder()
                .id(entity.getId())
                .goalType(entity.getGoalType())
                .targetValue(entity.getTargetValue())
                .targetCurrency(entity.getTargetCurrency())
                .assetClassTarget(entity.getAssetClassTarget())
                .sectorTarget(entity.getSectorTarget())
                .priority(entity.getPriority())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public PortfolioGoalEntity toEntity(PortfolioGoal domain) {
        if (domain == null) {
            return null;
        }
        return PortfolioGoalEntity.builder()
                .id(domain.getId())
                .goalType(domain.getGoalType())
                .targetValue(domain.getTargetValue())
                .targetCurrency(domain.getTargetCurrency())
                .assetClassTarget(domain.getAssetClassTarget())
                .sectorTarget(domain.getSectorTarget())
                .priority(domain.getPriority())
                .enabled(domain.isEnabled())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }
}
