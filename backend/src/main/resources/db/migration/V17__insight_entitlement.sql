-- V17: register AI_INSIGHTS as a Standard-only feature in the entitlement matrix.
INSERT INTO feature_entitlements (feature_key, display_name, min_tier)
VALUES ('ai_insights', 'AI-powered daily insights', 'STANDARD')
ON CONFLICT (feature_key) DO NOTHING;
