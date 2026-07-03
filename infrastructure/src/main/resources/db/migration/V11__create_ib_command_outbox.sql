-- ============================================================================
-- V11__create_ib_command_outbox.sql
-- Outbox pattern for all Interactive Brokers API calls.
-- 
-- DESIGN: Before any IB API call is made, a row is written here first.
-- A dedicated publisher thread reads unprocessed rows and sends them to IB.
-- On startup, all unprocessed rows are retried — this makes IB commands
-- survivable across application restarts and network failures.
--
-- This is the most critical table for reliability/fault-tolerance.
-- ============================================================================

CREATE TYPE ib_command_status AS ENUM ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED');
CREATE TYPE ib_command_type   AS ENUM (
    'SUBMIT_ORDER',
    'CANCEL_ORDER',
    'MODIFY_ORDER',
    'REQUEST_ACCOUNT_UPDATES',
    'REQUEST_POSITIONS',
    'REQUEST_EXECUTIONS',
    'SUBSCRIBE_MARKET_DATA',
    'UNSUBSCRIBE_MARKET_DATA',
    'REQUEST_CONTRACT_DETAILS',
    'REQUEST_NEXT_ORDER_ID'
);

CREATE TABLE ib_command_outbox
(
    id                UUID            NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    command_type      ib_command_type NOT NULL,
    status            ib_command_status NOT NULL DEFAULT 'PENDING',
    payload           JSONB           NOT NULL,       -- Command-specific parameters
    related_order_id  UUID,                           -- FK to orders (nullable)
    related_asset_id  UUID,                           -- FK to assets (nullable)
    attempt_count     INTEGER         NOT NULL DEFAULT 0,
    max_attempts      INTEGER         NOT NULL DEFAULT 3,
    last_attempt_at   TIMESTAMPTZ,
    next_retry_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    error_message     VARCHAR(1000),
    sent_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()

    -- No version: outbox uses optimistic skip (SELECT FOR UPDATE SKIP LOCKED)
);

-- Index for the publisher: find pending commands ready to send
CREATE INDEX idx_outbox_pending ON ib_command_outbox (status, next_retry_at)
    WHERE status IN ('PENDING', 'PROCESSING', 'FAILED');

CREATE INDEX idx_outbox_related_order ON ib_command_outbox (related_order_id)
    WHERE related_order_id IS NOT NULL;

CREATE INDEX idx_outbox_created_at ON ib_command_outbox (created_at DESC);

-- GIN index for JSONB queries
CREATE INDEX idx_outbox_payload_gin ON ib_command_outbox USING GIN (payload);

COMMENT ON TABLE  ib_command_outbox IS 'Outbox pattern: IB API calls written here before execution — survived across restarts';
COMMENT ON COLUMN ib_command_outbox.next_retry_at IS 'Publisher polls for rows WHERE next_retry_at <= NOW() AND status=PENDING';
COMMENT ON COLUMN ib_command_outbox.max_attempts  IS 'Max retries before status=FAILED; failed commands alert on-call';
