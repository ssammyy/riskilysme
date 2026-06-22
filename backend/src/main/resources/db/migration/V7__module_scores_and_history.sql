-- V7: per-user module scores (latest snapshot) and the append-only overall-score history.

CREATE TABLE module_scores (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    module_code     VARCHAR(20)  NOT NULL REFERENCES risk_modules (code),
    exposure        NUMERIC(5, 4) NOT NULL,
    pressure        NUMERIC(6, 2) NOT NULL,
    health          INTEGER      NOT NULL,
    band            VARCHAR(20)  NOT NULL
                      CHECK (band IN ('ALL_GOOD', 'WATCH_OUT', 'ACT_NOW', 'URGENT')),
    data_confidence VARCHAR(20)  NOT NULL DEFAULT 'PROFILE'
                      CHECK (data_confidence IN ('PROFILE', 'LIVE')),
    is_provisional  BOOLEAN      NOT NULL DEFAULT FALSE,
    calculated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_module_scores_user_module UNIQUE (user_id, module_code)
);

CREATE INDEX ix_module_scores_user ON module_scores (user_id);

CREATE TABLE score_history (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    overall_health INTEGER     NOT NULL,
    band           VARCHAR(20) NOT NULL
                     CHECK (band IN ('ALL_GOOD', 'WATCH_OUT', 'ACT_NOW', 'URGENT')),
    calculated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_score_history_user_time ON score_history (user_id, calculated_at DESC);
