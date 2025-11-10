package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.InitiatePhotoUploadCommand;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for InitiatePhotoUploadCommand.
 * Creates a new photo record in PENDING status.
 */
@Service
public class InitiatePhotoUploadCommandHandler {

    private final PhotoRepository photoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InitiatePhotoUploadCommandHandler(
        PhotoRepository photoRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.photoRepository = photoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<UUID> handle(InitiatePhotoUploadCommand command) {
        // Create new photo
        Photo photo = Photo.initiate(
            command.userId(),
            command.sessionId(),
            command.filename(),
            command.fileSize()
        );

        // Save photo
        return photoRepository.save(photo)
            .map(Photo::getId);
    }
}
