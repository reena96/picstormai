package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.RemoveTagFromPhotoCommand;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for RemoveTagFromPhotoCommand.
 * Removes a tag association from a photo.
 */
@Service
public class RemoveTagFromPhotoCommandHandler {

    private final PhotoRepository photoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RemoveTagFromPhotoCommandHandler(
        PhotoRepository photoRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.photoRepository = photoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Mono<UUID> handle(RemoveTagFromPhotoCommand command) {
        return photoRepository.findById(command.photoId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Photo not found: " + command.photoId())))
            .flatMap(photo -> {
                // Remove tag
                photo.removeTag(command.tagId());

                // Save photo
                return photoRepository.save(photo)
                    .map(Photo::getId);
            });
    }
}
