-- ============================================================================
-- V6__create_strategy_tables.sql
-- Strategy state machine persistence + append-only strategy event log.
-- strategy_state: current materialized state (mutable, 1 row per strategy).
-- strategy_events: append-only event sourcing log (immutable).
-- ============================================================================

-- ── Strategy instances ────────────────────────────────────────────────────────
CREATE TABLE strategy_instances
(
    id                      UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name                    VARCHAR(100)  NOT NULL UNIQUE,
    strategy_type           VARCHAR(30)   NOT NULL,   -- THRESHOLD_REBALANCE, FIXED_AMOUNT, HYBRID
    strategy_mode           VARCHAR(30)   NOT NULL,   -- FULL_REBALANCE, FIXED_AMOUNT, HYBRID
    state                   VARCHAR(30)   NOT NULL DEFAULT 'IDLE',
    buy_threshold           NUMERIC(18, 4) NOT NULL,  -- NLV below this → buy signal
    sell_threshold          NUMERIC(18, 4) NOT NULL,  -- NLV above this → sell signal
    threshold_currency      VARCHAR(10)   NOT NULL DEFAULT 'USD',
    fixed_amount_per_asset  NUMERIC(18, 4),           -- For FIXED_AMOUNT mode (USD per asset)
    enabled                 BOOLEAN       NOT NULL DEFAULT TRUE,
    paused                  BOOLEAN       NOT NULL DEFAULT FALSE,
    last_triggered_at       TIMESTAMPTZ,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version                 BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT si_type_chk  CHECK (strategy_type IN ('THRESHOLD_REBALANCE', 'FIXED_AMOUNT', 'HYBRID')),
    CONSTRAINT si_mode_chk  CHECK (strategy_mode IN ('FULL_REBALANCE', 'FIXED_AMOUNT', 'HYBRID')),
    CONSTRAINT si_state_chk CHECK (state IN ('IDLE', 'BUY_TRIGGERED', 'BUY_EXECUTING', 'BUY_COMPLETED',
                                              'SELL_TRIGGERED', 'SELL_EXECUTING', 'SELL_COMPLETED',
                                              'ERROR', 'RECOVERY')),
    CONSTRAINT si_thresholds_chk CHECK (buy_threshold < sell_threshold),
    CONSTRAINT si_buy_threshold_pos CHECK (buy_threshold > 0),
    CONSTRAINT si_sell_threshold_pos CHECK (sell_threshold > 0)
);

-- ── Strategy event log (append-only, event sourcing) ─────────────────────────
CREATE TABLE strategy_events
(
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    strategy_id     UUID          NOT NULL REFERENCES strategy_instances (id),
    event_type      VARCHAR(60)   NOT NULL,   -- BUY_THRESHOLD_CROSSED, ORDER_SUBMITTED, etc.
    from_state      VARCHAR(30),
    to_state        VARCHAR(30),
    payload         JSONB,                    -- Event-specific data (flexible schema)
    correlation_id  VARCHAR(100),
    actor           VARCHAR(50)   NOT NULL DEFAULT 'SYSTEM',
    occurred_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    sequence_num    BIGSERIAL     NOT NULL    -- Monotonic sequence for ordering

    -- Append-only: no updated_at, no version
);

CREATE INDEX idx_si_state     ON strategy_instances (state);
CREATE INDEX idx_si_enabled   ON strategy_instances (enabled) WHERE enabled = TRUE;

CREATE INDEX idx_se_strategy_id   ON strategy_events (strategy_id, occurred_at DESC);
CREATE INDEX idx_se_event_type    ON strategy_events (event_type);
CREATE INDEX idx_se_correlation   ON strategy_events (correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_se_sequence      ON strategy_events (sequence_num);

-- GIN index on JSONB payload for ad-hoc querying
CREATE INDEX idx_se_payload_gin ON strategy_events USING GIN (payload);

COMMENT ON TABLE  strategy_instances IS 'Strategy configurations with current state machine state';
COMMENT ON TABLE  strategy_events    IS 'Append-only event sourcing log — full audit of every strategy decision';
COMMENT ON COLUMN strategy_events.payload IS 'Event-specific JSONB payload; schema varies by event_type';
COMMENT ON COLUMN strategy_events.sequence_num IS 'Monotonic sequence for strict temporal ordering within a strategy';
