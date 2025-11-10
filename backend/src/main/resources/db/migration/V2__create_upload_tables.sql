-- =====================================================
-- V2: Create Upload Domain Tables
-- =====================================================
-- Tables: upload_sessions, photos
-- Purpose: Photo upload session tracking and photo metadata
-- =====================================================

-- =====================================================
-- UPLOAD_SESSIONS TABLE
-- =====================================================
CREATE TABLE upload_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    total_photos INTEGER NOT NULL DEFAULT 0,
    completed_photos INTEGER NOT NULL DEFAULT 0,
    failed_photos INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT upload_sessions_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT upload_sessions_total_photos_positive CHECK (total_photos >= 0),
    CONSTRAINT upload_sessions_completed_photos_valid CHECK (completed_photos >= 0 AND completed_photos <= total_photos),
    CONSTRAINT upload_sessions_failed_photos_valid CHECK (failed_photos >= 0 AND failed_photos <= total_photos),
    CONSTRAINT upload_sessions_completion_logic CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL) OR
        (status != 'COMPLETED' AND completed_at IS NULL)
    )
);

-- Indexes for upload_sessions table
CREATE INDEX idx_upload_sessions_user_id_created_at ON upload_sessions(user_id, created_at DESC);
CREATE INDEX idx_upload_sessions_user_id_status ON upload_sessions(user_id, status);
CREATE INDEX idx_upload_sessions_status_created_at ON upload_sessions(status, created_at DESC);

-- =====================================================
-- PHOTOS TABLE
-- =====================================================
CREATE TABLE photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id UUID REFERENCES upload_sessions(id) ON DELETE SET NULL,
    filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    s3_key VARCHAR(500) NOT NULL UNIQUE,
    s3_version_id VARCHAR(100),
    storage_url TEXT,
    upload_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INTEGER NOT NULL DEFAULT 0,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT photos_upload_status_check CHECK (upload_status IN ('PENDING', 'UPLOADING', 'COMPLETED', 'FAILED')),
    CONSTRAINT photos_progress_range CHECK (progress BETWEEN 0 AND 100),
    CONSTRAINT photos_file_size_positive CHECK (file_size IS NULL OR file_size > 0),
    CONSTRAINT photos_completion_logic CHECK (
        (upload_status = 'COMPLETED' AND progress = 100) OR
        (upload_status != 'COMPLETED')
    )
);

-- Indexes for photos table
CREATE INDEX idx_photos_user_id_created_at ON photos(user_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_photos_user_id_deleted_at_created_at ON photos(user_id, deleted_at, created_at DESC);
CREATE INDEX idx_photos_session_id_upload_status ON photos(session_id, upload_status) WHERE session_id IS NOT NULL;
CREATE INDEX idx_photos_s3_key ON photos(s3_key);
CREATE INDEX idx_photos_upload_status ON photos(upload_status) WHERE upload_status IN ('PENDING', 'FAILED');

-- GIN index for JSONB metadata queries
CREATE INDEX idx_photos_metadata ON photos USING GIN (metadata);

-- =====================================================
-- COMMENTS
-- =====================================================
COMMENT ON TABLE upload_sessions IS 'Tracks batch photo upload sessions for progress monitoring';
COMMENT ON TABLE photos IS 'Photo metadata and upload tracking (soft delete with deleted_at)';

COMMENT ON COLUMN upload_sessions.status IS 'Current session status: IN_PROGRESS, COMPLETED, FAILED, CANCELLED';
COMMENT ON COLUMN upload_sessions.total_photos IS 'Total number of photos in this upload session';
COMMENT ON COLUMN upload_sessions.completed_photos IS 'Number of photos successfully uploaded';
COMMENT ON COLUMN upload_sessions.failed_photos IS 'Number of photos that failed to upload';

COMMENT ON COLUMN photos.s3_key IS 'Unique S3 object key (e.g., users/{user_id}/photos/{photo_id}.jpg)';
COMMENT ON COLUMN photos.s3_version_id IS 'S3 version ID if versioning is enabled';
COMMENT ON COLUMN photos.upload_status IS 'Upload status: PENDING, UPLOADING, COMPLETED, FAILED';
COMMENT ON COLUMN photos.progress IS 'Upload progress percentage (0-100)';
COMMENT ON COLUMN photos.metadata IS 'JSONB field for EXIF data, dimensions, camera info, etc.';
COMMENT ON COLUMN photos.deleted_at IS 'Soft delete timestamp (NULL if not deleted, allows undelete feature)';
