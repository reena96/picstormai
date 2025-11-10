package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.AddTagToPhotoCommand;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for AddTagToPhotoCommand.
 * Associates a tag with a photo.
 */
@Service
public class AddTagToPhotoCommandHandler {

    private final PhotoRepository photoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AddTagToPhotoCommandHandler(
        PhotoRepository photoRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.photoRepository = photoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Mono<UUID> handle(AddTagToPhotoCommand command) {
        return photoRepository.findById(command.photoId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Photo not found: " + command.photoId())))
            .flatMap(photo -> {
                // Add tag
                photo.addTag(command.tagId());

                // Save photo
                return photoRepository.save(photo)
                    .map(Photo::getId);
            });
    }
}
