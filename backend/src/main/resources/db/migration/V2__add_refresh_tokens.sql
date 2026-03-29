-- V2__add_refresh_tokens.sql
-- Refresh token table for JWT token renewal

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGSERIAL       PRIMARY KEY,
    token           VARCHAR(255)    NOT NULL UNIQUE,
    user_id         BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expiry_date     TIMESTAMP       NOT NULL
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
