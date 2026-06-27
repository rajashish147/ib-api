-- ============================================================================
-- V14__seed_assets.sql
-- Default asset registry seeded on first deployment.
-- These are the 5 initial assets from the system configuration.
-- All assets start with enabled=TRUE and unresolved IB contract IDs.
-- IB contract IDs are resolved at application startup via reqContractDetails.
-- ============================================================================

INSERT INTO assets (symbol, exchange, currency, asset_class, multiplier, enabled)
VALUES
    -- ── Equity ETFs ────────────────────────────────────────────────────────────
    ('SPY',  'SMART', 'USD', 'ETF',     1,   TRUE),   -- SPDR S&P 500 ETF Trust
    ('QQQ',  'SMART', 'USD', 'ETF',     1,   TRUE),   -- Invesco QQQ Trust (Nasdaq-100)
    ('VOO',  'SMART', 'USD', 'ETF',     1,   TRUE),   -- Vanguard S&P 500 ETF

    -- ── Micro Futures ─────────────────────────────────────────────────────────
    -- MES = Micro E-mini S&P 500 Futures, multiplier = $5/point
    -- MNQ = Micro E-mini Nasdaq-100 Futures, multiplier = $2/point
    -- Note: Futures require expiry resolution at startup (see IbContractResolver)
    ('MES',  'CME',   'USD', 'FUTURES', 5,   TRUE),
    ('MNQ',  'CME',   'USD', 'FUTURES', 2,   TRUE)

ON CONFLICT (symbol, exchange) DO NOTHING;  -- Idempotent: safe to re-run

-- ── Validate seed data ─────────────────────────────────────────────────────────
DO $$
DECLARE
    asset_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO asset_count FROM assets;
    IF asset_count < 5 THEN
        RAISE WARNING 'Expected at least 5 assets after seed, found: %', asset_count;
    END IF;
    RAISE NOTICE 'Asset seed completed. Total assets in registry: %', asset_count;
END;
$$;
