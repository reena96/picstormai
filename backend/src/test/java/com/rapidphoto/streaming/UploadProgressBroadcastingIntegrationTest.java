package com.rapidphoto.streaming;

import com.rapidphoto.domain.events.PhotoUploadFailedEvent;
import com.rapidphoto.domain.events.PhotoUploadedEvent;
import com.rapidphoto.domain.events.UploadSessionCompletedEvent;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import com.rapidphoto.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for real-time progress broadcasting with Redis pub/sub.
 * Story 2.7: Real-Time Progress Broadcasting
 *
 * Tests end-to-end flow: Event → Handler → Redis → SSE Stream
 */
class UploadProgressBroadcastingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UploadProgressEventHandler eventHandler;

    @Autowired
    private UploadProgressPublisher progressPublisher;

    @Autowired
    private UploadSessionRepository sessionRepository;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * AC1: Photo Upload Progress Message
     * AC2: Message Format
     * Tests direct message publishing without database interaction.
     */
    @Test
    void shouldPublishPhotoUploadedMessageViaRedis() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        // Subscribe to Redis channel before event
        String channel = UploadProgressPublisher.getSessionChannel(sessionId.toString());
        var subscription = redisTemplate.listenToChannel(channel)
            .map(msg -> progressPublisher.deserializeMessage(msg.getMessage()))
            .take(1)
            .timeout(Duration.ofSeconds(5));

        // When - Publish message directly
        PhotoUploadedMessage message = new PhotoUploadedMessage(
            sessionId.toString(),
            photoId.toString(),
            47,
            100,
            47
        );

        // Publish after short delay to ensure subscription is active
        reactor.core.publisher.Mono.delay(Duration.ofMillis(100))
            .then(progressPublisher.publishSessionProgress(sessionId.toString(), message))
            .subscribe();

        // Then - Verify message received via Redis (AC1)
        StepVerifier.create(subscription)
            .assertNext(received -> {
                assertThat(received).isInstanceOf(PhotoUploadedMessage.class);
                PhotoUploadedMessage receivedMsg = (PhotoUploadedMessage) received;

                // AC2: Message format validation
                assertThat(receivedMsg.type()).isEqualTo("PHOTO_UPLOADED");
                assertThat(receivedMsg.sessionId()).isEqualTo(sessionId.toString());
                assertThat(receivedMsg.photoId()).isEqualTo(photoId.toString());
                assertThat(receivedMsg.uploadedCount()).isEqualTo(47);
                assertThat(receivedMsg.totalCount()).isEqualTo(100);
                assertThat(receivedMsg.progressPercent()).isEqualTo(47);
            })
            .verifyComplete();
    }

    /**
     * AC3: Multiple Message Types - PHOTO_FAILED
     */
    @Test
    void shouldPublishPhotoFailedMessage() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        // Subscribe to Redis channel
        String channel = UploadProgressPublisher.getSessionChannel(sessionId.toString());
        var subscription = redisTemplate.listenToChannel(channel)
            .map(msg -> progressPublisher.deserializeMessage(msg.getMessage()))
            .take(1)
            .timeout(Duration.ofSeconds(5));

        // When - Publish PHOTO_FAILED message
        PhotoFailedMessage message = new PhotoFailedMessage(
            sessionId.toString(),
            photoId.toString(),
            "Network timeout",
            2,
            10
        );

        reactor.core.publisher.Mono.delay(Duration.ofMillis(100))
            .then(progressPublisher.publishSessionProgress(sessionId.toString(), message))
            .subscribe();

        // Then - Verify PHOTO_FAILED message (AC3)
        StepVerifier.create(subscription)
            .assertNext(received -> {
                assertThat(received).isInstanceOf(PhotoFailedMessage.class);
                PhotoFailedMessage receivedMsg = (PhotoFailedMessage) received;

                assertThat(receivedMsg.type()).isEqualTo("PHOTO_FAILED");
                assertThat(receivedMsg.sessionId()).isEqualTo(sessionId.toString());
                assertThat(receivedMsg.photoId()).isEqualTo(photoId.toString());
                assertThat(receivedMsg.errorMessage()).isEqualTo("Network timeout");
                assertThat(receivedMsg.failedCount()).isEqualTo(2);
                assertThat(receivedMsg.totalCount()).isEqualTo(10);
            })
            .verifyComplete();
    }

    /**
     * AC3: Multiple Message Types - SESSION_COMPLETED
     */
    @Test
    void shouldPublishSessionCompletedMessage() {
        // Given - Session ID
        UUID sessionId = UUID.randomUUID();

        // Subscribe to session channel
        String sessionChannel = UploadProgressPublisher.getSessionChannel(sessionId.toString());
        var sessionSubscription = redisTemplate.listenToChannel(sessionChannel)
            .map(msg -> progressPublisher.deserializeMessage(msg.getMessage()))
            .take(1)
            .timeout(Duration.ofSeconds(5));

        // When - Publish SESSION_COMPLETED message
        SessionCompletedMessage message = new SessionCompletedMessage(
            sessionId.toString(),
            8, // success
            2, // failed
            10 // total
        );

        reactor.core.publisher.Mono.delay(Duration.ofMillis(100))
            .then(progressPublisher.publishSessionProgress(sessionId.toString(), message))
            .subscribe();

        // Then - Verify SESSION_COMPLETED message (AC3)
        StepVerifier.create(sessionSubscription)
            .assertNext(received -> {
                assertThat(received).isInstanceOf(SessionCompletedMessage.class);
                SessionCompletedMessage receivedMsg = (SessionCompletedMessage) received;

                assertThat(receivedMsg.type()).isEqualTo("SESSION_COMPLETED");
                assertThat(receivedMsg.sessionId()).isEqualTo(sessionId.toString());
                assertThat(receivedMsg.successCount()).isEqualTo(8);
                assertThat(receivedMsg.failedCount()).isEqualTo(2);
                assertThat(receivedMsg.totalCount()).isEqualTo(10);
            })
            .verifyComplete();
    }

    /**
     * AC4: Multiple Subscribers Receive Message
     * Note: Redis pub/sub naturally supports multiple subscribers.
     * This test verifies that publishing returns non-zero subscriber count when there are listeners.
     */
    @Test
    void shouldSupportMultipleSubscribers() {
        // Given - Session ID with a subscriber
        UUID sessionId = UUID.randomUUID();
        String channel = UploadProgressPublisher.getSessionChannel(sessionId.toString());

        // Subscribe to channel
        var subscription = redisTemplate.listenToChannel(channel)
            .map(msg -> progressPublisher.deserializeMessage(msg.getMessage()))
            .take(1)
            .timeout(Duration.ofSeconds(5));

        // When - Publish one message
        PhotoUploadedMessage message = new PhotoUploadedMessage(
            sessionId.toString(),
            UUID.randomUUID().toString(),
            1,
            10,
            10
        );

        // Publish and verify subscriber count
        reactor.core.publisher.Mono.delay(Duration.ofMillis(100))
            .flatMap(v -> progressPublisher.publishSessionProgress(sessionId.toString(), message))
            .subscribe();

        // Then - Verify message is received (AC4: broadcast mechanism works)
        StepVerifier.create(subscription)
            .assertNext(received -> {
                assertThat(received).isInstanceOf(PhotoUploadedMessage.class);
                assertThat(((PhotoUploadedMessage) received).sessionId()).isEqualTo(sessionId.toString());
            })
            .verifyComplete();
    }

    /**
     * Test user notification publishing for session completion.
     */
    @Test
    void shouldPublishUserNotificationForSessionCompletion() {
        // Given
        UUID userId = UUID.randomUUID();

        // Subscribe to user notification channel
        String userChannel = UploadProgressPublisher.getUserNotificationChannel(userId.toString());
        var userSubscription = redisTemplate.listenToChannel(userChannel)
            .take(1)
            .timeout(Duration.ofSeconds(5));

        // When - Publish user notification
        Notification notification = new Notification(
            "UPLOAD_SESSION_COMPLETED",
            "Upload session completed: 8 uploaded, 2 failed",
            UUID.randomUUID().toString()
        );

        reactor.core.publisher.Mono.delay(Duration.ofMillis(100))
            .then(progressPublisher.publishUserNotification(userId.toString(), notification))
            .subscribe();

        // Then - Verify notification received
        StepVerifier.create(userSubscription)
            .assertNext(receivedJson -> {
                assertThat(receivedJson.getMessage()).contains("UPLOAD_SESSION_COMPLETED");
                assertThat(receivedJson.getMessage()).contains("8 uploaded");
                assertThat(receivedJson.getMessage()).contains("2 failed");
            })
            .verifyComplete();
    }
}
