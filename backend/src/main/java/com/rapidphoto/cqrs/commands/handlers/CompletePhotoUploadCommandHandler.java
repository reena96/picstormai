package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.CompletePhotoUploadCommand;
import com.rapidphoto.domain.events.PhotoUploadedEvent;
import com.rapidphoto.domain.events.UploadSessionCompletedEvent;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.photo.S3Location;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import com.rapidphoto.streaming.UploadProgressEventHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for CompletePhotoUploadCommand.
 * Marks photo as completed and updates session progress.
 * Story 2.7: Real-Time Progress Broadcasting
 */
@Service
public class CompletePhotoUploadCommandHandler {

    private final PhotoRepository photoRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final UploadProgressEventHandler eventHandler;

    public CompletePhotoUploadCommandHandler(
        PhotoRepository photoRepository,
        UploadSessionRepository uploadSessionRepository,
        UploadProgressEventHandler eventHandler
    ) {
        this.photoRepository = photoRepository;
        this.uploadSessionRepository = uploadSessionRepository;
        this.eventHandler = eventHandler;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<UUID> handle(CompletePhotoUploadCommand command) {
        return photoRepository.findById(command.photoId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Photo not found: " + command.photoId())))
            .flatMap(photo -> {
                // Create S3 location
                S3Location location = S3Location.of(
                    "rapidphoto-uploads",
                    command.s3Key(),
                    command.s3VersionId()
                );

                // Start upload if not already started
                if (photo.getS3Location() == null) {
                    photo.startUpload(location);
                }

                // Complete upload
                photo.completeUpload(command.exifMetadata());

                // Save photo
                return photoRepository.save(photo)
                    .flatMap(savedPhoto -> {
                        // Update session progress
                        if (savedPhoto.getSessionId() != null) {
                            return uploadSessionRepository.findById(savedPhoto.getSessionId())
                                .flatMap(session -> {
                                    session.recordPhotoUploaded();

                                    // Check if session is complete
                                    boolean isSessionComplete =
                                        (session.getCompletedPhotos() + session.getFailedPhotos())
                                        == session.getTotalPhotos();

                                    // Save session first (within transaction)
                                    return uploadSessionRepository.save(session)
                                        .flatMap(savedSession -> {
                                            // Create photo uploaded event
                                            PhotoUploadedEvent photoEvent = new PhotoUploadedEvent(
                                                savedPhoto.getId(),
                                                savedPhoto.getUserId(),
                                                savedPhoto.getSessionId(),
                                                savedPhoto.getFilename()
                                            );

                                            // Publish photo uploaded event to Redis
                                            Mono<Void> photoEventPublish = eventHandler.handlePhotoUploaded(photoEvent);

                                            // If session complete, also publish completion event
                                            if (isSessionComplete) {
                                                UploadSessionCompletedEvent completionEvent =
                                                    new UploadSessionCompletedEvent(
                                                        savedSession.getId(),
                                                        savedSession.getUserId(),
                                                        savedSession.getCompletedPhotos(),
                                                        savedSession.getFailedPhotos()
                                                    );

                                                Mono<Void> completionPublish =
                                                    eventHandler.handleSessionCompleted(completionEvent);

                                                // Execute both publishes in parallel
                                                return Mono.when(photoEventPublish, completionPublish)
                                                    .thenReturn(savedPhoto);
                                            } else {
                                                return photoEventPublish.thenReturn(savedPhoto);
                                            }
                                        });
                                })
                                .thenReturn(savedPhoto);
                        }
                        return Mono.just(savedPhoto);
                    })
                    .map(Photo::getId);
            });
    }
}
