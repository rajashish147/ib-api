package com.ibtrader.domain.model.audit;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable entity representing a single entry in the system-wide audit log.
 *
 * <p>Every significant action that mutates domain state — order submissions,
 * strategy transitions, rebalance plan approvals, risk limit changes — must
 * produce an {@code AuditLog} entry. Entries are write-once and must never be
 * mutated after persistence.
 *
 * <p>The audit log supports two primary actors:
 * <ul>
 *   <li><strong>SYSTEM</strong> — actions initiated autonomously by the
 *       strategy engine, scheduler, or IB callback handlers.</li>
 *   <li><strong>API_USER</strong> — actions initiated by an authenticated
 *       user via the REST API.</li>
 * </ul>
 *
 * <p>Before/after state is captured as JSON strings so that a full diff of
 * every mutation is retained without coupling the audit layer to the domain
 * model's internal structure.
 *
 * <p>Use the factory methods ({@link #system}, {@link #api}) rather than
 * the Lombok builder for typical creation scenarios. The builder is retained
 * for ORM / mapping frameworks.
 */
@Getter
@Builder
@EqualsAndHashCode(of = "id")
public class AuditLog {

    // =========================================================================
    // Predefined actor constants
    // =========================================================================

    /** Actor identifier for actions originating from internal system processes. */
    public static final String ACTOR_SYSTEM    = "SYSTEM";

    /** Actor identifier for actions originating from the REST API. */
    public static final String ACTOR_API_USER  = "API_USER";

    /** Actor identifier for actions originating from the scheduler. */
    public static final String ACTOR_SCHEDULER = "SCHEDULER";

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Surrogate key unique across all audit log entries. */
    private final UUID id;

    /**
     * Correlation ID for distributed tracing. Allows all log entries produced
     * during a single request or job execution to be grouped together.
     * May be {@code null} for system-initiated events without a parent trace.
     */
    private final String correlationId;

    // -------------------------------------------------------------------------
    // What was affected
    // -------------------------------------------------------------------------

    /**
     * Simple name of the domain entity that was affected,
     * e.g. {@code "Order"}, {@code "Portfolio"}, {@code "StrategyInstance"}.
     */
    private final String entityType;

    /** UUID of the specific entity that was affected. */
    private final UUID entityId;

    // -------------------------------------------------------------------------
    // What happened
    // -------------------------------------------------------------------------

    /**
     * Upper-snake-case action identifier describing the mutation,
     * e.g. {@code "ORDER_SUBMITTED"}, {@code "STRATEGY_TRANSITIONED"},
     * {@code "PLAN_APPROVED"}.
     */
    private final String action;

    /**
     * Identity of the actor who performed the action.
     * Standard values: {@link #ACTOR_SYSTEM}, {@link #ACTOR_API_USER},
     * {@link #ACTOR_SCHEDULER}.
     */
    private final String actor;

    // -------------------------------------------------------------------------
    // State capture
    // -------------------------------------------------------------------------

    /**
     * JSON-serialised state of the entity <em>before</em> the mutation.
     * {@code null} for creation events.
     */
    private final String beforeState;

    /**
     * JSON-serialised state of the entity <em>after</em> the mutation.
     * {@code null} for deletion events.
     */
    private final String afterState;

    /**
     * Free-form human-readable description of the event context, additional
     * parameters, or rationale.
     */
    private final String details;

    // -------------------------------------------------------------------------
    // Timestamp
    // -------------------------------------------------------------------------

    /** Wall-clock time at which the audited action occurred. */
    private final Instant occurredAt;

    // =========================================================================
    // Factories
    // =========================================================================

    /**
     * Creates an audit log entry for an action initiated by the internal
     * system (strategy engine, callbacks, background jobs). No correlation
     * ID is recorded.
     *
     * @param entityType simple domain entity name (e.g. {@code "Order"})
     * @param entityId   UUID of the affected entity
     * @param action     upper-snake-case action code
     * @param details    optional free-form context description
     * @return an immutable audit log entry attributed to {@value #ACTOR_SYSTEM}
     */
    public static AuditLog system(
            String entityType,
            UUID entityId,
            String action,
            String details) {

        return AuditLog.builder()
                .id(UUID.randomUUID())
                .correlationId(null)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(ACTOR_SYSTEM)
                .beforeState(null)
                .afterState(null)
                .details(details)
                .occurredAt(Instant.now())
                .build();
    }

    /**
     * Creates an audit log entry for an action initiated via the REST API.
     * The {@code correlationId} should be extracted from the incoming request's
     * trace header (e.g. {@code X-Correlation-ID}) to enable distributed
     * tracing across service boundaries.
     *
     * @param correlationId distributed trace / correlation identifier
     * @param entityType    simple domain entity name
     * @param entityId      UUID of the affected entity
     * @param action        upper-snake-case action code
     * @param details       optional free-form context description
     * @return an immutable audit log entry attributed to {@value #ACTOR_API_USER}
     */
    public static AuditLog api(
            String correlationId,
            String entityType,
            UUID entityId,
            String action,
            String details) {

        return AuditLog.builder()
                .id(UUID.randomUUID())
                .correlationId(correlationId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(ACTOR_API_USER)
                .beforeState(null)
                .afterState(null)
                .details(details)
                .occurredAt(Instant.now())
                .build();
    }

    /**
     * Creates an audit log entry with full before/after state capture.
     * This is the richest form of audit entry and should be used whenever the
     * serialised entity state is available.
     *
     * @param correlationId distributed trace / correlation identifier (may be null)
     * @param actor         actor identifier string
     * @param entityType    simple domain entity name
     * @param entityId      UUID of the affected entity
     * @param action        upper-snake-case action code
     * @param beforeState   JSON-serialised pre-mutation state (may be null)
     * @param afterState    JSON-serialised post-mutation state (may be null)
     * @param details       optional free-form context description
     * @return a fully-populated immutable audit log entry
     */
    public static AuditLog withState(
            String correlationId,
            String actor,
            String entityType,
            UUID entityId,
            String action,
            String beforeState,
            String afterState,
            String details) {

        return AuditLog.builder()
                .id(UUID.randomUUID())
                .correlationId(correlationId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(actor)
                .beforeState(beforeState)
                .afterState(afterState)
                .details(details)
                .occurredAt(Instant.now())
                .build();
    }
}
