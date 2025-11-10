-- V9: Add notification and retry preferences to user_preferences
--
-- Adds columns for upload notification settings and auto-retry behavior

ALTER TABLE user_preferences
ADD COLUMN IF NOT EXISTS upload_complete_notifications BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE user_preferences
ADD COLUMN IF NOT EXISTS auto_retry_failed BOOLEAN NOT NULL DEFAULT TRUE;

-- Comments for documentation
COMMENT ON COLUMN user_preferences.upload_complete_notifications IS 'Whether to show notifications when uploads complete';
COMMENT ON COLUMN user_preferences.auto_retry_failed IS 'Whether to automatically retry failed uploads';
