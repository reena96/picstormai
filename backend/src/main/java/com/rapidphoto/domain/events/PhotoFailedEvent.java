package com.rapidphoto.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a photo upload fails.
 */
public class PhotoFailedEvent implements DomainEvent {

    private final UUID photoId;
    private final UUID userId;
    private final String errorMessage;
    private final Instant timestamp;

    public PhotoFailedEvent(UUID photoId, UUID userId, String errorMessage) {
        this.photoId = photoId;
        this.userId = userId;
        this.errorMessage = errorMessage;
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
        return "PhotoFailed";
    }

    public UUID getPhotoId() {
        return photoId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return String.format("PhotoFailedEvent{photoId=%s, userId=%s, error=%s, timestamp=%s}",
            photoId, userId, errorMessage, timestamp);
    }
}
