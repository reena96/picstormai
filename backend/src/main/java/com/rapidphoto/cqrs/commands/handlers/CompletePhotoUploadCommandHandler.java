package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.CompletePhotoUploadCommand;
import com.rapidphoto.domain.events.PhotoUploadedEvent;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.photo.S3Location;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for CompletePhotoUploadCommand.
 * Marks photo as completed and updates session progress.
 */
@Service
public class CompletePhotoUploadCommandHandler {

    private final PhotoRepository photoRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CompletePhotoUploadCommandHandler(
        PhotoRepository photoRepository,
        UploadSessionRepository uploadSessionRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.photoRepository = photoRepository;
        this.uploadSessionRepository = uploadSessionRepository;
        this.eventPublisher = eventPublisher;
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
                                    return uploadSessionRepository.save(session);
                                })
                                .then(Mono.just(savedPhoto));
                        }
                        return Mono.just(savedPhoto);
                    })
                    .doOnSuccess(savedPhoto -> {
                        // Publish domain event
                        eventPublisher.publishEvent(new PhotoUploadedEvent(
                            savedPhoto.getId(),
                            savedPhoto.getUserId(),
                            savedPhoto.getSessionId(),
                            savedPhoto.getFilename()
                        ));
                    })
                    .map(Photo::getId);
            });
    }
}
