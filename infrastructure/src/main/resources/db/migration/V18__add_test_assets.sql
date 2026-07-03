-- ============================================================================
-- V15__add_test_assets.sql
-- Add tech stocks for testing the basket strategy.
-- ============================================================================

INSERT INTO assets (symbol, exchange, currency, asset_class, multiplier, enabled)
VALUES
    ('AAPL',  'SMART', 'USD', 'STOCK', 1, TRUE),
    ('MSFT',  'SMART', 'USD', 'STOCK', 1, TRUE),
    ('SNDK',  'SMART', 'USD', 'STOCK', 1, TRUE),
    ('META',  'SMART', 'USD', 'STOCK', 1, TRUE),
    ('NVDA',  'SMART', 'USD', 'STOCK', 1, TRUE)
ON CONFLICT (symbol, exchange) DO NOTHING;
