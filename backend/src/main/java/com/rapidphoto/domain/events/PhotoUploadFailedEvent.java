package com.rapidphoto.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a photo upload fails.
 * Story 2.7: Real-Time Progress Broadcasting
 */
public class PhotoUploadFailedEvent implements DomainEvent {

    private final UUID photoId;
    private final UUID userId;
    private final UUID sessionId;
    private final String filename;
    private final String reason;
    private final Instant timestamp;

    public PhotoUploadFailedEvent(UUID photoId, UUID userId, UUID sessionId, String filename, String reason) {
        this.photoId = photoId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.filename = filename;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    @Override
    public UUID getAggregateId() {
        return photoId;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return "PhotoUploadFailed";
    }

    public UUID getPhotoId() {
        return photoId;
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

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("PhotoUploadFailedEvent{photoId=%s, filename=%s, userId=%s, reason=%s, timestamp=%s}",
            photoId, filename, userId, reason, timestamp);
    }
}
