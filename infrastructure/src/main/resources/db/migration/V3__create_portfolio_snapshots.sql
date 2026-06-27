-- ============================================================================
-- V3__create_portfolio_snapshots.sql
-- Time-series portfolio state snapshots captured every N seconds.
-- This is the primary historical record of portfolio health over time.
-- Table is INSERT-only (never updated) to maintain integrity.
-- ============================================================================

CREATE TABLE portfolio_snapshots
(
    id                      UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    portfolio_id            UUID          NOT NULL,    -- Logical portfolio identifier (account-based)
    account_id              VARCHAR(20)   NOT NULL,
    net_liquidation_value   NUMERIC(18, 4) NOT NULL,
    total_cash_value        NUMERIC(18, 4) NOT NULL,
    available_funds         NUMERIC(18, 4) NOT NULL,
    buying_power            NUMERIC(18, 4) NOT NULL,
    maintenance_margin      NUMERIC(18, 4) NOT NULL DEFAULT 0,
    initial_margin          NUMERIC(18, 4) NOT NULL DEFAULT 0,
    unrealized_pnl          NUMERIC(18, 4) NOT NULL DEFAULT 0,
    realized_pnl            NUMERIC(18, 4) NOT NULL DEFAULT 0,
    position_count          INTEGER       NOT NULL DEFAULT 0,
    currency                VARCHAR(10)   NOT NULL DEFAULT 'USD',
    captured_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    -- Snapshots are never updated (append-only)
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()

    -- No version/updated_at — this table is INSERT-only
);

-- Time-series access pattern: latest snapshot for account, range queries
CREATE INDEX idx_ps_portfolio_id          ON portfolio_snapshots (portfolio_id);
CREATE INDEX idx_ps_account_captured      ON portfolio_snapshots (account_id, captured_at DESC);
CREATE INDEX idx_ps_nlv_captured          ON portfolio_snapshots (net_liquidation_value, captured_at DESC);

-- Supports recent-snapshot queries without a volatile partial-index predicate.
CREATE INDEX idx_ps_recent ON portfolio_snapshots (account_id, captured_at DESC);

COMMENT ON TABLE  portfolio_snapshots IS 'Append-only time-series of portfolio state; never updated';
COMMENT ON COLUMN portfolio_snapshots.portfolio_id IS 'Logical portfolio UUID derived from account_id';
COMMENT ON COLUMN portfolio_snapshots.captured_at  IS 'When this snapshot was taken (system clock)';
