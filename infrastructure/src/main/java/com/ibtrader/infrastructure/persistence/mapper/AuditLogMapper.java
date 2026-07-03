package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.audit.AuditLog;
import com.ibtrader.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLog toDomain(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return AuditLog.builder()
                .id(entity.getId())
                .correlationId(entity.getCorrelationId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .actor(entity.getActor())
                .beforeState(entity.getBeforeState())
                .afterState(entity.getAfterState())
                .details(entity.getDetails())
                .occurredAt(entity.getOccurredAt())
                .build();
    }

    public AuditLogEntity toEntity(AuditLog domain) {
        if (domain == null) {
            return null;
        }
        return AuditLogEntity.builder()
                .id(domain.getId())
                .correlationId(domain.getCorrelationId())
                .entityType(domain.getEntityType())
                .entityId(domain.getEntityId())
                .action(domain.getAction())
                .actor(domain.getActor())
                .beforeState(domain.getBeforeState())
                .afterState(domain.getAfterState())
                .details(domain.getDetails())
                .occurredAt(domain.getOccurredAt())
                .build();
    }
}
