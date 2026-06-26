-- V18: user_deadline_statuses — per-user action state on regulatory deadlines.
-- Default REMIND_ME: users who never explicitly set a status receive daily reminders
-- when daysRemaining <= 15 (enforced in DeadlineReminderJob).

CREATE TABLE user_deadline_statuses (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id               BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    regulatory_notice_id  BIGINT       NOT NULL REFERENCES regulatory_notices (id) ON DELETE CASCADE,
    status                VARCHAR(20)  NOT NULL DEFAULT 'REMIND_ME'
                            CHECK (status IN ('DONE', 'IN_PROGRESS', 'REMIND_ME')),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, regulatory_notice_id)
);

CREATE INDEX ix_deadline_statuses_user ON user_deadline_statuses (user_id);
