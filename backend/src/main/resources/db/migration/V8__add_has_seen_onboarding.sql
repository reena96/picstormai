-- V8: Add has_seen_onboarding column to users table
--
-- Purpose: Track whether user has completed the onboarding tutorial
-- Default: false (new users haven't seen onboarding)
-- Used to show tutorial on first login only

ALTER TABLE users
ADD COLUMN has_seen_onboarding BOOLEAN NOT NULL DEFAULT FALSE;

-- Comment for documentation
COMMENT ON COLUMN users.has_seen_onboarding IS 'Tracks whether user has completed onboarding tutorial. False for new users, set to true after tutorial completion or skip.';
