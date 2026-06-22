-- V13: correct market data seed values to accurate June 2026 market prices.
-- V12 used placeholder approximations; this migration patches them to real figures.

-- Patch the market_data row seeded for today
UPDATE market_data
SET
    usdkes_spot   = 129.3,
    usdkes_prev   = 129.8,
    usdkes_7d_ago = 130.2,
    fuel_price    = 214.03,
    fuel_prev     = 212.50,
    unga_price    = 172.50,   -- midpoint of KES 165–180 range
    unga_prev     = 170.00,
    cbk_rate      = 8.75,
    cbk_prev      = 9.00,
    refreshed_at  = now()
WHERE snapshot_date = CURRENT_DATE;

-- Insert accurate CBK reference rates as new effective-dated rows
INSERT INTO cbk_rates (rate_type, rate_value, set_by) VALUES
    ('CBR',      8.75,   'system'),   -- CBK Monetary Policy Rate, June 2026
    ('USD_KES',  129.30, 'system'),   -- USD/KES spot rate, June 2026
    ('FUEL_KES', 214.03, 'system'),   -- Petrol pump price (KES/litre), Nairobi, June 2026
    ('UNGA_KES', 172.50, 'system')    -- Unga 1 kg mid-market price (KES), June 2026
ON CONFLICT (rate_type, effective_date) DO UPDATE SET rate_value = EXCLUDED.rate_value;
