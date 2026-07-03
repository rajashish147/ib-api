package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.audit.AuditLog;
import com.ibtrader.domain.port.outbound.AuditLogRepository;
import com.ibtrader.infrastructure.persistence.entity.AuditLogEntity;
import com.ibtrader.infrastructure.persistence.mapper.AuditLogMapper;
import com.ibtrader.infrastructure.persistence.repository.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementation for {@link AuditLogRepository}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;
    private final AuditLogMapper mapper;

    @Override
    public AuditLog save(AuditLog logEntry) {
        log.debug("Saving audit log entry for entity {} ({})", logEntry.getEntityType(), logEntry.getEntityId());
        AuditLogEntity entity = mapper.toEntity(logEntry);
        AuditLogEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId) {
        log.debug("Finding audit logs for entity {} ({})", entityType, entityId);
        return jpaRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByCorrelationId(String correlationId) {
        log.debug("Finding audit logs with correlationId {}", correlationId);
        return jpaRepository.findByCorrelationIdOrderByOccurredAtDesc(correlationId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        log.debug("Finding recent audit logs with limit {}", limit);
        return jpaRepository.findRecent(PageRequest.of(0, limit))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
