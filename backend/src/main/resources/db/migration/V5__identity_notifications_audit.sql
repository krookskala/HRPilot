-- V5__identity_notifications_audit.sql
-- Identity lifecycle, notifications, audit logs, and secure token support

ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS activated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS invitation_tokens (
    id              BIGSERIAL       PRIMARY KEY,
    token           VARCHAR(255)    NOT NULL UNIQUE,
    user_id         BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_by      BIGINT          REFERENCES users (id),
    expires_at      TIMESTAMP       NOT NULL,
    consumed_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invitation_tokens_token ON invitation_tokens (token);
CREATE INDEX IF NOT EXISTS idx_invitation_tokens_user ON invitation_tokens (user_id);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id              BIGSERIAL       PRIMARY KEY,
    token           VARCHAR(255)    NOT NULL UNIQUE,
    user_id         BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at      TIMESTAMP       NOT NULL,
    consumed_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token ON password_reset_tokens (token);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user ON password_reset_tokens (user_id);

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type            VARCHAR(50)     NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    message         TEXT            NOT NULL,
    action_url      VARCHAR(500),
    read_at         TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at ON notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read_at ON notifications (user_id, read_at);

CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL       PRIMARY KEY,
    actor_user_id   BIGINT          REFERENCES users (id),
    action_type     VARCHAR(100)    NOT NULL,
    target_type     VARCHAR(100)    NOT NULL,
    target_id       VARCHAR(100),
    summary         VARCHAR(500)    NOT NULL,
    details         TEXT,
    ip_address      VARCHAR(100),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_user ON audit_logs (actor_user_id);
