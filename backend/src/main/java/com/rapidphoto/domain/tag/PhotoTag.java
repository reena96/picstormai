package com.rapidphoto.domain.tag;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * PhotoTag entity - junction table for many-to-many relationship.
 * Links photos to tags.
 *
 * Note: Spring Data R2DBC 3.2.x doesn't fully support composite primary keys with @Id annotations.
 * Since this entity is only accessed via custom query methods (findByPhotoId, deleteByPhotoIdAndTagId, etc.)
 * and never via standard CRUD operations, we can safely omit @Id annotations.
 * The database constraint ensures uniqueness at the DB level.
 */
@Table("photo_tags")
public class PhotoTag {

    @Column("photo_id")
    private UUID photoId;

    @Column("tag_id")
    private UUID tagId;

    @Column("created_at")
    private Instant createdAt;

    // Package-private constructor for persistence
    PhotoTag() {
    }

    private PhotoTag(UUID photoId, UUID tagId) {
        this.photoId = photoId;
        this.tagId = tagId;
        this.createdAt = Instant.now();
    }

    /**
     * Factory method to create photo-tag relationship.
     */
    public static PhotoTag create(UUID photoId, UUID tagId) {
        if (photoId == null) {
            throw new IllegalArgumentException("Photo ID cannot be null");
        }
        if (tagId == null) {
            throw new IllegalArgumentException("Tag ID cannot be null");
        }
        return new PhotoTag(photoId, tagId);
    }

    // Getters

    public UUID getPhotoId() {
        return photoId;
    }

    public UUID getTagId() {
        return tagId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Composite key for PhotoTag.
     */
    public static class PhotoTagId implements Serializable {
        private UUID photoId;
        private UUID tagId;

        public PhotoTagId() {
        }

        public PhotoTagId(UUID photoId, UUID tagId) {
            this.photoId = photoId;
            this.tagId = tagId;
        }

        public UUID getPhotoId() {
            return photoId;
        }

        public void setPhotoId(UUID photoId) {
            this.photoId = photoId;
        }

        public UUID getTagId() {
            return tagId;
        }

        public void setTagId(UUID tagId) {
            this.tagId = tagId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PhotoTagId that = (PhotoTagId) o;
            return Objects.equals(photoId, that.photoId) &&
                   Objects.equals(tagId, that.tagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(photoId, tagId);
        }
    }
}
