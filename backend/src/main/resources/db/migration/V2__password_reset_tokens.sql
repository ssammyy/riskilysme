-- V2: password reset tokens. We store only a SHA-256 hash of the token (hex, 64 chars);
-- the raw token is sent in the email link and never persisted.

CREATE TABLE password_reset_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_prt_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX ix_prt_user ON password_reset_tokens (user_id);
