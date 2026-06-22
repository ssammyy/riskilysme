-- V12: market_data — daily market snapshot driving the scoring pressure model.
-- One row per snapshot_date (UNIQUE). The 05:50 EAT refresh job upserts today's row.
-- Prev/7d-ago values are derived from prior rows by the job; seeded row uses approximate deltas.

CREATE TABLE market_data (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    snapshot_date          DATE           NOT NULL,
    usdkes_spot            NUMERIC(12, 4) NOT NULL,
    usdkes_prev            NUMERIC(12, 4) NOT NULL DEFAULT 0,
    usdkes_7d_ago          NUMERIC(12, 4) NOT NULL DEFAULT 0,
    fuel_price             NUMERIC(10, 2) NOT NULL,
    fuel_prev              NUMERIC(10, 2) NOT NULL DEFAULT 0,
    unga_price             NUMERIC(10, 2) NOT NULL,
    unga_prev              NUMERIC(10, 2) NOT NULL DEFAULT 0,
    cbk_rate               NUMERIC(8, 4)  NOT NULL,
    cbk_prev               NUMERIC(8, 4)  NOT NULL DEFAULT 0,
    kra_deadline_days      INT            NOT NULL DEFAULT 30,
    active_circulars_count INT            NOT NULL DEFAULT 0,
    refreshed_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_market_data_date UNIQUE (snapshot_date)
);

CREATE INDEX ix_market_data_date_desc ON market_data (snapshot_date DESC);

-- Seed: approximate June 2026 values with realistic prev deltas
INSERT INTO market_data (
    snapshot_date, usdkes_spot, usdkes_prev, usdkes_7d_ago,
    fuel_price, fuel_prev, unga_price, unga_prev,
    cbk_rate, cbk_prev, kra_deadline_days, active_circulars_count
) VALUES (
    CURRENT_DATE, 130.5, 131.0, 132.0,
    195.0, 193.0, 180.0, 178.0,
    12.5, 13.0, 30, 0
);

-- Admin-managed commodity prices shown and editable in the CBK rates admin panel
INSERT INTO cbk_rates (rate_type, rate_value, set_by) VALUES
    ('FUEL_KES', 195.0, 'system'),   -- Petrol pump price (KES / litre), ~June 2026
    ('UNGA_KES', 180.0, 'system');   -- Unga 2kg packet price (KES), ~June 2026
