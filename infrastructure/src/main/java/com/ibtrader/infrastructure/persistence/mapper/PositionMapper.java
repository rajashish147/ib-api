package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.portfolio.Position;
import com.ibtrader.infrastructure.persistence.entity.PositionEntity;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {

    public Position toDomain(PositionEntity entity) {
        return Position.builder()
                .id(entity.getId())
                .portfolioId(entity.getPortfolioId())
                .assetId(entity.getAssetId())
                .symbol(entity.getSymbol())
                .quantity(entity.getQuantity())
                .averageCost(Money.of(entity.getAverageCost(), entity.getCurrency()))
                .marketPrice(Money.of(entity.getMarketPrice(), entity.getCurrency()))
                .marketValue(Money.of(entity.getMarketValue(), entity.getCurrency()))
                .unrealizedPnL(Money.of(entity.getUnrealizedPnl(), entity.getCurrency()))
                .realizedPnL(Money.of(entity.getRealizedPnl(), entity.getCurrency()))
                .lastUpdated(entity.getLastUpdated())
                .build();
    }

    public PositionEntity toEntity(Position domain) {
        return PositionEntity.builder()
                .id(domain.getId())
                .portfolioId(domain.getPortfolioId())
                .assetId(domain.getAssetId())
                .symbol(domain.getSymbol())
                .quantity(domain.getQuantity())
                .averageCost(domain.getAverageCost().getAmount())
                .currency(domain.getAverageCost().getCurrency())
                .marketPrice(domain.getMarketPrice().getAmount())
                .marketValue(domain.getMarketValue().getAmount())
                .unrealizedPnl(domain.getUnrealizedPnL().getAmount())
                .realizedPnl(domain.getRealizedPnL().getAmount())
                .lastUpdated(domain.getLastUpdated())
                .build();
    }
}
