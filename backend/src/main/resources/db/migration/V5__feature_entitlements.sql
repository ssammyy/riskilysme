-- V5: feature_entitlements — admin-managed "what is free vs paid" matrix.
-- The entitlement resolver reads from here (no hardcoded packaging). `enabled = false`
-- reverts a feature to its in-code default tier. Seeded from the SoW §3 tier matrix.

CREATE TABLE feature_entitlements (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    feature_key   VARCHAR(64)  NOT NULL UNIQUE,
    display_name  VARCHAR(120) NOT NULL,
    min_tier      VARCHAR(20)  NOT NULL CHECK (min_tier IN ('BASIC', 'STANDARD')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_by    VARCHAR(255),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO feature_entitlements (feature_key, display_name, min_tier) VALUES
  ('full_module_detail', 'Full module breakdown',        'STANDARD'),
  ('unlimited_alerts',   'Unlimited alerts',             'STANDARD'),
  ('full_market_strip',  'Full market data strip',       'STANDARD'),
  ('full_deadlines',     'Full compliance calendar',     'STANDARD'),
  ('whatif_calculator',  'What-If calculator',           'STANDARD'),
  ('ai_brief',           'AI market brief',              'STANDARD'),
  ('monthly_report',     'Monthly PDF report',           'STANDARD'),
  ('customer_tracker',   'Customer & supplier tracker',  'STANDARD'),
  ('loan_monitor',       'Loan & overdraft monitor',     'STANDARD'),
  ('whatsapp_alerts',    'WhatsApp alerts',              'STANDARD'),
  ('academy',            'RISKILY Academy',              'STANDARD');
