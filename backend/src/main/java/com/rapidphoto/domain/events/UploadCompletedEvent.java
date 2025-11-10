package com.rapidphoto.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an upload session completes successfully.
 */
public class UploadCompletedEvent implements DomainEvent {

    private final UUID sessionId;
    private final UUID userId;
    private final int totalPhotos;
    private final int completedPhotos;
    private final Instant timestamp;

    public UploadCompletedEvent(UUID sessionId, UUID userId, int totalPhotos, int completedPhotos) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.totalPhotos = totalPhotos;
        this.completedPhotos = completedPhotos;
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
        return "UploadCompleted";
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getTotalPhotos() {
        return totalPhotos;
    }

    public int getCompletedPhotos() {
        return completedPhotos;
    }

    @Override
    public String toString() {
        return String.format("UploadCompletedEvent{sessionId=%s, userId=%s, completed=%d/%d, timestamp=%s}",
            sessionId, userId, completedPhotos, totalPhotos, timestamp);
    }
}
