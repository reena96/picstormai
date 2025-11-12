-- =====================================================
-- V11: Make s3_key nullable for PENDING photos
-- =====================================================
-- The s3_key is generated after photo initiation, so it should be nullable
-- for PENDING photos, but required for COMPLETED photos.
-- =====================================================

-- Remove NOT NULL constraint from s3_key
ALTER TABLE photos ALTER COLUMN s3_key DROP NOT NULL;

-- Add a check constraint to ensure s3_key is present when status is COMPLETED
ALTER TABLE photos ADD CONSTRAINT photos_s3_key_required_when_completed
    CHECK (
        (upload_status = 'COMPLETED' AND s3_key IS NOT NULL) OR
        (upload_status != 'COMPLETED')
    );

COMMENT ON CONSTRAINT photos_s3_key_required_when_completed ON photos IS
    'Ensures s3_key is set when photo upload is completed, but allows null for PENDING/UPLOADING/FAILED states';
