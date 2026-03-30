-- V8__payroll_runs_and_payslips.sql
-- Structured payroll runs, components, configurable Germany-oriented rules, and payslip metadata

ALTER TABLE payroll_records
    ADD COLUMN IF NOT EXISTS gross_salary DECIMAL(19, 2),
    ADD COLUMN IF NOT EXISTS employee_social_contributions DECIMAL(19, 2),
    ADD COLUMN IF NOT EXISTS employer_social_contributions DECIMAL(19, 2),
    ADD COLUMN IF NOT EXISTS income_tax DECIMAL(19, 2),
    ADD COLUMN IF NOT EXISTS tax_class VARCHAR(10),
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS payslip_storage_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS payslip_filename VARCHAR(255);

UPDATE payroll_records
SET gross_salary = COALESCE(gross_salary, base_salary + bonus),
    employee_social_contributions = COALESCE(employee_social_contributions, deductions),
    employer_social_contributions = COALESCE(employer_social_contributions, 0),
    income_tax = COALESCE(income_tax, 0),
    tax_class = COALESCE(tax_class, 'I')
WHERE gross_salary IS NULL
   OR employee_social_contributions IS NULL
   OR employer_social_contributions IS NULL
   OR income_tax IS NULL
   OR tax_class IS NULL;

ALTER TABLE payroll_records
    ALTER COLUMN gross_salary SET NOT NULL,
    ALTER COLUMN employee_social_contributions SET NOT NULL,
    ALTER COLUMN employer_social_contributions SET NOT NULL,
    ALTER COLUMN income_tax SET NOT NULL,
    ALTER COLUMN tax_class SET NOT NULL;

CREATE TABLE IF NOT EXISTS payroll_runs (
    id                  BIGSERIAL       PRIMARY KEY,
    name                VARCHAR(255)    NOT NULL,
    year                INTEGER         NOT NULL,
    month               INTEGER         NOT NULL,
    status              VARCHAR(50)     NOT NULL,
    generated_by_user_id BIGINT         REFERENCES users (id),
    published_at        TIMESTAMP,
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP,
    UNIQUE (year, month, name)
);

ALTER TABLE payroll_records
    ADD COLUMN IF NOT EXISTS run_id BIGINT REFERENCES payroll_runs (id);

CREATE INDEX IF NOT EXISTS idx_payroll_records_run ON payroll_records (run_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_payroll_records_employee_period
    ON payroll_records (employee_id, year, month);

CREATE TABLE IF NOT EXISTS payroll_components (
    id                  BIGSERIAL       PRIMARY KEY,
    payroll_record_id   BIGINT          NOT NULL REFERENCES payroll_records (id) ON DELETE CASCADE,
    component_type      VARCHAR(50)     NOT NULL,
    code                VARCHAR(100)    NOT NULL,
    label               VARCHAR(255)    NOT NULL,
    amount              DECIMAL(19, 2)  NOT NULL,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payroll_components_record ON payroll_components (payroll_record_id);

CREATE TABLE IF NOT EXISTS payroll_rule_versions (
    id                              BIGSERIAL       PRIMARY KEY,
    version_label                   VARCHAR(100)    NOT NULL UNIQUE,
    employee_pension_rate           DECIMAL(10, 4)  NOT NULL,
    employee_health_rate            DECIMAL(10, 4)  NOT NULL,
    employee_unemployment_rate      DECIMAL(10, 4)  NOT NULL,
    employee_care_rate              DECIMAL(10, 4)  NOT NULL,
    employer_pension_rate           DECIMAL(10, 4)  NOT NULL,
    employer_health_rate            DECIMAL(10, 4)  NOT NULL,
    employer_unemployment_rate      DECIMAL(10, 4)  NOT NULL,
    employer_care_rate              DECIMAL(10, 4)  NOT NULL,
    income_tax_base_rate            DECIMAL(10, 4)  NOT NULL,
    solidarity_rate                 DECIMAL(10, 4)  NOT NULL,
    monthly_tax_free_allowance      DECIMAL(19, 2)  NOT NULL,
    is_active                       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at                      TIMESTAMP       NOT NULL,
    updated_at                      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payroll_tax_class_rules (
    id                          BIGSERIAL       PRIMARY KEY,
    rule_version_id             BIGINT          NOT NULL REFERENCES payroll_rule_versions (id) ON DELETE CASCADE,
    tax_class                   VARCHAR(10)     NOT NULL,
    monthly_allowance           DECIMAL(19, 2)  NOT NULL,
    tax_multiplier              DECIMAL(10, 4)  NOT NULL,
    created_at                  TIMESTAMP       NOT NULL,
    updated_at                  TIMESTAMP,
    UNIQUE (rule_version_id, tax_class)
);

INSERT INTO payroll_rule_versions (
    version_label,
    employee_pension_rate,
    employee_health_rate,
    employee_unemployment_rate,
    employee_care_rate,
    employer_pension_rate,
    employer_health_rate,
    employer_unemployment_rate,
    employer_care_rate,
    income_tax_base_rate,
    solidarity_rate,
    monthly_tax_free_allowance,
    is_active,
    created_at,
    updated_at
)
SELECT
    'DE-2026-DEFAULT',
    0.0930,
    0.0810,
    0.0130,
    0.0170,
    0.0930,
    0.0810,
    0.0130,
    0.0170,
    0.1400,
    0.0550,
    1000.00,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM payroll_rule_versions WHERE version_label = 'DE-2026-DEFAULT');

INSERT INTO payroll_tax_class_rules (rule_version_id, tax_class, monthly_allowance, tax_multiplier, created_at, updated_at)
SELECT rule.id, v.tax_class, v.monthly_allowance, v.tax_multiplier, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM payroll_rule_versions rule
CROSS JOIN (
    VALUES
        ('I', 1000.00, 1.00),
        ('II', 1200.00, 0.95),
        ('III', 1800.00, 0.75),
        ('IV', 1000.00, 1.00),
        ('V', 500.00, 1.20),
        ('VI', 0.00, 1.35)
) AS v(tax_class, monthly_allowance, tax_multiplier)
WHERE rule.version_label = 'DE-2026-DEFAULT'
  AND NOT EXISTS (
      SELECT 1
      FROM payroll_tax_class_rules existing
      WHERE existing.rule_version_id = rule.id
        AND existing.tax_class = v.tax_class
  );
