-- ============================================================================
-- V22__create_strategy_execution_history.sql
-- Records each execution attempt of a trading strategy by the engine pipeline.
-- Used for cooldown enforcement, audit trails, and performance analysis.
-- ============================================================================

CREATE TABLE strategy_execution_history
(
    id           UUID                     NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    strategy_id  UUID                     NOT NULL,
    executed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    successful   BOOLEAN                  NOT NULL DEFAULT FALSE,
    reason       VARCHAR(1000)
);

CREATE INDEX idx_strategy_exec_history_strategy_id ON strategy_execution_history (strategy_id);
CREATE INDEX idx_strategy_exec_history_executed_at ON strategy_execution_history (executed_at DESC);
