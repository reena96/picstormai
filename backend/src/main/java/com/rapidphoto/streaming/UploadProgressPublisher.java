package com.rapidphoto.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for publishing upload progress messages to Redis pub/sub channels.
 * Messages are broadcast to all backend instances and then sent to SSE clients.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 */
@Service
public class UploadProgressPublisher {

    private static final Logger log = LoggerFactory.getLogger(UploadProgressPublisher.class);
    private static final String SESSION_CHANNEL_PREFIX = "upload-session:";
    private static final String USER_NOTIFICATION_PREFIX = "user-notifications:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public UploadProgressPublisher(
        ReactiveRedisTemplate<String, String> redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish progress update for a specific upload session.
     * All backend instances subscribed to this session will receive the message.
     *
     * @param sessionId The upload session ID
     * @param message The progress message to broadcast
     * @return Number of subscribers that received the message
     */
    public Mono<Long> publishSessionProgress(String sessionId, UploadProgressMessage message) {
        String channel = SESSION_CHANNEL_PREFIX + sessionId;
        String messageJson = serializeMessage(message);

        return redisTemplate.convertAndSend(channel, messageJson)
            .doOnSuccess(count -> log.info("Published {} to channel {} - {} subscribers received",
                message.type(), channel, count))
            .doOnError(error -> log.error("Failed to publish message to channel {}: {}",
                channel, error.getMessage()));
    }

    /**
     * Publish notification to a specific user.
     * Used for user-level notifications (e.g., "All uploads completed").
     *
     * @param userId The user ID
     * @param notification The notification object
     * @return Number of subscribers that received the notification
     */
    public Mono<Long> publishUserNotification(String userId, Notification notification) {
        String channel = USER_NOTIFICATION_PREFIX + userId;
        String notificationJson = serializeNotification(notification);

        return redisTemplate.convertAndSend(channel, notificationJson)
            .doOnSuccess(count -> log.info("Published notification to user {} - {} subscribers",
                userId, count))
            .doOnError(error -> log.error("Failed to publish notification to user {}: {}",
                userId, error.getMessage()));
    }

    /**
     * Serialize notification to JSON.
     */
    private String serializeNotification(Notification notification) {
        try {
            return objectMapper.writeValueAsString(notification);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification: {}", notification, e);
            throw new RuntimeException("Failed to serialize notification", e);
        }
    }

    /**
     * Serialize message to JSON.
     * Jackson will automatically include the "type" discriminator field.
     */
    private String serializeMessage(UploadProgressMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", message, e);
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    /**
     * Deserialize JSON string to UploadProgressMessage.
     * Jackson will automatically determine the correct subtype based on "type" field.
     */
    public UploadProgressMessage deserializeMessage(String json) {
        try {
            return objectMapper.readValue(json, UploadProgressMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize message: {}", json, e);
            throw new RuntimeException("Failed to deserialize message", e);
        }
    }

    /**
     * Get the channel name for a session ID.
     * Useful for subscribing to session updates.
     */
    public static String getSessionChannel(String sessionId) {
        return SESSION_CHANNEL_PREFIX + sessionId;
    }

    /**
     * Get the channel name for a user ID.
     * Useful for subscribing to user notifications.
     */
    public static String getUserNotificationChannel(String userId) {
        return USER_NOTIFICATION_PREFIX + userId;
    }
}
