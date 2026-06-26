-- Adds health-cap values for the provisional regulatory floor (methodology §6.4).
-- These cap the REGULATORY module health when active circulars are present,
-- replacing the pressure-floor approach which was ineffective at ~5% exposure.
-- 1 circular → health ≤ 55 (watch_out), 2 → ≤ 40 (act_now), 3+ → ≤ 25 (urgent).
UPDATE scoring_config
SET value_json = jsonb_set(
    value_json::jsonb,
    '{provisionalHealthCap}',
    '{"one": 55, "two": 40, "threePlus": 25}'::jsonb
)::text
WHERE config_key = 'pressure.compliance';
