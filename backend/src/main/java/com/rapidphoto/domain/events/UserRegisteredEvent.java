package com.rapidphoto.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new user registers.
 */
public class UserRegisteredEvent implements DomainEvent {

    private final UUID userId;
    private final String email;
    private final Instant timestamp;

    public UserRegisteredEvent(UUID userId, String email) {
        this.userId = userId;
        this.email = email;
        this.timestamp = Instant.now();
    }

    @Override
    public UUID getAggregateId() {
        return userId;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return "UserRegistered";
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return String.format("UserRegisteredEvent{userId=%s, email=%s, timestamp=%s}",
            userId, email, timestamp);
    }
}
