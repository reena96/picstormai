package com.rapidphoto.streaming;

import com.rapidphoto.domain.events.PhotoUploadFailedEvent;
import com.rapidphoto.domain.events.PhotoUploadedEvent;
import com.rapidphoto.domain.events.UploadSessionCompletedEvent;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Handles domain events and publishes progress messages to Redis pub/sub.
 * Story 2.7: Real-Time Progress Broadcasting
 */
@Service
public class UploadProgressEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UploadProgressEventHandler.class);

    private final UploadProgressPublisher progressPublisher;
    private final UploadSessionRepository sessionRepository;

    public UploadProgressEventHandler(
        UploadProgressPublisher progressPublisher,
        UploadSessionRepository sessionRepository
    ) {
        this.progressPublisher = progressPublisher;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Handle photo upload completion event.
     * Publishes progress update to Redis after database save completes.
     */
    public Mono<Void> handlePhotoUploaded(PhotoUploadedEvent event) {
        // Fetch latest session state and publish to Redis
        return sessionRepository.findById(event.getSessionId())
            .flatMap(session -> {
                PhotoUploadedMessage message = new PhotoUploadedMessage(
                    event.getSessionId().toString(),
                    event.getPhotoId().toString(),
                    session.getCompletedPhotos(),
                    session.getTotalPhotos(),
                    session.getProgressPercentage()
                );

                // Publish to Redis channel (SSE clients will receive this)
                return progressPublisher.publishSessionProgress(event.getSessionId().toString(), message)
                    .doOnSuccess(count ->
                        log.info("Published PHOTO_UPLOADED to {} subscribers - Progress: {}/{}",
                            count, session.getCompletedPhotos(), session.getTotalPhotos())
                    );
            })
            .then();
    }

    /**
     * Handle photo upload failure event.
     * Publishes failure notification to Redis.
     */
    public Mono<Void> handlePhotoFailed(PhotoUploadFailedEvent event) {
        return sessionRepository.findById(event.getSessionId())
            .flatMap(session -> {
                PhotoFailedMessage message = new PhotoFailedMessage(
                    event.getSessionId().toString(),
                    event.getPhotoId().toString(),
                    event.getReason(),
                    session.getFailedPhotos(),
                    session.getTotalPhotos()
                );

                return progressPublisher.publishSessionProgress(event.getSessionId().toString(), message)
                    .doOnSuccess(count ->
                        log.warn("Published PHOTO_FAILED to {} subscribers - Reason: {}",
                            count, event.getReason())
                    );
            })
            .then();
    }

    /**
     * Handle session completion event.
     * Publishes to both session channel and user notification channel.
     */
    public Mono<Void> handleSessionCompleted(UploadSessionCompletedEvent event) {
        SessionCompletedMessage message = new SessionCompletedMessage(
            event.getSessionId().toString(),
            event.getUploadedCount(),
            event.getFailedCount(),
            event.getUploadedCount() + event.getFailedCount()
        );

        // Publish to session channel
        Mono<Void> sessionNotification = progressPublisher
            .publishSessionProgress(event.getSessionId().toString(), message)
            .doOnSuccess(count ->
                log.info("Published SESSION_COMPLETED to {} subscribers - Uploaded: {}, Failed: {}",
                    count, event.getUploadedCount(), event.getFailedCount())
            )
            .then();

        // Also publish user-specific notification
        Notification userNotification = new Notification(
            "UPLOAD_SESSION_COMPLETED",
            "Upload session completed: " + event.getUploadedCount() + " uploaded, " + event.getFailedCount() + " failed",
            event.getSessionId().toString()
        );

        Mono<Void> userNotificationPublish = progressPublisher
            .publishUserNotification(event.getUserId().toString(), userNotification)
            .then();

        // Execute both publishes in parallel
        return Mono.when(sessionNotification, userNotificationPublish);
    }
}
