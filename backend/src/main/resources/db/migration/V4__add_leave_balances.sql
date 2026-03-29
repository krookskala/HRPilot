-- V4__add_leave_balances.sql
-- Leave balance tracking per employee, leave type, and year

CREATE TABLE IF NOT EXISTS leave_balances (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    leave_type      VARCHAR(50)     NOT NULL,
    year            INTEGER         NOT NULL,
    total_days      INTEGER         NOT NULL,
    used_days       INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP,
    UNIQUE (employee_id, leave_type, year)
);

CREATE INDEX idx_leave_balances_employee_year ON leave_balances (employee_id, year);
