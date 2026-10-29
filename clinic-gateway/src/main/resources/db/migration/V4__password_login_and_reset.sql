-- Email/password sign-in: a bcrypt hash per account (NULL for Google-only accounts),
-- plus simple lockout counters to slow down password guessing.
ALTER TABLE clinic_users ADD COLUMN password_hash VARCHAR(100);
ALTER TABLE clinic_users ADD COLUMN password_updated_at TIMESTAMP;
ALTER TABLE clinic_users ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE clinic_users ADD COLUMN locked_until TIMESTAMP;

-- One-time password reset links. Only a SHA-256 hash of the emailed token is stored.
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES clinic_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    requested_ip VARCHAR(64)
);
CREATE UNIQUE INDEX uk_password_reset_token_hash ON password_reset_tokens(token_hash);
CREATE INDEX idx_password_reset_user_created ON password_reset_tokens(user_id, created_at DESC);
