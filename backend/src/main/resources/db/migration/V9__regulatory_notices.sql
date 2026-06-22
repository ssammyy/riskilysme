-- V9: regulatory_notices — Kenya SME compliance deadlines shown in the dashboard grid.
-- recurring_day_of_month: day within the month the obligation falls (e.g. 20 = 20th).
-- recurring_month: NULL = monthly obligation; 1-12 = annual obligation in that specific month.
-- The API computes daysRemaining server-side from the current date.

CREATE TABLE regulatory_notices (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title                  VARCHAR(255) NOT NULL,
    title_sw               VARCHAR(255) NOT NULL,
    description            TEXT,
    description_sw         TEXT,
    authority              VARCHAR(20)  NOT NULL
                             CHECK (authority IN ('KRA', 'CBK', 'NSSF', 'NHIF', 'KEBS', 'OTHER')),
    recurring_day_of_month INTEGER      NOT NULL,
    recurring_month        INTEGER,     -- NULL = every month; 1-12 = specific month (annual)
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Seed: standard Kenyan SME regulatory obligations (dates per KRA/NSSF/NHIF calendars)
INSERT INTO regulatory_notices
    (title, title_sw, description, description_sw, authority, recurring_day_of_month, recurring_month)
VALUES
    (
        'VAT Return & Payment',
        'Kuwasilisha na Kulipa VAT',
        'Monthly VAT return filing and payment due via KRA iTax.',
        'Fomu ya VAT ya kila mwezi na malipo kupitia KRA iTax.',
        'KRA', 20, NULL
    ),
    (
        'PAYE Remittance',
        'Kulipa PAYE',
        'Pay As You Earn payroll deductions must be remitted to KRA.',
        'Makato ya mishahara ya PAYE lazima yalipwe kwa KRA.',
        'KRA', 9, NULL
    ),
    (
        'NSSF Contributions',
        'Michango ya NSSF',
        'Employer and employee National Social Security Fund contributions.',
        'Michango ya NSSF kwa mwajiri na mwajiriwa.',
        'NSSF', 15, NULL
    ),
    (
        'NHIF Remittance',
        'Kulipa NHIF',
        'National Hospital Insurance Fund monthly contributions for employees.',
        'Michango ya NHIF ya kila mwezi kwa wafanyakazi.',
        'NHIF', 9, NULL
    ),
    (
        'Withholding Tax Remittance',
        'Kulipa Kodi ya Zuio',
        'Withholding tax on qualifying payments remitted to KRA by the 20th.',
        'Kodi ya zuio kwa malipo yanayostahili hupelekwa KRA ifikapo siku ya 20.',
        'KRA', 20, NULL
    ),
    (
        'Corporation Tax 1st Instalment',
        'Awali ya Kwanza ya Kodi ya Kampuni',
        'First instalment of estimated corporation tax due in April.',
        'Awali ya kwanza ya kodi ya kampuni inayokadiriwa hulipwa Aprili.',
        'KRA', 20, 4
    );
