-- V4: audit_log — records privileged admin actions (CBK rate amendments, entitlement edits,
-- tier changes, suspends, etc.). affected_modules is JSON; old/new values are free text.

CREATE TABLE audit_log (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    action           VARCHAR(80)  NOT NULL,
    entity           VARCHAR(80),
    old_value        TEXT,
    new_value        TEXT,
    delta_pct        NUMERIC(10, 4),
    performed_by     VARCHAR(255) NOT NULL,
    affected_modules JSONB        NOT NULL DEFAULT '[]'::jsonb,
    timestamp        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_log_timestamp ON audit_log (timestamp DESC);
