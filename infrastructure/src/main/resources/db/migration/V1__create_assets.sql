-- ============================================================================
-- V1__create_assets.sql
-- Asset registry: all tradeable instruments supported by the system.
-- Assets are configuration-driven and can be enabled/disabled without code changes.
-- ============================================================================

CREATE TABLE assets
(
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    symbol          VARCHAR(20)  NOT NULL,
    exchange        VARCHAR(20)  NOT NULL,              -- e.g., 'SMART', 'CME', 'NASDAQ'
    currency        VARCHAR(10)  NOT NULL DEFAULT 'USD',
    asset_class     VARCHAR(20)  NOT NULL,              -- STOCK, ETF, FUTURES, FOREX, OPTIONS
    ib_con_id       INTEGER,                             -- IB contract ID (populated after resolution)
    multiplier      NUMERIC(18, 6) NOT NULL DEFAULT 1,  -- Contract multiplier (important for futures)
    expiry_date     DATE,                                -- For futures contracts (nullable for equities)
    local_symbol    VARCHAR(50),                         -- IB local symbol (e.g., 'MESM4')
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT assets_symbol_exchange_uq UNIQUE (symbol, exchange),
    CONSTRAINT assets_ib_con_id_uq       UNIQUE (ib_con_id),
    CONSTRAINT assets_asset_class_chk    CHECK (asset_class IN ('STOCK', 'ETF', 'FUTURES', 'FOREX', 'OPTIONS', 'INDEX')),
    CONSTRAINT assets_multiplier_pos_chk CHECK (multiplier > 0)
);

-- Index for common lookups
CREATE INDEX idx_assets_symbol      ON assets (symbol);
CREATE INDEX idx_assets_asset_class ON assets (asset_class);
CREATE INDEX idx_assets_enabled     ON assets (enabled) WHERE enabled = TRUE;

COMMENT ON TABLE  assets              IS 'Registry of all tradeable instruments supported by the system';
COMMENT ON COLUMN assets.ib_con_id   IS 'Interactive Brokers contract ID, resolved at startup via reqContractDetails';
COMMENT ON COLUMN assets.multiplier  IS 'Contract multiplier — e.g., MES=5, MNQ=2, equities=1';
COMMENT ON COLUMN assets.local_symbol IS 'IB-specific local symbol for futures delivery months, e.g. MESM4';
