-- V11: AMD-006 v3.0 onboarding profile columns.
-- Replaces the multi-choice profile fields as the primary scoring inputs.
-- Old columns (business_type, employee_range, import_behaviour, payment_methods, biggest_cost)
-- are RETAINED for existing data; they are no longer read by the ExposureCalculator.
-- All new columns are nullable: NULL = question not yet answered (baseline weights apply).

ALTER TABLE users
    ADD COLUMN q1_fx_yes              BOOLEAN,   -- Foreign currency exposure
    ADD COLUMN q2_loans_yes           BOOLEAN,   -- Active loans / overdrafts
    ADD COLUMN q2b_interest_rate      VARCHAR(20)
                 CHECK (q2b_interest_rate IN ('below_15', '15_to_20', 'above_20', 'not_sure')),
    ADD COLUMN q3_credit_sales_yes    BOOLEAN,   -- Sells on credit / has debtors
    ADD COLUMN q4_fixed_costs_yes     BOOLEAN,   -- Fuel / electricity / rent / water are major costs
    ADD COLUMN q5_concentration_yes   BOOLEAN,   -- One entity = 40%+ of business survival
    ADD COLUMN q6_cash_timing_yes     BOOLEAN,   -- Cash timing gap (owed money, can't pay bills now)
    ADD COLUMN q7_supplier_dep_yes    BOOLEAN,   -- Relies on 1–2 key suppliers
    ADD COLUMN q8_informal_credit_yes BOOLEAN;   -- Uses mobile loans / chamas / informal credit
