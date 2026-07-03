package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.infrastructure.persistence.entity.TradeSignalEntity;
import org.springframework.stereotype.Component;

@Component
public class TradeSignalMapper {

    /**
     * Maps TradeSignal domain model to TradeSignalEntity.
     */
    public TradeSignalEntity toEntity(TradeSignal domain) {
        if (domain == null) {
            return null;
        }

        return TradeSignalEntity.builder()
                .id(domain.getId())
                .strategyId(domain.getStrategyId())
                .symbol(domain.getSymbol())
                .action(domain.getAction())
                .quantityType(domain.getQuantityType())
                .quantityValue(domain.getQuantityValue())
                .reason(domain.getReason())
                .confidence(domain.getConfidence())
                .generatedAt(domain.getGeneratedAt())
                .build();
    }

    /**
     * Maps TradeSignalEntity to TradeSignal domain model.
     */
    public TradeSignal toDomain(TradeSignalEntity entity) {
        if (entity == null) {
            return null;
        }

        return TradeSignal.builder()
                .id(entity.getId())
                .strategyId(entity.getStrategyId())
                .symbol(entity.getSymbol())
                .action(entity.getAction())
                .quantityType(entity.getQuantityType())
                .quantityValue(entity.getQuantityValue())
                .reason(entity.getReason())
                .confidence(entity.getConfidence())
                .generatedAt(entity.getGeneratedAt())
                .build();
    }
}
