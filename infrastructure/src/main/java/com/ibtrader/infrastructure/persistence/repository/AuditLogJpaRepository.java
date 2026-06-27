package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findByEntityTypeAndEntityId(String entityType, UUID entityId);
}
