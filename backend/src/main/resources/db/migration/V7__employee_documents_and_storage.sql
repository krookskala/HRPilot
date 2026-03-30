-- V7__employee_documents_and_storage.sql
-- Employee document metadata and storage provider foundation

CREATE TABLE IF NOT EXISTS employee_documents (
    id                  BIGSERIAL       PRIMARY KEY,
    employee_id         BIGINT          NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    uploaded_by_user_id BIGINT          REFERENCES users (id),
    document_type       VARCHAR(50)     NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    description         TEXT,
    original_filename   VARCHAR(255)    NOT NULL,
    content_type        VARCHAR(150)    NOT NULL,
    storage_key         VARCHAR(500)    NOT NULL UNIQUE,
    file_size           BIGINT          NOT NULL,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_employee_documents_employee ON employee_documents (employee_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_employee_documents_type ON employee_documents (document_type);
