-- V6: the 7 SME risk modules (reference data). sme_name_key points at the i18n dictionary
-- so display names/score-band labels stay in the language files, not the DB.

CREATE TABLE risk_modules (
    code          VARCHAR(20)  PRIMARY KEY,
    sme_name_key  VARCHAR(80)  NOT NULL,
    display_order INTEGER      NOT NULL
);

INSERT INTO risk_modules (code, sme_name_key, display_order) VALUES
  ('FX',           'module.fx',           1),
  ('LIQUIDITY',    'module.liquidity',    2),
  ('COUNTERPARTY', 'module.counterparty', 3),
  ('COMMODITY',    'module.commodity',    4),
  ('CREDIT',       'module.credit',       5),
  ('REGULATORY',   'module.regulatory',   6),
  ('MACRO',        'module.macro',        7);
