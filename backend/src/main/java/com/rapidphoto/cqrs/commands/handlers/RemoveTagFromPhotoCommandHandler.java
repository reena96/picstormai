package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.RemoveTagFromPhotoCommand;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.tag.PhotoTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Handler for RemoveTagFromPhotoCommand.
 * Removes a tag association from a photo.
 * Tag entity remains in database (may be used by other photos).
 */
@Service
public class RemoveTagFromPhotoCommandHandler {

    private final PhotoRepository photoRepository;
    private final PhotoTagRepository photoTagRepository;

    public RemoveTagFromPhotoCommandHandler(
        PhotoRepository photoRepository,
        PhotoTagRepository photoTagRepository
    ) {
        this.photoRepository = photoRepository;
        this.photoTagRepository = photoTagRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> handle(RemoveTagFromPhotoCommand command) {
        // 1. Verify photo exists and belongs to user
        return photoRepository.findById(command.photoId())
            .switchIfEmpty(Mono.error(new PhotoNotFoundException("Photo not found: " + command.photoId())))
            .filter(photo -> photo.getUserId().equals(command.userId()))
            .switchIfEmpty(Mono.error(new UnauthorizedException("Photo does not belong to user")))
            .flatMap(photo ->
                // 2. Remove photo-tag relationship
                // Note: Tag entity remains in database (may be used by other photos)
                photoTagRepository.deleteByPhotoIdAndTagId(command.photoId(), command.tagId())
            );
    }

    /**
     * Exception thrown when photo not found.
     */
    public static class PhotoNotFoundException extends RuntimeException {
        public PhotoNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when user tries to modify photo they don't own.
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
