-- ============================================================================
-- V4__create_orders.sql
-- Order lifecycle tracking. Every order ever submitted is permanently stored.
-- order_events provides append-only lifecycle history per order.
-- ============================================================================

-- ── Main orders table ─────────────────────────────────────────────────────────
CREATE TABLE orders
(
    id                  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    ib_order_id         INTEGER,                          -- IB's numeric order ID (null until submitted)
    account_id          VARCHAR(20)   NOT NULL,
    asset_id            UUID          NOT NULL REFERENCES assets (id),
    symbol              VARCHAR(20)   NOT NULL,           -- Denormalized
    order_type          VARCHAR(20)   NOT NULL,           -- MARKET, LIMIT, STOP, STOP_LIMIT, etc.
    side                VARCHAR(10)   NOT NULL,           -- BUY, SELL
    quantity            NUMERIC(18, 6) NOT NULL,
    filled_quantity     NUMERIC(18, 6) NOT NULL DEFAULT 0,
    remaining_quantity  NUMERIC(18, 6) NOT NULL,
    limit_price         NUMERIC(18, 4),
    stop_price          NUMERIC(18, 4),
    avg_fill_price      NUMERIC(18, 4),
    currency            VARCHAR(10)   NOT NULL DEFAULT 'USD',
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING_SUBMIT',
    strategy_ref        VARCHAR(100),                     -- Strategy name/ID that generated this order
    rebalance_plan_id   UUID,                             -- FK to rebalance_plans (added later via V12)
    rejection_reason    VARCHAR(500),
    submitted_at        TIMESTAMPTZ,
    last_updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version             BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT orders_ib_order_id_uq     UNIQUE (ib_order_id),
    CONSTRAINT orders_order_type_chk     CHECK (order_type IN ('MARKET', 'LIMIT', 'STOP', 'STOP_LIMIT', 'BRACKET', 'MIDPRICE', 'TRAIL', 'TRAIL_LIMIT')),
    CONSTRAINT orders_side_chk           CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT orders_status_chk         CHECK (status IN ('PENDING_SUBMIT', 'SUBMITTED', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED', 'PENDING_CANCEL', 'INACTIVE', 'ERROR')),
    CONSTRAINT orders_quantity_pos_chk   CHECK (quantity > 0),
    CONSTRAINT orders_filled_lte_qty_chk CHECK (filled_quantity <= quantity),
    CONSTRAINT orders_remaining_chk      CHECK (remaining_quantity >= 0)
);

-- ── Order status event log (append-only) ─────────────────────────────────────
CREATE TABLE order_events
(
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id    UUID         NOT NULL REFERENCES orders (id),
    event_type  VARCHAR(50)  NOT NULL,   -- SUBMITTED, FILLED, PARTIAL_FILL, CANCELLED, REJECTED, ERROR
    from_status VARCHAR(20),
    to_status   VARCHAR(20),
    filled_qty  NUMERIC(18, 6),
    fill_price  NUMERIC(18, 4),
    message     VARCHAR(500),
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()

    -- Append-only: no updated_at or version
);

-- Indexes for order queries
CREATE INDEX idx_orders_status         ON orders (status);
CREATE INDEX idx_orders_account_id     ON orders (account_id);
CREATE INDEX idx_orders_asset_id       ON orders (asset_id);
CREATE INDEX idx_orders_ib_order_id    ON orders (ib_order_id);
CREATE INDEX idx_orders_strategy_ref   ON orders (strategy_ref);
CREATE INDEX idx_orders_rebalance_plan ON orders (rebalance_plan_id) WHERE rebalance_plan_id IS NOT NULL;
CREATE INDEX idx_orders_created_at     ON orders (created_at DESC);

-- Partial indexes for active order recovery
CREATE INDEX idx_orders_open ON orders (account_id, status)
    WHERE status IN ('SUBMITTED', 'PARTIALLY_FILLED', 'PENDING_CANCEL', 'PENDING_SUBMIT');

-- Order events index
CREATE INDEX idx_order_events_order_id    ON order_events (order_id, occurred_at DESC);
CREATE INDEX idx_order_events_event_type  ON order_events (event_type);

COMMENT ON TABLE  orders                  IS 'Permanent record of all orders submitted, including historical and cancelled';
COMMENT ON COLUMN orders.ib_order_id     IS 'IB numeric order ID; null until order is accepted by IB API';
COMMENT ON COLUMN orders.strategy_ref    IS 'Free-form reference to the strategy or plan that created this order';
COMMENT ON TABLE  order_events            IS 'Append-only lifecycle event log per order — never updated';
