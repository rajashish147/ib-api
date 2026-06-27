-- ============================================================================
-- V12__create_rebalance_plans.sql
-- Rebalance plan header + line items.
-- Plans are generated before execution and can require approval (configurable).
-- All plans are permanently stored for audit purposes.
-- ============================================================================

CREATE TABLE rebalance_plans
(
    id                      UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    strategy_id             UUID          NOT NULL REFERENCES strategy_instances (id),
    trigger_type            VARCHAR(20)   NOT NULL,   -- BUY_THRESHOLD, SELL_THRESHOLD, SCHEDULED, MANUAL
    strategy_mode           VARCHAR(20)   NOT NULL,   -- FULL_REBALANCE, FIXED_AMOUNT, HYBRID
    portfolio_nlv_at_trigger NUMERIC(18, 4) NOT NULL, -- NLV that triggered the plan
    available_budget        NUMERIC(18, 4) NOT NULL,  -- Cash available to deploy
    currency                VARCHAR(10)   NOT NULL DEFAULT 'USD',
    status                  VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    notes                   TEXT,
    executed_at             TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version                 BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT rp_trigger_type_chk CHECK (trigger_type IN ('BUY_THRESHOLD', 'SELL_THRESHOLD', 'SCHEDULED', 'MANUAL')),
    CONSTRAINT rp_mode_chk         CHECK (strategy_mode IN ('FULL_REBALANCE', 'FIXED_AMOUNT', 'HYBRID')),
    CONSTRAINT rp_status_chk       CHECK (status IN ('DRAFT', 'APPROVED', 'EXECUTING', 'COMPLETED', 'CANCELLED', 'FAILED')),
    CONSTRAINT rp_budget_pos_chk   CHECK (available_budget >= 0)
);

-- ── Rebalance plan line items ─────────────────────────────────────────────────
CREATE TABLE rebalance_plan_items
(
    id                UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    plan_id           UUID          NOT NULL REFERENCES rebalance_plans (id) ON DELETE CASCADE,
    asset_id          UUID          NOT NULL REFERENCES assets (id),
    symbol            VARCHAR(20)   NOT NULL,
    current_weight    NUMERIC(8, 4) NOT NULL DEFAULT 0,    -- Current % allocation
    target_weight     NUMERIC(8, 4) NOT NULL DEFAULT 0,    -- Target % allocation
    drift             NUMERIC(8, 4) NOT NULL DEFAULT 0,    -- Abs difference in weights
    current_quantity  NUMERIC(18, 6) NOT NULL DEFAULT 0,
    target_quantity   NUMERIC(18, 6) NOT NULL DEFAULT 0,
    quantity_delta    NUMERIC(18, 6) NOT NULL,              -- +ve = buy, -ve = sell
    side              VARCHAR(10)   NOT NULL,               -- BUY, SELL
    estimated_price   NUMERIC(18, 4) NOT NULL,
    estimated_value   NUMERIC(18, 4) NOT NULL,             -- Abs(qty_delta * estimated_price)
    currency          VARCHAR(10)   NOT NULL DEFAULT 'USD',
    order_id          UUID,                                  -- Set after order placed
    order_placed      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT rpi_side_chk        CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT rpi_price_pos_chk   CHECK (estimated_price > 0),
    CONSTRAINT rpi_value_pos_chk   CHECK (estimated_value >= 0),
    CONSTRAINT rpi_weight_range_chk CHECK (current_weight >= 0 AND current_weight <= 100
                                       AND target_weight >= 0 AND target_weight <= 100)
);

-- Now add the FK from orders to rebalance_plans (orders was created in V4)
ALTER TABLE orders ADD CONSTRAINT orders_rebalance_plan_fk
    FOREIGN KEY (rebalance_plan_id) REFERENCES rebalance_plans (id);

CREATE INDEX idx_rp_strategy_id   ON rebalance_plans (strategy_id, created_at DESC);
CREATE INDEX idx_rp_status        ON rebalance_plans (status);
CREATE INDEX idx_rpi_plan_id      ON rebalance_plan_items (plan_id);
CREATE INDEX idx_rpi_asset_id     ON rebalance_plan_items (asset_id);
CREATE INDEX idx_rpi_order_id     ON rebalance_plan_items (order_id) WHERE order_id IS NOT NULL;

COMMENT ON TABLE  rebalance_plans                  IS 'Generated rebalance plans — stored permanently for audit';
COMMENT ON TABLE  rebalance_plan_items             IS 'Individual trade legs within a rebalance plan';
COMMENT ON COLUMN rebalance_plan_items.quantity_delta IS '+ve = buy (increase position), -ve = sell (reduce position)';
