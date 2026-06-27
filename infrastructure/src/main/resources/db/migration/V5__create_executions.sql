-- ============================================================================
-- V5__create_executions.sql
-- Individual fill records from IB (one row per partial or full fill).
-- IB provides these via execDetails callback with a unique execId per fill.
-- ============================================================================

CREATE TABLE executions
(
    id            UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id      UUID          NOT NULL REFERENCES orders (id),
    ib_order_id   INTEGER       NOT NULL,
    exec_id       VARCHAR(100)  NOT NULL,    -- IB's globally unique execution ID
    account_id    VARCHAR(20)   NOT NULL,
    asset_id      UUID          NOT NULL REFERENCES assets (id),
    symbol        VARCHAR(20)   NOT NULL,
    side          VARCHAR(10)   NOT NULL,    -- BUY, SELL
    quantity      NUMERIC(18, 6) NOT NULL,
    price         NUMERIC(18, 4) NOT NULL,
    commission    NUMERIC(18, 4) NOT NULL DEFAULT 0,
    realized_pnl  NUMERIC(18, 4) NOT NULL DEFAULT 0,
    currency      VARCHAR(10)   NOT NULL DEFAULT 'USD',
    exchange      VARCHAR(20),               -- Which exchange filled the order
    executed_at   TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    -- exec_id is globally unique from IB
    CONSTRAINT executions_exec_id_uq UNIQUE (exec_id),
    CONSTRAINT executions_side_chk   CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT executions_qty_pos_chk CHECK (quantity > 0),
    CONSTRAINT executions_price_pos_chk CHECK (price > 0)
);

CREATE INDEX idx_executions_order_id    ON executions (order_id);
CREATE INDEX idx_executions_asset_id    ON executions (asset_id);
CREATE INDEX idx_executions_account_id  ON executions (account_id, executed_at DESC);
CREATE INDEX idx_executions_exec_id     ON executions (exec_id);
CREATE INDEX idx_executions_date        ON executions (executed_at DESC);

COMMENT ON TABLE  executions          IS 'Individual fill records from IB — one row per execution, never updated';
COMMENT ON COLUMN executions.exec_id IS 'IB globally unique execution ID — used for idempotent processing';
