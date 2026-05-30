-- Creates table to track per-device login sessions (Telegram-style active sessions)
CREATE TABLE IF NOT EXISTS user_device_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES _user(user_id) ON DELETE CASCADE,
    device_id VARCHAR(64) NOT NULL,              -- short public identifier to show in UI
    jti VARCHAR(64) NOT NULL,                    -- JWT ID for current access token
    refresh_token_hash VARCHAR(128),             -- optional hashed refresh token (future)
    user_agent TEXT,                             -- raw user agent string
    device_name VARCHAR(255),                    -- parsed friendly name (e.g. Chrome on Windows)
    platform VARCHAR(100),                       -- OS / platform
    browser VARCHAR(100),                        -- browser name
    ip_address VARCHAR(64),                      -- last seen IP
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    last_active_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    revoked_at TIMESTAMP WITHOUT TIME ZONE,
    password_version_at_issue INTEGER,           -- for correlation when passwordVersion changes
    CONSTRAINT uq_user_device_session_device UNIQUE (user_id, device_id),
    CONSTRAINT uq_user_device_session_jti UNIQUE (jti)
);

CREATE INDEX IF NOT EXISTS idx_user_device_session_user_active ON user_device_session (user_id) WHERE revoked_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_user_device_session_last_active ON user_device_session (user_id, last_active_at DESC);
