-- V1 baseline: users table (authentication + role + SME profile).
-- Fully separate stack: this is Riskily SME's own schema (no skin_mode, no UTM columns).
-- Enum-like columns are VARCHAR + CHECK so they can evolve via append-only migrations.

CREATE TABLE users (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Authentication
    email                   VARCHAR(255) NOT NULL,
    password_hash           VARCHAR(255) NOT NULL,
    email_verified          BOOLEAN      NOT NULL DEFAULT FALSE,
    role                    VARCHAR(20)  NOT NULL DEFAULT 'USER'
                              CHECK (role IN ('USER', 'ADMIN')),
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                              CHECK (status IN ('ACTIVE', 'SUSPENDED')),

    -- Profile (collected during onboarding; nullable until then)
    first_name              VARCHAR(120),
    business_name           VARCHAR(200),
    business_type           VARCHAR(40),
    employee_range          VARCHAR(20),
    import_behaviour        VARCHAR(30),
    payment_methods         JSONB        NOT NULL DEFAULT '[]'::jsonb,
    biggest_cost            VARCHAR(40),

    -- Preferences & subscription
    language                VARCHAR(2)   NOT NULL DEFAULT 'EN'
                              CHECK (language IN ('EN', 'SW')),
    subscription_tier       VARCHAR(20)  NOT NULL DEFAULT 'BASIC'
                              CHECK (subscription_tier IN ('BASIC', 'STANDARD')),
    monthly_alert_count     INTEGER      NOT NULL DEFAULT 0,

    onboarding_completed_at TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_users_email ON users (LOWER(email));
