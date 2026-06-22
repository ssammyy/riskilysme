-- V10: cbk_rates — Central Bank of Kenya reference rates used in scoring pressure formulas.
-- The most recent row per rate_type (by effective_date) is the live rate.
-- Changes are audited via audit_log (action = 'CBK_RATE_CHANGE').

CREATE TABLE cbk_rates (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rate_type      VARCHAR(40)    NOT NULL,
    rate_value     NUMERIC(12, 6) NOT NULL,
    effective_date DATE           NOT NULL DEFAULT CURRENT_DATE,
    set_by         VARCHAR(255)   NOT NULL DEFAULT 'system',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_cbk_rates_type_date   ON cbk_rates (rate_type, effective_date);
CREATE        INDEX ix_cbk_rates_type_latest ON cbk_rates (rate_type, effective_date DESC);

-- Seed: approximate values as of June 2026
INSERT INTO cbk_rates (rate_type, rate_value, set_by) VALUES
    ('CBR',        12.500, 'system'),   -- Central Bank Rate (%)
    ('USD_KES',   130.500, 'system'),   -- USD/KES spot rate
    ('T_BILL_91',  15.800, 'system'),   -- 91-day T-Bill yield (%)
    ('INFLATION',   5.100, 'system');   -- CPI inflation rate (%)
