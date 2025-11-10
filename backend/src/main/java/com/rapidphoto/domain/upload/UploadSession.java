package com.rapidphoto.domain.upload;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * UploadSession aggregate root.
 * Manages batch photo upload sessions with progress tracking.
 * No setters - all modifications through business methods.
 */
@Table("upload_sessions")
public class UploadSession implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID userId;
    private SessionStatus status;
    private int totalPhotos;
    private int completedPhotos;
    private int failedPhotos;
    private Instant createdAt;
    private Instant completedAt;

    @Transient
    private boolean isNew = true;

    // Package-private constructor for persistence (entities loaded from DB are not new)
    UploadSession() {
        this.isNew = false;
    }

    private UploadSession(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
        this.status = SessionStatus.IN_PROGRESS;
        this.totalPhotos = 0;
        this.completedPhotos = 0;
        this.failedPhotos = 0;
        this.createdAt = Instant.now();
    }

    /**
     * Factory method to start new upload session.
     */
    public static UploadSession start(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return new UploadSession(UUID.randomUUID(), userId);
    }

    /**
     * Set total number of photos in this session.
     * Can only be called when status is IN_PROGRESS.
     */
    public void setTotalPhotos(int total) {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot set total photos on completed session");
        }
        if (total < 0) {
            throw new IllegalArgumentException("Total photos cannot be negative");
        }
        this.totalPhotos = total;
    }

    /**
     * Record that a photo was successfully uploaded.
     */
    public void recordPhotoUploaded() {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot record photo on non-active session");
        }
        this.completedPhotos++;
    }

    /**
     * Record that a photo upload failed.
     */
    public void recordPhotoFailed() {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot record failure on non-active session");
        }
        this.failedPhotos++;
    }

    /**
     * Mark session as completed successfully.
     * Can only transition from IN_PROGRESS.
     */
    public void complete() {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Can only complete session from IN_PROGRESS state, current: " + status
            );
        }
        this.status = SessionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Mark session as failed.
     * Can only transition from IN_PROGRESS.
     */
    public void fail() {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Can only fail session from IN_PROGRESS state, current: " + status
            );
        }
        this.status = SessionStatus.FAILED;
        this.completedAt = Instant.now();
    }

    /**
     * Cancel the upload session.
     * Can only transition from IN_PROGRESS.
     */
    public void cancel() {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Can only cancel session from IN_PROGRESS state, current: " + status
            );
        }
        this.status = SessionStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    /**
     * Calculate upload progress percentage (0-100).
     */
    public int getProgressPercentage() {
        if (totalPhotos == 0) {
            return 0;
        }
        return (int) ((completedPhotos * 100.0) / totalPhotos);
    }

    /**
     * Check if session is still active.
     */
    public boolean isActive() {
        return status == SessionStatus.IN_PROGRESS;
    }

    // Getters (no setters - immutability enforced)

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public int getTotalPhotos() {
        return totalPhotos;
    }

    public int getCompletedPhotos() {
        return completedPhotos;
    }

    public int getFailedPhotos() {
        return failedPhotos;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    // Persistable interface methods
    @Override
    public boolean isNew() {
        return isNew;
    }
}
