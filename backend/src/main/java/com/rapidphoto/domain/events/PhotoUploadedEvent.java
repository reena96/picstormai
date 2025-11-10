package com.rapidphoto.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a photo upload completes successfully.
 */
public class PhotoUploadedEvent implements DomainEvent {

    private final UUID photoId;
    private final UUID userId;
    private final UUID sessionId;
    private final String filename;
    private final Instant timestamp;

    public PhotoUploadedEvent(UUID photoId, UUID userId, UUID sessionId, String filename) {
        this.photoId = photoId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.filename = filename;
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
        return "PhotoUploaded";
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

    @Override
    public String toString() {
        return String.format("PhotoUploadedEvent{photoId=%s, filename=%s, userId=%s, timestamp=%s}",
            photoId, filename, userId, timestamp);
    }
}
