-- ============================================================================
-- V10__create_audit_logs.sql
-- System-wide audit trail. Every significant action is recorded here.
-- Supports regulatory-style audit requirements for trading systems.
-- This table is append-only — never updated or deleted (regulatory requirement).
-- ============================================================================

CREATE TABLE audit_logs
(
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    correlation_id  VARCHAR(100),            -- HTTP request ID or internal correlation ID
    entity_type     VARCHAR(50)   NOT NULL,  -- 'Order', 'Portfolio', 'Strategy', 'Risk', 'Asset'
    entity_id       UUID,                    -- ID of the entity being acted upon
    action          VARCHAR(100)  NOT NULL,  -- e.g., 'ORDER_SUBMITTED', 'RISK_LIMIT_VIOLATED'
    actor           VARCHAR(50)   NOT NULL DEFAULT 'SYSTEM',  -- 'SYSTEM', 'API_USER', 'SCHEDULER'
    before_state    TEXT,                    -- JSON representation of entity before action
    after_state     TEXT,                    -- JSON representation of entity after action
    details         TEXT,                    -- Free-form context/explanation
    ip_address      INET,                    -- Source IP for API-initiated actions
    occurred_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()

    -- Append-only: no updated_at, no version, no primary key-based updates allowed
);

-- Indexes optimized for audit queries
CREATE INDEX idx_al_correlation_id ON audit_logs (correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_al_entity         ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_al_action         ON audit_logs (action);
CREATE INDEX idx_al_actor          ON audit_logs (actor);
CREATE INDEX idx_al_occurred_at    ON audit_logs (occurred_at DESC);

CREATE INDEX idx_al_recent ON audit_logs (entity_type, occurred_at DESC);

COMMENT ON TABLE  audit_logs              IS 'System-wide append-only audit trail — never updated, never deleted';
COMMENT ON COLUMN audit_logs.actor        IS 'SYSTEM=scheduler/internal, API_USER=REST caller, SCHEDULER=job';
COMMENT ON COLUMN audit_logs.before_state IS 'JSON snapshot of entity state before action (for diff/rollback analysis)';
COMMENT ON COLUMN audit_logs.after_state  IS 'JSON snapshot of entity state after action';
