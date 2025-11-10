package com.rapidphoto.domain.photo;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Photo aggregate root.
 * Manages photo lifecycle from upload to completion.
 * No setters - all modifications through business methods.
 */
@Table("photos")
public class Photo implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID userId;
    private UUID sessionId;
    private String filename;
    private long fileSize;
    private String s3Key;
    private String s3VersionId;
    private String storageUrl;
    private PhotoStatus uploadStatus;
    private int progress; // 0-100
    private Map<String, Object> metadata; // JSONB in database
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    @Transient
    private Set<UUID> tagIds = new HashSet<>(); // Tags managed separately

    @Transient
    private boolean isNew = true;

    // Package-private constructor for persistence (entities loaded from DB are not new)
    Photo() {
        this.isNew = false;
    }

    private Photo(UUID id, UUID userId, UUID sessionId, String filename, long fileSize) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.uploadStatus = PhotoStatus.PENDING;
        this.progress = 0;
        this.metadata = new HashMap<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Factory method to initiate new photo upload.
     */
    public static Photo initiate(UUID userId, UUID sessionId, String filename, long fileSize) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }

        return new Photo(UUID.randomUUID(), userId, sessionId, filename.trim(), fileSize);
    }

    /**
     * Start upload with S3 location.
     */
    public void startUpload(S3Location location) {
        if (uploadStatus != PhotoStatus.PENDING) {
            throw new IllegalStateException("Can only start upload from PENDING state");
        }
        if (location == null) {
            throw new IllegalArgumentException("S3 location cannot be null");
        }

        this.s3Key = location.getKey();
        this.s3VersionId = location.getVersionId();
        this.storageUrl = location.toUri();
        this.uploadStatus = PhotoStatus.UPLOADING;
        this.updatedAt = Instant.now();
    }

    /**
     * Update upload progress (0-100).
     */
    public void updateProgress(int percentage) {
        if (uploadStatus != PhotoStatus.UPLOADING) {
            throw new IllegalStateException("Can only update progress when UPLOADING");
        }
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }

        this.progress = percentage;
        this.updatedAt = Instant.now();
    }

    /**
     * Complete upload with optional EXIF metadata.
     */
    public void completeUpload(Map<String, Object> exifMetadata) {
        if (uploadStatus != PhotoStatus.UPLOADING) {
            throw new IllegalStateException("Can only complete from UPLOADING state");
        }
        if (s3Key == null || s3Key.isEmpty()) {
            throw new IllegalStateException("S3 location required before completing upload");
        }

        this.uploadStatus = PhotoStatus.COMPLETED;
        this.progress = 100;
        if (exifMetadata != null) {
            this.metadata.putAll(exifMetadata);
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Mark upload as failed with error message.
     */
    public void failUpload(String errorMessage) {
        if (uploadStatus == PhotoStatus.COMPLETED) {
            throw new IllegalStateException("Cannot fail already completed upload");
        }

        this.uploadStatus = PhotoStatus.FAILED;
        if (errorMessage != null) {
            this.metadata.put("errorMessage", errorMessage);
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Retry failed upload (reset to PENDING).
     */
    public void retry() {
        if (uploadStatus != PhotoStatus.FAILED) {
            throw new IllegalStateException("Can only retry from FAILED state");
        }

        this.uploadStatus = PhotoStatus.PENDING;
        this.progress = 0;
        this.metadata.remove("errorMessage");
        this.updatedAt = Instant.now();
    }

    /**
     * Add tag to photo.
     */
    public void addTag(UUID tagId) {
        if (tagId == null) {
            throw new IllegalArgumentException("Tag ID cannot be null");
        }
        this.tagIds.add(tagId);
        this.updatedAt = Instant.now();
    }

    /**
     * Remove tag from photo.
     */
    public void removeTag(UUID tagId) {
        if (tagId == null) {
            throw new IllegalArgumentException("Tag ID cannot be null");
        }
        this.tagIds.remove(tagId);
        this.updatedAt = Instant.now();
    }

    /**
     * Soft delete photo (sets deletedAt timestamp).
     */
    public void softDelete() {
        if (this.deletedAt != null) {
            return; // Already deleted, idempotent
        }
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Restore soft-deleted photo.
     */
    public void restore() {
        if (this.deletedAt == null) {
            return; // Not deleted, idempotent
        }
        this.deletedAt = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if photo is deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    // Getters (no setters - immutability enforced)

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getFilename() {
        return filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public S3Location getS3Location() {
        if (s3Key == null) {
            return null;
        }
        // Reconstruct from stored data (bucket would come from config)
        return S3Location.of("rapidphoto-uploads", s3Key, s3VersionId);
    }

    public PhotoStatus getUploadStatus() {
        return uploadStatus;
    }

    public int getProgress() {
        return progress;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata); // Return copy for immutability
    }

    public Set<UUID> getTagIds() {
        return new HashSet<>(tagIds); // Return copy for immutability
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    // Persistable interface methods
    @Override
    public boolean isNew() {
        return isNew;
    }
}
