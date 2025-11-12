package com.rapidphoto.streaming;

import com.rapidphoto.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis pub/sub functionality.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 *
 * AC4: Redis Pub/Sub Integration
 */
class RedisPubSubIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    private UploadProgressPublisher progressPublisher;

    @Test
    void shouldPublishAndReceiveMessageViaRedisPubSub() {
        // Given
        String sessionId = "session-" + UUID.randomUUID();
        String channel = UploadProgressPublisher.getSessionChannel(sessionId);

        PhotoUploadedMessage message = new PhotoUploadedMessage(
            sessionId,
            "photo-123",
            1,
            10,
            10
        );

        // Subscribe to channel first
        var subscription = redisTemplate.listenToChannel(channel)
            .map(msg -> progressPublisher.deserializeMessage(msg.getMessage()))
            .take(1)
            .timeout(Duration.ofSeconds(10));

        // Publish message after short delay to ensure subscription is active
        reactor.core.publisher.Mono.delay(Duration.ofMillis(200))
            .then(progressPublisher.publishSessionProgress(sessionId, message))
            .subscribe();

        // When/Then - Verify message is received
        StepVerifier.create(subscription)
            .assertNext(received -> {
                assertThat(received).isInstanceOf(PhotoUploadedMessage.class);
                PhotoUploadedMessage uploaded = (PhotoUploadedMessage) received;
                assertThat(uploaded.sessionId()).isEqualTo(sessionId);
                assertThat(uploaded.photoId()).isEqualTo("photo-123");
                assertThat(uploaded.uploadedCount()).isEqualTo(1);
            })
            .verifyComplete();
    }

    @Test
    void shouldPublishToMultipleChannels() {
        // Given
        String sessionId1 = "session-" + UUID.randomUUID();
        String sessionId2 = "session-" + UUID.randomUUID();

        PhotoUploadedMessage message1 = new PhotoUploadedMessage(sessionId1, "photo-1", 1, 10, 10);
        PhotoUploadedMessage message2 = new PhotoUploadedMessage(sessionId2, "photo-2", 1, 10, 10);

        // When - Publish to both channels
        StepVerifier.create(
            progressPublisher.publishSessionProgress(sessionId1, message1)
        )
            .expectNextCount(1) // Expect subscriber count
            .verifyComplete();

        StepVerifier.create(
            progressPublisher.publishSessionProgress(sessionId2, message2)
        )
            .expectNextCount(1) // Expect subscriber count
            .verifyComplete();

        // Then - Both publishes succeed (verified by no errors)
    }

    @Test
    void shouldPublishUserNotification() {
        // Given
        String userId = UUID.randomUUID().toString();
        Notification notification = new Notification(
            "UPLOAD_SESSION_COMPLETED",
            "All uploads completed",
            "session-123"
        );

        // When - Publish notification
        StepVerifier.create(
            progressPublisher.publishUserNotification(userId, notification)
        )
            .assertNext(subscriberCount -> {
                // May be 0 if no subscribers, that's OK
                assertThat(subscriberCount).isNotNull();
            })
            .verifyComplete();
    }

    @Test
    void shouldHandlePublishWithNoSubscribers() {
        // Given - Channel with no subscribers
        String sessionId = "session-no-subscribers-" + UUID.randomUUID();
        PhotoUploadedMessage message = new PhotoUploadedMessage(sessionId, "photo-999", 1, 10, 10);

        // When - Publish to channel with no subscribers
        StepVerifier.create(
            progressPublisher.publishSessionProgress(sessionId, message)
        )
            .assertNext(subscriberCount -> {
                // Should be 0 since no one is subscribed
                assertThat(subscriberCount).isEqualTo(0L);
            })
            .verifyComplete();
    }
}
