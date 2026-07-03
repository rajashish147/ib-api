package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.common.Percentage;
import com.ibtrader.domain.model.order.OrderSide;
import com.ibtrader.domain.model.strategy.PlanStatus;
import com.ibtrader.domain.model.strategy.RebalancePlan;
import com.ibtrader.domain.model.strategy.RebalancePlanItem;
import com.ibtrader.domain.model.strategy.StrategyMode;
import com.ibtrader.domain.model.strategy.TriggerType;
import com.ibtrader.infrastructure.persistence.entity.RebalancePlanEntity;
import com.ibtrader.infrastructure.persistence.entity.RebalancePlanItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RebalancePlanMapper {

    public RebalancePlan toDomain(RebalancePlanEntity entity) {
        if (entity == null) {
            return null;
        }

        List<RebalancePlanItem> items = entity.getItems() != null 
            ? entity.getItems().stream().map(this::toDomain).collect(Collectors.toList())
            : List.of();

        return RebalancePlan.builder()
                .id(entity.getId())
                .strategyId(entity.getStrategyId())
                .triggerType(entity.getTriggerType() != null ? TriggerType.valueOf(entity.getTriggerType()) : null)
                .mode(entity.getMode() != null ? StrategyMode.valueOf(entity.getMode()) : null)
                .portfolioNlvAtTrigger(Money.of(entity.getPortfolioNlvAtTrigger(), entity.getPortfolioNlvCurrency()))
                .availableBudget(Money.of(entity.getAvailableBudget(), entity.getAvailableBudgetCurrency()))
                .status(entity.getStatus() != null ? PlanStatus.valueOf(entity.getStatus()) : null)
                .items(items)
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .executedAt(entity.getExecutedAt())
                .completedAt(entity.getCompletedAt())
                .version(entity.getVersion())
                .build();
    }

    public RebalancePlanItem toDomain(RebalancePlanItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return RebalancePlanItem.builder()
                .id(entity.getId())
                .planId(entity.getPlan() != null ? entity.getPlan().getId() : null)
                .assetId(entity.getAssetId())
                .symbol(entity.getSymbol())
                .currentWeight(Percentage.of(entity.getCurrentWeight()))
                .targetWeight(Percentage.of(entity.getTargetWeight()))
                .drift(Percentage.of(entity.getDrift()))
                .currentQuantity(entity.getCurrentQuantity())
                .targetQuantity(entity.getTargetQuantity())
                .quantityDelta(entity.getQuantityDelta())
                .side(entity.getSide() != null ? OrderSide.valueOf(entity.getSide()) : null)
                .estimatedPrice(Money.of(entity.getEstimatedPriceAmount(), entity.getEstimatedPriceCurrency()))
                .estimatedValue(Money.of(entity.getEstimatedValueAmount(), entity.getEstimatedValueCurrency()))
                .orderId(entity.getOrderId())
                .orderPlaced(entity.isOrderPlaced())
                .build();
    }

    public RebalancePlanEntity toEntity(RebalancePlan domain) {
        if (domain == null) {
            return null;
        }

        RebalancePlanEntity entity = RebalancePlanEntity.builder()
                .id(domain.getId())
                .strategyId(domain.getStrategyId())
                .triggerType(domain.getTriggerType() != null ? domain.getTriggerType().name() : null)
                .mode(domain.getMode() != null ? domain.getMode().name() : null)
                .portfolioNlvAtTrigger(domain.getPortfolioNlvAtTrigger() != null 
                        ? domain.getPortfolioNlvAtTrigger().getAmount() : null)
                .portfolioNlvCurrency(domain.getPortfolioNlvAtTrigger() != null 
                        ? domain.getPortfolioNlvAtTrigger().getCurrency() : null)
                .availableBudget(domain.getAvailableBudget() != null 
                        ? domain.getAvailableBudget().getAmount() : null)
                .availableBudgetCurrency(domain.getAvailableBudget() != null 
                        ? domain.getAvailableBudget().getCurrency() : null)
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .executedAt(domain.getExecutedAt())
                .completedAt(domain.getCompletedAt())
                .version(domain.getVersion())
                .build();

        if (domain.getItems() != null) {
            List<RebalancePlanItemEntity> itemEntities = domain.getItems().stream()
                    .map(item -> {
                        RebalancePlanItemEntity itemEntity = toEntity(item);
                        itemEntity.setPlan(entity);
                        return itemEntity;
                    })
                    .collect(Collectors.toList());
            entity.setItems(itemEntities);
        }

        return entity;
    }

    public RebalancePlanItemEntity toEntity(RebalancePlanItem domain) {
        if (domain == null) {
            return null;
        }
        return RebalancePlanItemEntity.builder()
                .id(domain.getId())
                .assetId(domain.getAssetId())
                .symbol(domain.getSymbol())
                .currentWeight(domain.getCurrentWeight().getValue())
                .targetWeight(domain.getTargetWeight().getValue())
                .drift(domain.getDrift().getValue())
                .currentQuantity(domain.getCurrentQuantity())
                .targetQuantity(domain.getTargetQuantity())
                .quantityDelta(domain.getQuantityDelta())
                .side(domain.getSide() != null ? domain.getSide().name() : null)
                .estimatedPriceAmount(domain.getEstimatedPrice() != null ? domain.getEstimatedPrice().getAmount() : null)
                .estimatedPriceCurrency(domain.getEstimatedPrice() != null ? domain.getEstimatedPrice().getCurrency() : null)
                .estimatedValueAmount(domain.getEstimatedValue() != null ? domain.getEstimatedValue().getAmount() : null)
                .estimatedValueCurrency(domain.getEstimatedValue() != null ? domain.getEstimatedValue().getCurrency() : null)
                .orderId(domain.getOrderId())
                .orderPlaced(domain.isOrderPlaced())
                .build();
    }
}
