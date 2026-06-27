-- ============================================================================
-- V9__create_risk_limits.sql
-- Configurable risk parameters. One row per LimitType.
-- All pre-trade risk checks read from this table (cached in memory).
-- ============================================================================

CREATE TABLE risk_limits
(
    id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    limit_type  VARCHAR(60)   NOT NULL UNIQUE,  -- Matches LimitType enum
    value       NUMERIC(18, 6) NOT NULL,         -- Percentage or absolute $ depending on type
    enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    description VARCHAR(500)  NOT NULL DEFAULT '',
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version     BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT rl_limit_type_chk CHECK (limit_type IN (
        'MAX_POSITION_SIZE_PCT',
        'MAX_FUTURES_EXPOSURE_PCT',
        'MAX_LEVERAGE',
        'MAX_DAILY_LOSS_PCT',
        'MAX_DRAWDOWN_PCT',
        'MAX_CONCENTRATION_PCT',
        'MAX_SECTOR_CONCENTRATION_PCT',
        'EMERGENCY_STOP_NLV'
    )),
    CONSTRAINT rl_value_positive CHECK (value >= 0)
);

CREATE INDEX idx_rl_limit_type ON risk_limits (limit_type);
CREATE INDEX idx_rl_enabled    ON risk_limits (enabled) WHERE enabled = TRUE;

COMMENT ON TABLE  risk_limits            IS 'Configurable risk limits — updated at runtime without application restart';
COMMENT ON COLUMN risk_limits.value      IS 'For PCT types: percentage 0-100. For EMERGENCY_STOP_NLV: absolute USD amount';
COMMENT ON COLUMN risk_limits.limit_type IS 'Matches LimitType enum — do not change without corresponding code update';
