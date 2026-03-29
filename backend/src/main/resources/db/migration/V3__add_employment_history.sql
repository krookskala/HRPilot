-- V3__add_employment_history.sql
-- Employment history tracking for position, department, and salary changes

CREATE TABLE IF NOT EXISTS employment_history (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    change_type     VARCHAR(50)     NOT NULL,
    old_value       VARCHAR(500),
    new_value       VARCHAR(500),
    changed_at      TIMESTAMP       NOT NULL
);

CREATE INDEX idx_employment_history_employee ON employment_history (employee_id);
CREATE INDEX idx_employment_history_type ON employment_history (change_type);
