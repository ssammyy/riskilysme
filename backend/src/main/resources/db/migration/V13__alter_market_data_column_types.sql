-- V13: alter market_data columns from NUMERIC to DOUBLE PRECISION to match Kotlin entity Double properties and pass Hibernate schema validation.

ALTER TABLE market_data ALTER COLUMN usdkes_spot TYPE DOUBLE PRECISION;
ALTER TABLE market_data ALTER COLUMN usdkes_prev TYPE DOUBLE PRECISION;
ALTER TABLE market_data ALTER COLUMN usdkes_7d_ago TYPE DOUBLE PRECISION;
ALTER TABLE market_data ALTER COLUMN fuel_price TYPE DOUBLE PRECISION;
ALTER TABLE market_data ALTER COLUMN fuel_prev TYPE DOUBLE PRECISION;
ALTER TABLE market_data ALTER COLUMN unga_price TYPE DOUBLE PRECISION;
ALTER TABLE market_data ALTER COLUMN unga_prev TYPE DOUBLE PRECISION;
ALTER TABLE market_data ALTER COLUMN cbk_rate TYPE DOUBLE PRECISION;
ALTER TABLE market_data ALTER COLUMN cbk_prev TYPE DOUBLE PRECISION;
