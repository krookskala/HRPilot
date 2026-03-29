-- V1__init_schema.sql
-- HRPilot initial database schema
-- Creates all tables matching current JPA entity definitions

-- =============================================
-- 1. USERS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(50)     NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    preferred_lang  VARCHAR(10)     NOT NULL DEFAULT 'en',
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

-- =============================================
-- 2. DEPARTMENTS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS departments (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL UNIQUE,
    manager_id      BIGINT          REFERENCES users (id),
    parent_dept_id  BIGINT          REFERENCES departments (id),
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_departments_manager ON departments (manager_id);
CREATE INDEX idx_departments_parent ON departments (parent_dept_id);

-- =============================================
-- 3. EMPLOYEES TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS employees (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL UNIQUE REFERENCES users (id),
    first_name      VARCHAR(255)    NOT NULL,
    last_name       VARCHAR(255)    NOT NULL,
    position        VARCHAR(255)    NOT NULL,
    salary          DECIMAL(19, 2)  NOT NULL,
    hire_date       DATE            NOT NULL,
    department_id   BIGINT          REFERENCES departments (id),
    photo_url       VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_employees_user ON employees (user_id);
CREATE INDEX idx_employees_department ON employees (department_id);
CREATE INDEX idx_employees_name ON employees (last_name, first_name);

-- =============================================
-- 4. LEAVE REQUESTS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS leave_requests (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees (id),
    type            VARCHAR(50)     NOT NULL,
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    status          VARCHAR(50)     DEFAULT 'PENDING',
    reason          TEXT,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_leave_requests_employee ON leave_requests (employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests (status);
CREATE INDEX idx_leave_requests_dates ON leave_requests (start_date, end_date);

-- =============================================
-- 5. PAYROLL RECORDS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS payroll_records (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees (id),
    year            INTEGER         NOT NULL,
    month           INTEGER         NOT NULL,
    base_salary     DECIMAL(19, 2)  NOT NULL,
    bonus           DECIMAL(19, 2)  NOT NULL,
    deductions      DECIMAL(19, 2)  NOT NULL,
    net_salary      DECIMAL(19, 2)  NOT NULL,
    status          VARCHAR(50)     DEFAULT 'DRAFT',
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_payroll_employee ON payroll_records (employee_id);
CREATE INDEX idx_payroll_period ON payroll_records (year, month);
CREATE INDEX idx_payroll_status ON payroll_records (status);
