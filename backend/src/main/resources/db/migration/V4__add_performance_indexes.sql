-- =====================================================
-- V4: Add Performance Optimization Indexes
-- =====================================================
-- Purpose: Optimize common query patterns
-- =====================================================

-- =====================================================
-- GALLERY QUERY OPTIMIZATION
-- =====================================================
-- Composite index for user photo gallery queries
-- Query pattern: SELECT * FROM photos WHERE user_id = ? AND deleted_at IS NULL ORDER BY created_at DESC LIMIT 50
CREATE INDEX idx_photos_gallery_query ON photos(user_id, deleted_at, created_at DESC);

-- Partial index for user's non-deleted photos (covering index)
CREATE INDEX idx_photos_user_active ON photos(user_id, created_at DESC, filename, file_size, upload_status)
    WHERE deleted_at IS NULL;

-- =====================================================
-- UPLOAD SESSION MONITORING
-- =====================================================
-- Composite index for active upload sessions
-- Query pattern: SELECT * FROM upload_sessions WHERE user_id = ? AND status = 'IN_PROGRESS' ORDER BY created_at DESC
CREATE INDEX idx_upload_sessions_active ON upload_sessions(user_id, status, created_at DESC)
    WHERE status IN ('IN_PROGRESS', 'FAILED');

-- =====================================================
-- FAILED UPLOAD MONITORING
-- =====================================================
-- Partial index for failed photo uploads (admin monitoring)
-- Query pattern: SELECT * FROM photos WHERE upload_status = 'FAILED' ORDER BY created_at DESC
CREATE INDEX idx_photos_failed_uploads ON photos(created_at DESC)
    WHERE upload_status = 'FAILED';

-- =====================================================
-- TAG SEARCH OPTIMIZATION
-- =====================================================
-- Index for case-insensitive tag name search
-- Query pattern: SELECT * FROM tags WHERE user_id = ? AND LOWER(name) LIKE LOWER(?)
CREATE INDEX idx_tags_name_lower ON tags(user_id, LOWER(name));

-- =====================================================
-- REFRESH TOKEN CLEANUP
-- =====================================================
-- Partial index for expired token cleanup job
-- Query pattern: DELETE FROM refresh_tokens WHERE expires_at < NOW() AND revoked_at IS NULL
CREATE INDEX idx_refresh_tokens_expired ON refresh_tokens(expires_at)
    WHERE revoked_at IS NULL;

-- =====================================================
-- JSONB METADATA SEARCH
-- =====================================================
-- Additional GIN index paths for specific JSONB queries
-- Supports queries like: WHERE metadata->>'camera_model' = 'iPhone 14'
CREATE INDEX idx_photos_metadata_camera ON photos((metadata->>'camera_model'))
    WHERE metadata IS NOT NULL;

CREATE INDEX idx_photos_metadata_dimensions ON photos((metadata->>'width'), (metadata->>'height'))
    WHERE metadata IS NOT NULL;

-- =====================================================
-- STATISTICS UPDATE
-- =====================================================
-- Analyze tables to update query planner statistics
ANALYZE users;
ANALYZE user_preferences;
ANALYZE refresh_tokens;
ANALYZE upload_sessions;
ANALYZE photos;
ANALYZE tags;
ANALYZE photo_tags;

-- =====================================================
-- COMMENTS
-- =====================================================
COMMENT ON INDEX idx_photos_gallery_query IS 'Optimizes user gallery pagination queries (most common query pattern)';
COMMENT ON INDEX idx_photos_user_active IS 'Covering index for active photos (avoids table lookup for common columns)';
COMMENT ON INDEX idx_upload_sessions_active IS 'Optimizes monitoring of in-progress upload sessions';
COMMENT ON INDEX idx_photos_failed_uploads IS 'Supports admin dashboard for failed upload monitoring';
COMMENT ON INDEX idx_tags_name_lower IS 'Case-insensitive tag search optimization';
COMMENT ON INDEX idx_refresh_tokens_expired IS 'Supports automated cleanup job for expired tokens';
COMMENT ON INDEX idx_photos_metadata_camera IS 'Optimizes EXIF camera model queries';
COMMENT ON INDEX idx_photos_metadata_dimensions IS 'Optimizes photo dimension filtering';
