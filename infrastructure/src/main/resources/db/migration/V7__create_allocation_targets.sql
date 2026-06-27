-- ============================================================================
-- V7__create_allocation_targets.sql
-- Target portfolio allocation weights per strategy.
-- These drive the FULL_REBALANCE strategy mode (Option A).
-- ============================================================================

CREATE TABLE allocation_targets
(
    id            UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    strategy_id   UUID          NOT NULL REFERENCES strategy_instances (id) ON DELETE CASCADE,
    asset_id      UUID          NOT NULL REFERENCES assets (id),
    symbol        VARCHAR(20)   NOT NULL,           -- Denormalized for fast display
    target_weight NUMERIC(8, 4) NOT NULL,           -- Percentage, e.g., 40.00 = 40%
    enabled       BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version       BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT at_strategy_asset_uq       UNIQUE (strategy_id, asset_id),
    CONSTRAINT at_target_weight_range_chk CHECK (target_weight >= 0 AND target_weight <= 100)
);

-- Trigger to validate total allocation per strategy does not exceed 100%
-- (This is a soft check; the application layer also validates)
CREATE OR REPLACE FUNCTION check_allocation_sum()
RETURNS TRIGGER AS $$
DECLARE
    total_weight NUMERIC;
BEGIN
    SELECT COALESCE(SUM(target_weight), 0)
    INTO total_weight
    FROM allocation_targets
    WHERE strategy_id = NEW.strategy_id
      AND enabled = TRUE
      AND id != COALESCE(NEW.id, gen_random_uuid()); -- Exclude current row on UPDATE

    IF (total_weight + NEW.target_weight) > 100.0001 THEN
        RAISE EXCEPTION 'Total allocation for strategy % would exceed 100%%: current=%, adding=%',
            NEW.strategy_id, total_weight, NEW.target_weight;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_allocation_sum
    BEFORE INSERT OR UPDATE ON allocation_targets
    FOR EACH ROW
    WHEN (NEW.enabled = TRUE)
    EXECUTE FUNCTION check_allocation_sum();

CREATE INDEX idx_at_strategy_id ON allocation_targets (strategy_id);
CREATE INDEX idx_at_asset_id    ON allocation_targets (asset_id);

COMMENT ON TABLE  allocation_targets              IS 'Target allocation weights (%) per strategy — drives FULL_REBALANCE mode';
COMMENT ON COLUMN allocation_targets.target_weight IS 'Target allocation percentage, e.g., 40.00 = 40% of portfolio';
