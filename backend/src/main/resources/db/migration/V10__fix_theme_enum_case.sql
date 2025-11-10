-- V10: Fix theme enum case to match Java enum (UPPERCASE)
--
-- Changes theme check constraint from lowercase to uppercase values

ALTER TABLE user_preferences
DROP CONSTRAINT IF EXISTS user_preferences_theme_check;

ALTER TABLE user_preferences
ADD CONSTRAINT user_preferences_theme_check CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM'));

-- Update existing rows to uppercase (if any exist)
UPDATE user_preferences SET theme = UPPER(theme);
