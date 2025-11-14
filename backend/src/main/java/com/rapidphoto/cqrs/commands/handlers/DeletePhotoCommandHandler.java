package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.DeletePhotoCommand;
import com.rapidphoto.domain.photo.PhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Handler for DeletePhotoCommand.
 * Soft deletes a photo by setting deletedAt timestamp.
 */
@Service
public class DeletePhotoCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(DeletePhotoCommandHandler.class);
    private final PhotoRepository photoRepository;

    public DeletePhotoCommandHandler(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    @Transactional
    public Mono<Void> handle(DeletePhotoCommand command) {
        log.info("Deleting photo {} for user {}", command.photoId(), command.userId());

        return photoRepository.findById(command.photoId())
            .switchIfEmpty(Mono.error(new PhotoNotFoundException("Photo not found")))
            .flatMap(photo -> {
                // Verify ownership
                if (!photo.getUserId().equals(command.userId())) {
                    log.warn("User {} attempted to delete photo {} owned by {}",
                        command.userId(), command.photoId(), photo.getUserId());
                    return Mono.error(new UnauthorizedException("Not authorized to delete this photo"));
                }

                // Soft delete
                photo.softDelete();
                log.info("Photo {} soft deleted", command.photoId());

                return photoRepository.save(photo);
            })
            .then();
    }

    public static class PhotoNotFoundException extends RuntimeException {
        public PhotoNotFoundException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
