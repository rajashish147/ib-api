-- ============================================================================
-- V15__rule_engine_schema.sql
-- Advanced Rule-Based Trading Engine (Phase 1)
-- Introduces dynamic versioned strategies, expression trees, actions, 
-- advanced watchlists, and evaluation history.
-- ============================================================================

-- ── 1. Versioned Strategies ────────────────────────────────────────────────

CREATE TABLE trading_strategies (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    priority INTEGER NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    cooldown_minutes INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE trading_strategy_versions (
    id UUID PRIMARY KEY,
    strategy_id UUID NOT NULL REFERENCES trading_strategies(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    risk_profile VARCHAR(50) NOT NULL,
    execution_mode VARCHAR(20) NOT NULL, -- LIVE, PAPER, BACKTEST, SIMULATION, DISABLED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (strategy_id, version_number)
);

-- ── 2. Expression Trees (Rules) ──────────────────────────────────────────

CREATE TABLE expression_nodes (
    id UUID PRIMARY KEY,
    strategy_version_id UUID NOT NULL REFERENCES trading_strategy_versions(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES expression_nodes(id) ON DELETE CASCADE,
    node_type VARCHAR(20) NOT NULL, -- LOGICAL_AND, LOGICAL_OR, CONDITION
    left_operand VARCHAR(100), -- E.g., 'PortfolioValue', 'AAPL.Close'
    operator VARCHAR(20), -- '>', '<=', '==', 'BETWEEN', 'IN'
    right_operand VARCHAR(255), -- E.g., '100000', 'Cash'
    rule_description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ── 3. Rule Actions ────────────────────────────────────────────────────────

CREATE TABLE rule_actions (
    id UUID PRIMARY KEY,
    strategy_version_id UUID NOT NULL REFERENCES trading_strategy_versions(id) ON DELETE CASCADE,
    action_type VARCHAR(30) NOT NULL, -- BUY, SELL, BUY_PERCENT, CLOSE_POSITION, etc.
    quantity_type VARCHAR(30) NOT NULL, -- SHARES, USD_AMOUNT, PCT_CASH, PCT_PORTFOLIO, RISK_BASED
    quantity_value NUMERIC(18, 6),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ── 4. Advanced Watchlists ─────────────────────────────────────────────────

CREATE TABLE watchlists (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    priority INTEGER NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE watchlist_symbols (
    id UUID PRIMARY KEY,
    watchlist_id UUID NOT NULL REFERENCES watchlists(id) ON DELETE CASCADE,
    symbol VARCHAR(20) NOT NULL,
    sector VARCHAR(50),
    industry VARCHAR(50),
    tags JSONB, -- E.g., ["AI", "Dividend"]
    allocation_limit NUMERIC(8, 4), -- Max percentage of portfolio allowed
    notes TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (watchlist_id, symbol)
);

-- ── 5. Evaluation History ──────────────────────────────────────────────────

CREATE TABLE evaluation_history (
    id UUID PRIMARY KEY,
    strategy_id UUID NOT NULL REFERENCES trading_strategies(id),
    strategy_version_id UUID NOT NULL REFERENCES trading_strategy_versions(id),
    evaluation_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    portfolio_snapshot_id UUID, -- Reference to portfolio_snapshots if applicable
    matched_rules JSONB,
    unmatched_rules JSONB,
    generated_signals JSONB,
    rejected_signals JSONB,
    rejection_reasons JSONB,
    planned_orders JSONB,
    execution_outcome VARCHAR(50)
);

CREATE INDEX idx_eval_history_strategy ON evaluation_history(strategy_id, evaluation_time DESC);
