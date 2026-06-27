-- ============================================================================
-- V8__create_market_data_cache.sql
-- Persistent price cache for assets. In-memory cache is backed by this table
-- for recovery after restart. Updated via IB real-time market data callbacks.
-- ============================================================================

CREATE TABLE market_data_cache
(
    id            UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    asset_id      UUID          NOT NULL REFERENCES assets (id) ON DELETE CASCADE UNIQUE,
    symbol        VARCHAR(20)   NOT NULL,
    last_price    NUMERIC(18, 6) NOT NULL,
    bid_price     NUMERIC(18, 6),
    ask_price     NUMERIC(18, 6),
    open_price    NUMERIC(18, 6),
    high_price    NUMERIC(18, 6),
    low_price     NUMERIC(18, 6),
    close_price   NUMERIC(18, 6),
    volume        NUMERIC(18, 0),
    currency      VARCHAR(10)   NOT NULL DEFAULT 'USD',
    ticker_id     INTEGER,                  -- IB ticker request ID (for unsubscribe)
    price_at      TIMESTAMPTZ   NOT NULL,   -- When IB provided this price
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()

    -- No version: last-writer-wins for market data is acceptable
);

CREATE INDEX idx_mdc_asset_id  ON market_data_cache (asset_id);
CREATE INDEX idx_mdc_symbol    ON market_data_cache (symbol);
CREATE INDEX idx_mdc_price_at  ON market_data_cache (price_at);

COMMENT ON TABLE  market_data_cache           IS 'Persistent price cache backed by IB real-time market data';
COMMENT ON COLUMN market_data_cache.ticker_id IS 'IB reqMktData tickerId — used to cancel subscription on shutdown';
COMMENT ON COLUMN market_data_cache.price_at  IS 'Timestamp when IB delivered this price (not when we stored it)';
