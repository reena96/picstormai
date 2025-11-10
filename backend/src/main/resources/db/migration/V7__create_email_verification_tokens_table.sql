-- V7: Create email_verification_tokens table for email verification
--
-- Email verification tokens are 32-character hex strings that expire in 24 hours.
-- One-time use tokens deleted after successful verification.

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    token VARCHAR(32) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key to users table
    CONSTRAINT fk_email_verification_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Index for finding tokens by token string (primary lookup)
CREATE INDEX IF NOT EXISTS idx_email_verification_token ON email_verification_tokens(token);

-- Index for finding tokens by user (one token per user)
CREATE INDEX IF NOT EXISTS idx_email_verification_user_id ON email_verification_tokens(user_id);

-- Index for cleanup job (finding expired tokens)
CREATE INDEX IF NOT EXISTS idx_email_verification_expires_at ON email_verification_tokens(expires_at);

-- Comments for documentation
COMMENT ON TABLE email_verification_tokens IS 'Stores email verification tokens for new user registration';
COMMENT ON COLUMN email_verification_tokens.token IS '32-character secure random hex string';
COMMENT ON COLUMN email_verification_tokens.expires_at IS 'Token expiration timestamp (24 hours from creation)';
