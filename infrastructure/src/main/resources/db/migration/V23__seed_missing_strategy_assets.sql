-- ============================================================================
-- V23__seed_missing_strategy_assets.sql
-- Ensures all assets referenced by active strategies exist in the assets table.
-- GOOGL is referenced by strategy a5413d02 but was not previously seeded.
-- MSFT is seeded in V18 but added here with DO NOTHING for idempotency.
-- ============================================================================

INSERT INTO assets (symbol, exchange, currency, asset_class, multiplier, enabled)
VALUES
    ('GOOGL', 'SMART', 'USD', 'STOCK', 1, TRUE),
    ('MSFT',  'SMART', 'USD', 'STOCK', 1, TRUE)
ON CONFLICT (symbol, exchange) DO NOTHING;
