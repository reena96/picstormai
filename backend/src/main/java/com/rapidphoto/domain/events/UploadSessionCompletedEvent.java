package com.rapidphoto.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when all photos in an upload session have finished processing.
 * Story 2.7: Real-Time Progress Broadcasting
 */
public class UploadSessionCompletedEvent implements DomainEvent {

    private final UUID sessionId;
    private final UUID userId;
    private final int uploadedCount;
    private final int failedCount;
    private final Instant timestamp;

    public UploadSessionCompletedEvent(UUID sessionId, UUID userId, int uploadedCount, int failedCount) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.uploadedCount = uploadedCount;
        this.failedCount = failedCount;
        this.timestamp = Instant.now();
    }

    @Override
    public UUID getAggregateId() {
        return sessionId;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return "UploadSessionCompleted";
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getUploadedCount() {
        return uploadedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    @Override
    public String toString() {
        return String.format("UploadSessionCompletedEvent{sessionId=%s, userId=%s, uploaded=%d, failed=%d, timestamp=%s}",
            sessionId, userId, uploadedCount, failedCount, timestamp);
    }
}
