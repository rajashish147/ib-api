package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.audit.AuditLog;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port (repository) for {@link AuditLog} persistence.
 *
 * <p>Audit logs are append-only records of significant actions performed within
 * the platform.  They must never be updated or deleted once written.  Queries
 * support operational dashboards, compliance reporting, and debugging workflows.</p>
 */
public interface AuditLogRepository {

    /**
     * Persists a new audit log entry.
     *
     * <p>This method always inserts a new record.  Existing audit entries are
     * immutable and must not be modified.</p>
     *
     * @param log the audit log entry to persist; must not be {@code null}
     * @return the persisted audit log entry
     */
    AuditLog save(AuditLog log);

    /**
     * Retrieves all audit log entries for a specific entity type and entity identifier,
     * ordered from most recent to oldest.
     *
     * <p>For example, to retrieve all audit events for an order aggregate, pass
     * {@code entityType = "Order"} and {@code entityId = <order-uuid>}.</p>
     *
     * @param entityType the logical entity type name (e.g. {@code "Order"},
     *                   {@code "Portfolio"}); must not be blank
     * @param entityId   the domain UUID of the entity; must not be {@code null}
     * @return a (possibly empty) list of matching audit entries; never {@code null}
     */
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    /**
     * Retrieves all audit log entries that share a correlation identifier.
     *
     * <p>A correlation ID links multiple audit entries that were generated as part of
     * the same logical business operation (e.g. a rebalance plan execution that spawns
     * multiple orders).</p>
     *
     * @param correlationId the correlation identifier; must not be blank
     * @return a (possibly empty) list of related audit entries; never {@code null}
     */
    List<AuditLog> findByCorrelationId(String correlationId);

    /**
     * Retrieves the most recent audit log entries across all entity types.
     *
     * <p>Useful for populating an operator activity feed or a recent-events widget
     * on the admin dashboard.</p>
     *
     * @param limit the maximum number of entries to return; must be positive
     * @return a list of the most recent audit entries (up to {@code limit}); never {@code null}
     */
    List<AuditLog> findRecent(int limit);
}
