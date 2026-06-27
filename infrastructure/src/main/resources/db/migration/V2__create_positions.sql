-- ============================================================================
-- V2__create_positions.sql
-- Current live positions as reported by IB and reconciled by the system.
-- Positions are always reconciled against IB on startup and after fills.
-- ============================================================================

CREATE TABLE positions
(
    id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    portfolio_id     UUID          NOT NULL,            -- FK to portfolio_snapshots conceptually; references our account
    asset_id         UUID          NOT NULL REFERENCES assets (id),
    symbol           VARCHAR(20)   NOT NULL,            -- Denormalized for fast queries
    quantity         NUMERIC(18, 6) NOT NULL,           -- Positive = long, negative = short
    average_cost     NUMERIC(18, 4) NOT NULL DEFAULT 0,
    currency         VARCHAR(10)   NOT NULL DEFAULT 'USD',
    market_price     NUMERIC(18, 4) NOT NULL DEFAULT 0,
    market_value     NUMERIC(18, 4) NOT NULL DEFAULT 0,
    unrealized_pnl   NUMERIC(18, 4) NOT NULL DEFAULT 0,
    realized_pnl     NUMERIC(18, 4) NOT NULL DEFAULT 0,
    last_updated     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version          BIGINT        NOT NULL DEFAULT 0,

    -- Composite unique: one position per asset per portfolio
    CONSTRAINT positions_portfolio_asset_uq UNIQUE (portfolio_id, asset_id)
);

-- Critical indexes for position lookups
CREATE INDEX idx_positions_portfolio_id ON positions (portfolio_id);
CREATE INDEX idx_positions_asset_id     ON positions (asset_id);
CREATE INDEX idx_positions_symbol       ON positions (symbol);

-- Index for finding non-zero positions (common in queries)
CREATE INDEX idx_positions_nonzero      ON positions (portfolio_id, quantity)
    WHERE quantity <> 0;

COMMENT ON TABLE  positions             IS 'Current live positions — always reconciled against IB account state';
COMMENT ON COLUMN positions.quantity   IS 'Positive = long position; negative = short position; 0 = closed';
COMMENT ON COLUMN positions.currency   IS 'Currency of the position value (usually USD for US stocks)';
