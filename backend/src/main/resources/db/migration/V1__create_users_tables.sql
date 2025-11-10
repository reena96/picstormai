-- =====================================================
-- V1: Create User-Related Tables
-- =====================================================
-- Tables: users, user_preferences, refresh_tokens
-- Purpose: Core authentication and user management
-- =====================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- USERS TABLE
-- =====================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT users_email_format_check CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Indexes for users table
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- =====================================================
-- USER_PREFERENCES TABLE
-- =====================================================
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    animations BOOLEAN NOT NULL DEFAULT TRUE,
    sound BOOLEAN NOT NULL DEFAULT TRUE,
    theme VARCHAR(20) NOT NULL DEFAULT 'light',
    concurrent_uploads INTEGER NOT NULL DEFAULT 3,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT user_preferences_user_id_unique UNIQUE (user_id),
    CONSTRAINT user_preferences_theme_check CHECK (theme IN ('light', 'dark', 'system')),
    CONSTRAINT user_preferences_concurrent_uploads_check CHECK (concurrent_uploads BETWEEN 1 AND 10)
);

-- Index for user_preferences table
CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);

-- =====================================================
-- REFRESH_TOKENS TABLE
-- =====================================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT refresh_tokens_expires_future_check CHECK (expires_at > created_at)
);

-- Indexes for refresh_tokens table
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at) WHERE revoked_at IS NULL;

-- =====================================================
-- COMMENTS
-- =====================================================
COMMENT ON TABLE users IS 'Core user authentication and profile information';
COMMENT ON TABLE user_preferences IS 'User-specific application preferences and settings';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for session management';

COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password (never store plaintext)';
COMMENT ON COLUMN users.email_verified IS 'Email verification status for account activation';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of refresh token (never store plaintext tokens)';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Timestamp when token was revoked (NULL if still valid)';
