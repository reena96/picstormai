-- =====================================================
-- V5: Add Automatic updated_at Triggers
-- =====================================================
-- Purpose: Automatically update updated_at timestamps on row updates
-- Tables: users, user_preferences, photos
-- =====================================================

-- =====================================================
-- CREATE REUSABLE TRIGGER FUNCTION
-- =====================================================
-- This function automatically sets updated_at to the current timestamp
-- whenever a row is updated. It's designed to be reused across multiple tables.
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_updated_at_column() IS 'Automatically updates updated_at timestamp on row modification';

-- =====================================================
-- CREATE TRIGGERS FOR EACH TABLE
-- =====================================================

-- Trigger for users table
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TRIGGER update_users_updated_at ON users IS 'Automatically updates updated_at when user record is modified';

-- Trigger for user_preferences table
CREATE TRIGGER update_user_preferences_updated_at
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TRIGGER update_user_preferences_updated_at ON user_preferences IS 'Automatically updates updated_at when user preferences are modified';

-- Trigger for photos table
CREATE TRIGGER update_photos_updated_at
    BEFORE UPDATE ON photos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TRIGGER update_photos_updated_at ON photos IS 'Automatically updates updated_at when photo record is modified';

-- =====================================================
-- VERIFICATION NOTES
-- =====================================================
-- To verify triggers are working:
-- 1. Update a record: UPDATE users SET display_name = 'New Name' WHERE id = '<user_id>';
-- 2. Check updated_at: SELECT id, display_name, updated_at FROM users WHERE id = '<user_id>';
-- 3. Confirm updated_at reflects the current timestamp
--
-- To list all triggers:
-- SELECT trigger_name, event_manipulation, event_object_table, action_statement
-- FROM information_schema.triggers
-- WHERE trigger_schema = 'public' AND trigger_name LIKE 'update_%_updated_at';
-- =====================================================
