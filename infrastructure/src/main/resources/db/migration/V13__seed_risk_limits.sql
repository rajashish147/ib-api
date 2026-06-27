-- ============================================================================
-- V13__seed_risk_limits.sql
-- Default risk limit values seeded on first deployment.
-- These match the application.yml defaults and can be updated at runtime
-- via the Risk API without a deployment.
-- ============================================================================

INSERT INTO risk_limits (limit_type, value, enabled, description)
VALUES
    -- Maximum single position size as % of portfolio NLV
    ('MAX_POSITION_SIZE_PCT',       60.0,   TRUE,
     'Maximum single position size as percentage of portfolio NLV. ' ||
     'Prevents catastrophic concentration in a single instrument.'),

    -- Maximum total futures exposure (notional) as % of portfolio NLV
    ('MAX_FUTURES_EXPOSURE_PCT',    20.0,   TRUE,
     'Maximum total futures notional exposure as percentage of portfolio NLV. ' ||
     'Controls leverage from derivative positions.'),

    -- Maximum portfolio leverage ratio (total notional / NLV)
    ('MAX_LEVERAGE',                2.0,    TRUE,
     'Maximum portfolio leverage ratio. ' ||
     'E.g., 2.0 means total notional cannot exceed 2x NLV.'),

    -- Maximum daily P&L loss as % of NLV at start of day
    ('MAX_DAILY_LOSS_PCT',          5.0,    TRUE,
     'Maximum acceptable daily loss as percentage of start-of-day NLV. ' ||
     'Triggers trading halt if breached; resets at market open each day.'),

    -- Maximum drawdown from rolling peak as % of peak NLV
    ('MAX_DRAWDOWN_PCT',            15.0,   TRUE,
     'Maximum drawdown from rolling 30-day peak NLV as a percentage. ' ||
     'Triggers circuit breaker if portfolio falls more than this from peak.'),

    -- Maximum single asset concentration
    ('MAX_CONCENTRATION_PCT',       60.0,   TRUE,
     'Maximum single asset concentration as percentage of portfolio NLV. ' ||
     'Prevents over-concentration even if target allocation is high.'),

    -- Maximum sector concentration
    ('MAX_SECTOR_CONCENTRATION_PCT', 80.0,  TRUE,
     'Maximum single sector concentration as percentage of portfolio NLV. ' ||
     'Prevents all-in on one sector (e.g., all tech ETFs).'),

    -- Emergency stop: hard floor on NLV
    ('EMERGENCY_STOP_NLV',          20000.0, TRUE,
     'Emergency stop if portfolio NLV falls below this absolute dollar amount. ' ||
     'All trading is halted immediately; requires manual restart.')

ON CONFLICT (limit_type) DO NOTHING;  -- Idempotent: do not overwrite user changes on re-migration

COMMENT ON TABLE risk_limits IS
    'Default values seeded by V13; update via Risk API or direct SQL as needed';
