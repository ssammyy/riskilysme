-- V3: scoring_config — versioned calibration constants for the Business Health Score.
-- The scoring engine (Sprint 2) reads ALL constants from here; nothing is hardcoded.
-- value_json holds a JSON scalar or object; config_version lets us re-calibrate without redeploy.

CREATE TABLE scoring_config (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_version VARCHAR(20)  NOT NULL,
    config_key     VARCHAR(100) NOT NULL,
    value_json     TEXT         NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_scoring_config_version_key UNIQUE (config_version, config_key)
);

CREATE INDEX ix_scoring_config_active ON scoring_config (active);

-- Seed: methodology v1.0 defaults (see Scoring Methodology canonical doc).
INSERT INTO scoring_config (config_version, config_key, value_json) VALUES
  ('v1.0', 'bands', '{"allGood":75,"watchOut":50,"actNow":25}'),
  ('v1.0', 'weights', '{"equalBaseTotal":0.40,"exposureTiltTotal":0.60}'),
  ('v1.0', 'pressure.fx', '{"base":25,"depr7dCoef":6,"depr1dCoef":4,"apprEaseCoef":3}'),
  ('v1.0', 'pressure.cost', '{"base":25,"fuelCoef":8,"ungaCoef":6}'),
  ('v1.0', 'pressure.credit', '{"base":20,"neutralCbkRate":7,"rateLevelMult":5,"rateLevelCap":50,"rateRiseCoef":8}'),
  ('v1.0', 'pressure.cashflow', '{"base":20,"rateLevelFactorMult":0.6}'),
  ('v1.0', 'pressure.compliance', '{"base":15,"deadline":{"d3":35,"d7":25,"d14":15,"other":5},"provisionalFloor":{"one":45,"two":60,"threePlus":75}}'),
  ('v1.0', 'pressure.customerSupplier', '{"baseline":25}'),
  ('v1.0', 'pressure.macro', '{"fxWeight":0.40,"costWeight":0.30,"creditWeight":0.30}'),
  ('v1.0', 'exposure.fx', '{"yes_regularly":1.0,"both":0.8,"yes_sometimes":0.55,"no_local":0.1}'),
  ('v1.0', 'exposure.cashflow', '{"base":0.30,"invoice":0.40,"cheque":0.10,"emp_11_25":0.10,"emp_26_50":0.15,"emp_50_plus":0.20}'),
  ('v1.0', 'exposure.customerSupplier', '{"invoice":0.70,"default":0.30}'),
  ('v1.0', 'exposure.cost', '{"biggestCost":{"stock_inventory":0.80,"transport_fuel":0.85,"utilities":0.55,"salaries":0.45,"loan_repayments":0.30,"rent":0.40},"sectorBonus":0.10}'),
  ('v1.0', 'exposure.credit', '{"loan_repayments":0.80,"default":0.40}'),
  ('v1.0', 'exposure.compliance', '{"base":0.50,"emp_6_plus":0.10,"emp_26_plus":0.10,"importing":0.20}'),
  ('v1.0', 'exposure.macro', '{"floor":0.50}');
