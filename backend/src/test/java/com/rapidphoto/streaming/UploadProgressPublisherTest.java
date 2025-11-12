package com.rapidphoto.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UploadProgressPublisher.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 */
@ExtendWith(MockitoExtension.class)
class UploadProgressPublisherTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    private UploadProgressPublisher publisher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new UploadProgressPublisher(redisTemplate, objectMapper);
    }

    @Test
    void shouldPublishPhotoUploadedMessage() {
        // Given
        String sessionId = "session-123";
        PhotoUploadedMessage message = new PhotoUploadedMessage(
            sessionId,
            "photo-456",
            5,
            10,
            50
        );

        when(redisTemplate.convertAndSend(anyString(), anyString()))
            .thenReturn(Mono.just(2L)); // 2 subscribers received

        // When
        Mono<Long> result = publisher.publishSessionProgress(sessionId, message);

        // Then
        StepVerifier.create(result)
            .assertNext(count -> assertThat(count).isEqualTo(2L))
            .verifyComplete();

        verify(redisTemplate).convertAndSend(
            eq("upload-session:session-123"),
            anyString()
        );
    }

    @Test
    void shouldPublishPhotoFailedMessage() {
        // Given
        String sessionId = "session-789";
        PhotoFailedMessage message = new PhotoFailedMessage(
            sessionId,
            "photo-999",
            "Network timeout",
            1,
            10
        );

        when(redisTemplate.convertAndSend(anyString(), anyString()))
            .thenReturn(Mono.just(1L));

        // When
        Mono<Long> result = publisher.publishSessionProgress(sessionId, message);

        // Then
        StepVerifier.create(result)
            .assertNext(count -> assertThat(count).isEqualTo(1L))
            .verifyComplete();
    }

    @Test
    void shouldPublishSessionCompletedMessage() {
        // Given
        String sessionId = "session-complete";
        SessionCompletedMessage message = new SessionCompletedMessage(
            sessionId,
            9,
            1,
            10
        );

        when(redisTemplate.convertAndSend(anyString(), anyString()))
            .thenReturn(Mono.just(3L));

        // When
        Mono<Long> result = publisher.publishSessionProgress(sessionId, message);

        // Then
        StepVerifier.create(result)
            .assertNext(count -> assertThat(count).isEqualTo(3L))
            .verifyComplete();
    }

    @Test
    void shouldPublishUserNotification() {
        // Given
        String userId = "user-123";
        Notification notification = new Notification(
            "UPLOAD_SESSION_COMPLETED",
            "All uploads completed",
            "session-123"
        );

        when(redisTemplate.convertAndSend(anyString(), anyString()))
            .thenReturn(Mono.just(1L));

        // When
        Mono<Long> result = publisher.publishUserNotification(userId, notification);

        // Then
        StepVerifier.create(result)
            .assertNext(count -> assertThat(count).isEqualTo(1L))
            .verifyComplete();

        verify(redisTemplate).convertAndSend(
            eq("user-notifications:user-123"),
            anyString()
        );
    }

    @Test
    void shouldSerializeAndDeserializePhotoUploadedMessage() {
        // Given
        PhotoUploadedMessage original = new PhotoUploadedMessage(
            "session-123",
            "photo-456",
            5,
            10,
            50
        );

        // When - Serialize
        String json = null;
        try {
            json = objectMapper.writeValueAsString(original);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then - Verify JSON contains type field
        assertThat(json).contains("\"type\":\"PHOTO_UPLOADED\"");
        assertThat(json).contains("\"sessionId\":\"session-123\"");
        assertThat(json).contains("\"photoId\":\"photo-456\"");

        // When - Deserialize
        UploadProgressMessage deserialized = publisher.deserializeMessage(json);

        // Then - Verify correct type and data
        assertThat(deserialized).isInstanceOf(PhotoUploadedMessage.class);
        PhotoUploadedMessage uploaded = (PhotoUploadedMessage) deserialized;
        assertThat(uploaded.sessionId()).isEqualTo("session-123");
        assertThat(uploaded.photoId()).isEqualTo("photo-456");
        assertThat(uploaded.uploadedCount()).isEqualTo(5);
        assertThat(uploaded.totalCount()).isEqualTo(10);
        assertThat(uploaded.progressPercent()).isEqualTo(50);
    }

    @Test
    void shouldSerializeAndDeserializePhotoFailedMessage() {
        // Given
        PhotoFailedMessage original = new PhotoFailedMessage(
            "session-789",
            "photo-999",
            "Network timeout",
            1,
            10
        );

        // When - Serialize
        String json = null;
        try {
            json = objectMapper.writeValueAsString(original);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then - Verify JSON contains type field
        assertThat(json).contains("\"type\":\"PHOTO_FAILED\"");
        assertThat(json).contains("\"errorMessage\":\"Network timeout\"");

        // When - Deserialize
        UploadProgressMessage deserialized = publisher.deserializeMessage(json);

        // Then - Verify correct type and data
        assertThat(deserialized).isInstanceOf(PhotoFailedMessage.class);
        PhotoFailedMessage failed = (PhotoFailedMessage) deserialized;
        assertThat(failed.sessionId()).isEqualTo("session-789");
        assertThat(failed.photoId()).isEqualTo("photo-999");
        assertThat(failed.errorMessage()).isEqualTo("Network timeout");
    }

    @Test
    void shouldGenerateCorrectChannelNames() {
        // Test session channel
        String sessionChannel = UploadProgressPublisher.getSessionChannel("session-123");
        assertThat(sessionChannel).isEqualTo("upload-session:session-123");

        // Test user notification channel
        String userChannel = UploadProgressPublisher.getUserNotificationChannel("user-456");
        assertThat(userChannel).isEqualTo("user-notifications:user-456");
    }

    @Test
    void shouldHandlePublishError() {
        // Given
        String sessionId = "session-error";
        PhotoUploadedMessage message = new PhotoUploadedMessage(
            sessionId,
            "photo-123",
            1,
            10,
            10
        );

        when(redisTemplate.convertAndSend(anyString(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        // When
        Mono<Long> result = publisher.publishSessionProgress(sessionId, message);

        // Then
        StepVerifier.create(result)
            .expectErrorMessage("Redis connection failed")
            .verify();
    }
}
