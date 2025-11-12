package com.rapidphoto.streaming;

import com.rapidphoto.domain.events.PhotoUploadFailedEvent;
import com.rapidphoto.domain.events.PhotoUploadedEvent;
import com.rapidphoto.domain.events.UploadSessionCompletedEvent;
import com.rapidphoto.domain.upload.SessionStatus;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UploadProgressEventHandler.
 * Story 2.7: Real-Time Progress Broadcasting
 */
@ExtendWith(MockitoExtension.class)
class UploadProgressEventHandlerTest {

    @Mock
    private UploadProgressPublisher progressPublisher;

    @Mock
    private UploadSessionRepository sessionRepository;

    private UploadProgressEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new UploadProgressEventHandler(progressPublisher, sessionRepository);
    }

    @Test
    void shouldHandlePhotoUploadedEvent() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PhotoUploadedEvent event = new PhotoUploadedEvent(
            photoId,
            userId,
            sessionId,
            "test-photo.jpg"
        );

        UploadSession session = UploadSession.start(userId);
        session.setTotalPhotos(10);
        session.recordPhotoUploaded();
        session.recordPhotoUploaded();
        session.recordPhotoUploaded(); // 3 photos uploaded

        when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
        when(progressPublisher.publishSessionProgress(any(), any())).thenReturn(Mono.just(2L));

        // When
        Mono<Void> result = eventHandler.handlePhotoUploaded(event);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        // Verify message was published
        ArgumentCaptor<PhotoUploadedMessage> messageCaptor = ArgumentCaptor.forClass(PhotoUploadedMessage.class);
        verify(progressPublisher).publishSessionProgress(eq(sessionId.toString()), messageCaptor.capture());

        PhotoUploadedMessage message = messageCaptor.getValue();
        assertThat(message.sessionId()).isEqualTo(sessionId.toString());
        assertThat(message.photoId()).isEqualTo(photoId.toString());
        assertThat(message.uploadedCount()).isEqualTo(3);
        assertThat(message.totalCount()).isEqualTo(10);
        assertThat(message.progressPercent()).isEqualTo(30); // 3/10 * 100
    }

    @Test
    void shouldHandlePhotoFailedEvent() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PhotoUploadFailedEvent event = new PhotoUploadFailedEvent(
            photoId,
            userId,
            sessionId,
            "failed-photo.jpg",
            "Network timeout"
        );

        UploadSession session = UploadSession.start(userId);
        session.setTotalPhotos(10);
        session.recordPhotoFailed();
        session.recordPhotoFailed(); // 2 photos failed

        when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
        when(progressPublisher.publishSessionProgress(any(), any())).thenReturn(Mono.just(1L));

        // When
        Mono<Void> result = eventHandler.handlePhotoFailed(event);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        // Verify message was published
        ArgumentCaptor<PhotoFailedMessage> messageCaptor = ArgumentCaptor.forClass(PhotoFailedMessage.class);
        verify(progressPublisher).publishSessionProgress(eq(sessionId.toString()), messageCaptor.capture());

        PhotoFailedMessage message = messageCaptor.getValue();
        assertThat(message.sessionId()).isEqualTo(sessionId.toString());
        assertThat(message.photoId()).isEqualTo(photoId.toString());
        assertThat(message.errorMessage()).isEqualTo("Network timeout");
        assertThat(message.failedCount()).isEqualTo(2);
        assertThat(message.totalCount()).isEqualTo(10);
    }

    @Test
    void shouldHandleSessionCompletedEvent() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UploadSessionCompletedEvent event = new UploadSessionCompletedEvent(
            sessionId,
            userId,
            8, // uploaded
            2  // failed
        );

        when(progressPublisher.publishSessionProgress(any(), any())).thenReturn(Mono.just(3L));
        when(progressPublisher.publishUserNotification(any(), any())).thenReturn(Mono.just(1L));

        // When
        Mono<Void> result = eventHandler.handleSessionCompleted(event);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        // Verify session progress message was published
        ArgumentCaptor<SessionCompletedMessage> sessionMessageCaptor =
            ArgumentCaptor.forClass(SessionCompletedMessage.class);
        verify(progressPublisher).publishSessionProgress(
            eq(sessionId.toString()),
            sessionMessageCaptor.capture()
        );

        SessionCompletedMessage sessionMessage = sessionMessageCaptor.getValue();
        assertThat(sessionMessage.sessionId()).isEqualTo(sessionId.toString());
        assertThat(sessionMessage.successCount()).isEqualTo(8);
        assertThat(sessionMessage.failedCount()).isEqualTo(2);
        assertThat(sessionMessage.totalCount()).isEqualTo(10);

        // Verify user notification was published
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(progressPublisher).publishUserNotification(
            eq(userId.toString()),
            notificationCaptor.capture()
        );

        Notification notification = notificationCaptor.getValue();
        assertThat(notification.type()).isEqualTo("UPLOAD_SESSION_COMPLETED");
        assertThat(notification.message()).contains("8 uploaded");
        assertThat(notification.message()).contains("2 failed");
        assertThat(notification.sessionId()).isEqualTo(sessionId.toString());
    }

    @Test
    void shouldHandleSessionNotFound() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PhotoUploadedEvent event = new PhotoUploadedEvent(
            photoId,
            userId,
            sessionId,
            "test-photo.jpg"
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = eventHandler.handlePhotoUploaded(event);

        // Then - Should complete without error (empty session = no event to publish)
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void shouldCalculateProgressPercentageCorrectly() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PhotoUploadedEvent event = new PhotoUploadedEvent(
            photoId,
            userId,
            sessionId,
            "test-photo.jpg"
        );

        UploadSession session = UploadSession.start(userId);
        session.setTotalPhotos(100);
        // Upload 47 photos
        for (int i = 0; i < 47; i++) {
            session.recordPhotoUploaded();
        }

        when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
        when(progressPublisher.publishSessionProgress(any(), any())).thenReturn(Mono.just(1L));

        // When
        Mono<Void> result = eventHandler.handlePhotoUploaded(event);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        // Verify progress percentage is 47%
        ArgumentCaptor<PhotoUploadedMessage> messageCaptor = ArgumentCaptor.forClass(PhotoUploadedMessage.class);
        verify(progressPublisher).publishSessionProgress(eq(sessionId.toString()), messageCaptor.capture());

        PhotoUploadedMessage message = messageCaptor.getValue();
        assertThat(message.progressPercent()).isEqualTo(47);
    }

    @Test
    void shouldHandlePublisherError() {
        // Given
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PhotoUploadedEvent event = new PhotoUploadedEvent(
            photoId,
            userId,
            sessionId,
            "test-photo.jpg"
        );

        UploadSession session = UploadSession.start(userId);
        session.setTotalPhotos(10);

        when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
        when(progressPublisher.publishSessionProgress(any(), any()))
            .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        // When
        Mono<Void> result = eventHandler.handlePhotoUploaded(event);

        // Then - Error should propagate
        StepVerifier.create(result)
            .expectErrorMessage("Redis connection failed")
            .verify();
    }
}
