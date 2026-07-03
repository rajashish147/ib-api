package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.risk.LimitType;
import com.ibtrader.domain.model.risk.RiskLimit;
import com.ibtrader.infrastructure.persistence.entity.RiskLimitEntity;
import org.springframework.stereotype.Component;

@Component
public class RiskLimitMapper {

    public RiskLimit toDomain(RiskLimitEntity entity) {
        if (entity == null) {
            return null;
        }
        return RiskLimit.builder()
                .id(entity.getId())
                .limitType(entity.getLimitType() != null ? LimitType.valueOf(entity.getLimitType()) : null)
                .value(entity.getValue())
                .enabled(entity.isEnabled())
                .description(entity.getDescription())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public RiskLimitEntity toEntity(RiskLimit domain) {
        if (domain == null) {
            return null;
        }
        return RiskLimitEntity.builder()
                .id(domain.getId())
                .limitType(domain.getLimitType() != null ? domain.getLimitType().name() : null)
                .value(domain.getValue())
                .enabled(domain.isEnabled())
                .description(domain.getDescription())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
