-- ============================================================================
-- V16__advanced_trading_schema.sql
-- Advanced Rule-Based Trading Engine (Phase 2)
-- Introduces Portfolio Goals, Variable Registry, Indicator Metadata, 
-- Execution Policies, and Strategy Dependencies.
-- ============================================================================

-- ── 1. Portfolio Goal Engine ───────────────────────────────────────────────

CREATE TABLE portfolio_goals (
    id UUID PRIMARY KEY,
    goal_type VARCHAR(50) NOT NULL, -- E.g., 'GROW_PORTFOLIO', 'GENERATE_DIVIDENDS', 'CASH_RESERVE'
    target_value NUMERIC(18, 4) NOT NULL,
    target_currency VARCHAR(10),
    asset_class_target VARCHAR(50), -- E.g., 'EQUITIES'
    sector_target VARCHAR(50),      -- E.g., 'TECHNOLOGY'
    priority INTEGER NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- ── 2. Variable Registry ───────────────────────────────────────────────────

CREATE TABLE variable_registry (
    id UUID PRIMARY KEY,
    variable_name VARCHAR(100) NOT NULL UNIQUE, -- E.g., 'SPY.Close', 'PortfolioValue'
    provider_type VARCHAR(50) NOT NULL, -- E.g., 'MARKET_DATA', 'PORTFOLIO_METRIC', 'CUSTOM'
    configuration JSONB, -- Stores metadata needed to resolve the variable
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ── 3. Indicator Metadata ──────────────────────────────────────────────────

CREATE TABLE indicator_metadata (
    id UUID PRIMARY KEY,
    indicator_name VARCHAR(50) NOT NULL UNIQUE, -- E.g., 'SMA_50', 'RSI_14'
    provider_class VARCHAR(255) NOT NULL, -- Java class responsible for calculation
    default_parameters JSONB, -- E.g., {"period": 14}
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ── 4. Execution Policies ──────────────────────────────────────────────────

CREATE TABLE execution_policies (
    id UUID PRIMARY KEY,
    strategy_version_id UUID NOT NULL REFERENCES trading_strategy_versions(id) ON DELETE CASCADE,
    policy_type VARCHAR(50) NOT NULL, -- IMMEDIATE, TWAP, VWAP, ICEBERG, LIMIT_CHASE
    parameters JSONB, -- Configuration for the policy (e.g., TWAP duration)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ── 5. Strategy Dependencies ───────────────────────────────────────────────

CREATE TABLE strategy_dependencies (
    id UUID PRIMARY KEY,
    parent_strategy_id UUID NOT NULL REFERENCES trading_strategies(id) ON DELETE CASCADE,
    child_strategy_id UUID NOT NULL REFERENCES trading_strategies(id) ON DELETE CASCADE,
    condition_state VARCHAR(50) NOT NULL, -- E.g., 'ONLY_IF_BULLISH'
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (parent_strategy_id, child_strategy_id)
);
