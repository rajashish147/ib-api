package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.order.OrderSide;
import com.ibtrader.domain.model.strategy.RuleAction;
import com.ibtrader.infrastructure.persistence.entity.RuleActionEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper component for RuleAction domain model and RuleActionEntity.
 */
@Component
public class RuleActionMapper {

    public RuleAction toDomain(RuleActionEntity entity) {
        if (entity == null) {
            return null;
        }
        return RuleAction.builder()
                .id(entity.getId())
                .actionType(entity.getActionType() != null ? OrderSide.valueOf(entity.getActionType()) : null)
                .quantityType(entity.getQuantityType())
                .quantityValue(entity.getQuantityValue())
                .build();
    }

    public RuleActionEntity toEntity(RuleAction domain) {
        if (domain == null) {
            return null;
        }
        return RuleActionEntity.builder()
                .id(domain.getId())
                .actionType(domain.getActionType() != null ? domain.getActionType().name() : null)
                .quantityType(domain.getQuantityType())
                .quantityValue(domain.getQuantityValue())
                .build();
    }
}
