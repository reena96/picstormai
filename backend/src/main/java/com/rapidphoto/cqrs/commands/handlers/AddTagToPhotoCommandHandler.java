package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.AddTagToPhotoCommand;
import com.rapidphoto.cqrs.dtos.TagDTO;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.tag.PhotoTag;
import com.rapidphoto.domain.tag.PhotoTagRepository;
import com.rapidphoto.domain.tag.Tag;
import com.rapidphoto.domain.tag.TagRepository;
import com.rapidphoto.util.ColorPalette;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for AddTagToPhotoCommand.
 * Implements find-or-create tag logic with max 10 tags enforcement.
 */
@Service
public class AddTagToPhotoCommandHandler {

    private final PhotoRepository photoRepository;
    private final TagRepository tagRepository;
    private final PhotoTagRepository photoTagRepository;

    public AddTagToPhotoCommandHandler(
        PhotoRepository photoRepository,
        TagRepository tagRepository,
        PhotoTagRepository photoTagRepository
    ) {
        this.photoRepository = photoRepository;
        this.tagRepository = tagRepository;
        this.photoTagRepository = photoTagRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<TagDTO> handle(AddTagToPhotoCommand command) {
        // 1. Verify photo exists and belongs to user
        return photoRepository.findById(command.photoId())
            .switchIfEmpty(Mono.error(new PhotoNotFoundException("Photo not found: " + command.photoId())))
            .filter(photo -> photo.getUserId().equals(command.userId()))
            .switchIfEmpty(Mono.error(new UnauthorizedException("Photo does not belong to user")))
            .flatMap(photo ->
                // 2. Check tag limit (max 10 tags per photo)
                photoTagRepository.countByPhotoId(command.photoId())
                    .flatMap(count -> {
                        if (count >= 10) {
                            return Mono.error(new MaxTagsExceededException("Maximum 10 tags per photo"));
                        }
                        // 3. Find or create tag
                        return findOrCreateTag(command.userId(), command.tagName());
                    })
            )
            .flatMap(tag ->
                // 4. Check if photo-tag relationship already exists
                photoTagRepository.existsByPhotoIdAndTagId(command.photoId(), tag.getId())
                    .flatMap(exists -> {
                        if (exists) {
                            // Tag already associated with photo, just return it
                            return Mono.just(TagDTO.fromDomain(tag));
                        }
                        // 5. Create photo-tag relationship
                        return photoTagRepository.save(PhotoTag.create(command.photoId(), tag.getId()))
                            .thenReturn(TagDTO.fromDomain(tag));
                    })
            );
    }

    /**
     * Find existing tag or create new one.
     */
    private Mono<Tag> findOrCreateTag(UUID userId, String tagName) {
        String trimmedName = tagName.trim();
        return tagRepository.findByUserIdAndName(userId, trimmedName)
            .switchIfEmpty(createNewTag(userId, trimmedName));
    }

    /**
     * Create new tag with random color.
     */
    private Mono<Tag> createNewTag(UUID userId, String tagName) {
        String color = ColorPalette.getRandomColor();
        Tag tag = Tag.create(userId, tagName, color);
        return tagRepository.save(tag);
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
     * Exception thrown when max tags limit exceeded.
     */
    public static class MaxTagsExceededException extends RuntimeException {
        public MaxTagsExceededException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when user tries to tag photo they don't own.
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
