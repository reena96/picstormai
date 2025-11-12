package com.rapidphoto.streaming;

/**
 * Notification message for user-level events.
 * Story 2.7: Real-Time Progress Broadcasting
 */
public record Notification(
    String type,
    String message,
    String sessionId
) {
}
