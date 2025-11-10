package com.rapidphoto.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events.
 * Domain events represent facts that happened in the domain.
 */
public interface DomainEvent {

    /**
     * Get the unique identifier of the aggregate that emitted this event.
     */
    UUID getAggregateId();

    /**
     * Get the timestamp when this event occurred.
     */
    Instant getTimestamp();

    /**
     * Get the event type name.
     */
    String getEventType();
}
