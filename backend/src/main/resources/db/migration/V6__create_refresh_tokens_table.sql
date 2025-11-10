-- V6: Create refresh_tokens table for JWT refresh token storage
--
-- Refresh tokens are long-lived tokens (30 days) stored as BCrypt hash.
-- Used for token rotation pattern - each refresh invalidates old token.

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key to users table
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Index for finding tokens by user (useful for logout all devices)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Index for cleanup job (finding expired tokens)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Comments for documentation
COMMENT ON TABLE refresh_tokens IS 'Stores BCrypt hashed refresh tokens for JWT authentication';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'BCrypt hash of the JWT refresh token (never store plain tokens)';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp (30 days from creation)';
