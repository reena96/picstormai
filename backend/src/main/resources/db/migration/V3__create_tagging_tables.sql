-- =====================================================
-- V3: Create Tagging Tables
-- =====================================================
-- Tables: tags, photo_tags
-- Purpose: Photo organization with user-defined tags
-- =====================================================

-- =====================================================
-- TAGS TABLE
-- =====================================================
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(30) NOT NULL,
    color VARCHAR(7) NOT NULL DEFAULT '#3B82F6',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT tags_user_name_unique UNIQUE (user_id, name),
    CONSTRAINT tags_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT tags_color_hex_format CHECK (color ~* '^#[0-9A-F]{6}$')
);

-- Indexes for tags table
CREATE INDEX idx_tags_user_id_name ON tags(user_id, name);
CREATE INDEX idx_tags_user_id_created_at ON tags(user_id, created_at DESC);

-- =====================================================
-- PHOTO_TAGS TABLE (Many-to-Many Junction)
-- =====================================================
CREATE TABLE photo_tags (
    photo_id UUID NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Composite primary key
    PRIMARY KEY (photo_id, tag_id)
);

-- Indexes for photo_tags table
CREATE INDEX idx_photo_tags_tag_id_photo_id ON photo_tags(tag_id, photo_id);
CREATE INDEX idx_photo_tags_photo_id ON photo_tags(photo_id);
CREATE INDEX idx_photo_tags_created_at ON photo_tags(created_at DESC);

-- =====================================================
-- COMMENTS
-- =====================================================
COMMENT ON TABLE tags IS 'User-defined tags for photo organization (max 30 chars per tag)';
COMMENT ON TABLE photo_tags IS 'Many-to-many relationship between photos and tags';

COMMENT ON COLUMN tags.name IS 'Tag name (unique per user, case-sensitive)';
COMMENT ON COLUMN tags.color IS 'Hex color code for tag display (#RRGGBB format)';

COMMENT ON COLUMN photo_tags.photo_id IS 'Foreign key to photos table (CASCADE delete)';
COMMENT ON COLUMN photo_tags.tag_id IS 'Foreign key to tags table (CASCADE delete)';
