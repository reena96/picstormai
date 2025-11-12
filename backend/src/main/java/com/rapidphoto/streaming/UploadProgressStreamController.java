package com.rapidphoto.streaming;

import com.rapidphoto.security.CurrentUser;
import com.rapidphoto.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.UUID;

/**
 * SSE (Server-Sent Events) controller for real-time upload progress streaming.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 *
 * Endpoints:
 * - GET /api/upload/sessions/{sessionId}/stream - Stream progress for specific session
 * - GET /api/upload/sessions/notifications/stream - Stream user-level notifications
 */
@RestController
@RequestMapping("/api/upload/sessions")
public class UploadProgressStreamController {

    private static final Logger log = LoggerFactory.getLogger(UploadProgressStreamController.class);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final UploadProgressPublisher progressPublisher;

    public UploadProgressStreamController(
        ReactiveRedisTemplate<String, String> redisTemplate,
        UploadProgressPublisher progressPublisher
    ) {
        this.redisTemplate = redisTemplate;
        this.progressPublisher = progressPublisher;
    }

    /**
     * Stream progress updates for a specific upload session.
     * Client connects with EventSource API:
     * const eventSource = new EventSource('/api/upload/sessions/{sessionId}/stream');
     *
     * AC1: SSE connection succeeds with valid JWT
     * AC2: Stream endpoint available
     * AC3: JWT authentication required
     * AC4: Redis pub/sub integration
     * AC5: Automatic reconnection support
     */
    @GetMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<UploadProgressMessage>> streamProgress(
        @PathVariable String sessionId,
        @CurrentUser UserPrincipal currentUser
    ) {
        log.info("Client {} subscribing to session {}", currentUser.userId(), sessionId);

        // TODO: Verify user owns this session (Story 2.2 - StartUploadSession)
        // For now, we trust the authenticated user

        String channel = UploadProgressPublisher.getSessionChannel(sessionId);

        // Subscribe to Redis pub/sub channel for this session
        Flux<ServerSentEvent<UploadProgressMessage>> messageFlux = redisTemplate
            .listenToChannel(channel)
            .map(message -> {
                UploadProgressMessage progress = progressPublisher.deserializeMessage(
                    message.getMessage()
                );
                return ServerSentEvent.<UploadProgressMessage>builder()
                    .event(progress.type())
                    .data(progress)
                    .id(UUID.randomUUID().toString())
                    .build();
            })
            .doOnSubscribe(s -> log.info("SSE stream started for session {}", sessionId))
            .doOnCancel(() -> log.info("SSE stream cancelled for session {}", sessionId))
            .doOnComplete(() -> log.info("SSE stream completed for session {}", sessionId))
            .doOnError(error -> log.error("SSE stream error for session {}: {}",
                sessionId, error.getMessage()));

        // Add heartbeat to keep connection alive
        Flux<ServerSentEvent<UploadProgressMessage>> heartbeat = Flux.interval(HEARTBEAT_INTERVAL)
            .map(tick -> ServerSentEvent.<UploadProgressMessage>builder()
                .comment("heartbeat")
                .build());

        // Merge messages and heartbeat
        return Flux.merge(messageFlux, heartbeat);
    }

    /**
     * Stream user-level notifications.
     * Used for notifications like "All sessions completed", "Storage quota exceeded", etc.
     *
     * Client connects with:
     * const eventSource = new EventSource('/api/upload/sessions/notifications/stream');
     */
    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamUserNotifications(
        @CurrentUser UserPrincipal currentUser
    ) {
        String userId = currentUser.userId().toString();
        log.info("Client {} subscribing to user notifications", userId);

        String channel = UploadProgressPublisher.getUserNotificationChannel(userId);

        // Subscribe to Redis pub/sub channel for this user
        Flux<ServerSentEvent<String>> messageFlux = redisTemplate
            .listenToChannel(channel)
            .map(message -> ServerSentEvent.<String>builder()
                .event("notification")
                .data(message.getMessage())
                .id(UUID.randomUUID().toString())
                .build())
            .doOnSubscribe(s -> log.info("SSE notification stream started for user {}", userId))
            .doOnCancel(() -> log.info("SSE notification stream cancelled for user {}", userId))
            .doOnError(error -> log.error("SSE notification stream error for user {}: {}",
                userId, error.getMessage()));

        // Add heartbeat to keep connection alive
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(HEARTBEAT_INTERVAL)
            .map(tick -> ServerSentEvent.<String>builder()
                .comment("heartbeat")
                .build());

        // Merge messages and heartbeat
        return Flux.merge(messageFlux, heartbeat);
    }
}
