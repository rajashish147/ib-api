-- ============================================================================
-- V17__add_basket_trading.sql
-- Adds thresholds to strategies and creates a new strategy_basket_targets table.
-- ============================================================================

-- 1. Add threshold columns and state to trading_strategies
ALTER TABLE trading_strategies
ADD COLUMN buy_threshold NUMERIC(18, 4) DEFAULT 0.0,
ADD COLUMN sell_threshold NUMERIC(18, 4) DEFAULT 0.0,
ADD COLUMN state VARCHAR(30) NOT NULL DEFAULT 'IDLE';

-- 2. Create Strategy Basket Targets table
CREATE TABLE strategy_basket_targets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    strategy_id UUID NOT NULL REFERENCES trading_strategies(id) ON DELETE CASCADE,
    symbol VARCHAR(20) NOT NULL,
    asset_class VARCHAR(20) NOT NULL,
    quantity NUMERIC(18, 6) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (strategy_id, symbol)
);

CREATE INDEX idx_strategy_basket_strategy_id ON strategy_basket_targets(strategy_id);
