-- V6__leave_workflow_foundation.sql
-- Production-ready leave workflow foundation with policies, history, and holiday support

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS working_days INTEGER,
    ADD COLUMN IF NOT EXISTS approved_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS rejected_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS cancelled_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS actioned_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;

UPDATE leave_requests
SET working_days = GREATEST(1, (end_date - start_date) + 1)
WHERE working_days IS NULL;

ALTER TABLE leave_requests
    ALTER COLUMN working_days SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_leave_requests_approved_by ON leave_requests (approved_by_user_id);
CREATE INDEX IF NOT EXISTS idx_leave_requests_rejected_by ON leave_requests (rejected_by_user_id);
CREATE INDEX IF NOT EXISTS idx_leave_requests_cancelled_by ON leave_requests (cancelled_by_user_id);

CREATE TABLE IF NOT EXISTS leave_request_history (
    id              BIGSERIAL       PRIMARY KEY,
    leave_request_id BIGINT         NOT NULL REFERENCES leave_requests (id) ON DELETE CASCADE,
    actor_user_id   BIGINT          REFERENCES users (id),
    action_type     VARCHAR(50)     NOT NULL,
    from_status     VARCHAR(50),
    to_status       VARCHAR(50)     NOT NULL,
    note            TEXT,
    occurred_at     TIMESTAMP       NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_leave_request_history_request ON leave_request_history (leave_request_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS holiday_calendar_entries (
    id              BIGSERIAL       PRIMARY KEY,
    holiday_date    DATE            NOT NULL,
    holiday_year    INTEGER         NOT NULL,
    country_code    VARCHAR(10)     NOT NULL,
    state_code      VARCHAR(10),
    name            VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_holiday_calendar_range ON holiday_calendar_entries (holiday_date, country_code, state_code);
CREATE INDEX IF NOT EXISTS idx_holiday_calendar_year ON holiday_calendar_entries (holiday_year, country_code, state_code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_holiday_calendar_date_scope
    ON holiday_calendar_entries (holiday_date, country_code, COALESCE(state_code, 'ALL'));

CREATE TABLE IF NOT EXISTS company_settings (
    id              BIGSERIAL       PRIMARY KEY,
    company_name    VARCHAR(255)    NOT NULL,
    country_code    VARCHAR(10)     NOT NULL,
    state_code      VARCHAR(10),
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);

INSERT INTO company_settings (company_name, country_code, state_code, created_at, updated_at)
SELECT 'HRPilot Demo Company', 'DE', 'BY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM company_settings);

CREATE TABLE IF NOT EXISTS leave_policies (
    id                  BIGSERIAL       PRIMARY KEY,
    leave_type          VARCHAR(50)     NOT NULL UNIQUE,
    annual_days         INTEGER         NOT NULL,
    carryover_enabled   BOOLEAN         NOT NULL DEFAULT FALSE,
    carryover_max_days  INTEGER         NOT NULL DEFAULT 0,
    expires_month       INTEGER,
    expires_day         INTEGER,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP
);

INSERT INTO leave_policies (leave_type, annual_days, carryover_enabled, carryover_max_days, expires_month, expires_day, created_at, updated_at)
SELECT 'ANNUAL', 30, TRUE, 5, 3, 31, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM leave_policies WHERE leave_type = 'ANNUAL');

INSERT INTO leave_policies (leave_type, annual_days, carryover_enabled, carryover_max_days, expires_month, expires_day, created_at, updated_at)
SELECT 'SICK', 15, FALSE, 0, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM leave_policies WHERE leave_type = 'SICK');

INSERT INTO leave_policies (leave_type, annual_days, carryover_enabled, carryover_max_days, expires_month, expires_day, created_at, updated_at)
SELECT 'UNPAID', 10, FALSE, 0, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM leave_policies WHERE leave_type = 'UNPAID');
