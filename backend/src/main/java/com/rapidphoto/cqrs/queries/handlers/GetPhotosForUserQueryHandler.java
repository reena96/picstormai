package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.PhotoDTO;
import com.rapidphoto.cqrs.dtos.PhotoWithTagsDTO;
import com.rapidphoto.cqrs.dtos.TagDTO;
import com.rapidphoto.cqrs.queries.GetPhotosForUserQuery;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.tag.TagRepository;
import com.rapidphoto.infrastructure.S3Service;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query handler for GetPhotosForUserQuery.
 * Read-only operation with pagination - returns DTOs with tags, no @Transactional.
 */
@Service
public class GetPhotosForUserQueryHandler {

    private final PhotoRepository photoRepository;
    private final TagRepository tagRepository;
    private final S3Service s3Service;

    public GetPhotosForUserQueryHandler(
        PhotoRepository photoRepository,
        TagRepository tagRepository,
        S3Service s3Service
    ) {
        this.photoRepository = photoRepository;
        this.tagRepository = tagRepository;
        this.s3Service = s3Service;
    }

    /**
     * Handle query to get photos with tags.
     * Returns PhotoWithTagsDTO for gallery display.
     * Story 3.4: Enhanced with tag filtering (AND logic).
     */
    public Flux<PhotoWithTagsDTO> handle(GetPhotosForUserQuery query) {
        // Apply pagination using skip and take
        int skip = query.page() * query.size();

        // Choose query based on whether tags filter is present
        Flux<com.rapidphoto.domain.photo.Photo> photoFlux;
        if (query.tagIds() != null && !query.tagIds().isEmpty()) {
            // Filter by tags (AND logic - photos must have ALL selected tags)
            photoFlux = photoRepository.findByUserIdAndAllTags(
                query.userId(),
                query.tagIds(),
                query.tagIds().size()  // tagCount for HAVING clause
            );
        } else {
            // No filter - return all photos
            photoFlux = photoRepository.findByUserId(query.userId());
        }

        return photoFlux
            .skip(skip)
            .take(query.size())
            .collectList()
            .flatMapMany(photos -> {
                if (photos.isEmpty()) {
                    return Flux.empty();
                }

                // Batch fetch tags for all photos
                List<UUID> photoIds = photos.stream()
                    .map(photo -> photo.getId())
                    .collect(Collectors.toList());

                return tagRepository.findByPhotoIds(photoIds)
                    .collect(Collectors.groupingBy(
                        TagRepository.TagWithPhotoId::photoId,
                        Collectors.mapping(
                            twp -> TagDTO.from(twp.id(), twp.name(), twp.color(), twp.createdAt()),
                            Collectors.toList()
                        )
                    ))
                    .flatMapMany(tagsByPhotoId ->
                        Flux.fromIterable(photos)
                            .flatMap(photo -> {
                                List<TagDTO> tags = tagsByPhotoId.getOrDefault(photo.getId(), List.of());

                                // Generate presigned URL for photo viewing (5 minute expiry)
                                if (photo.getS3Location() != null) {
                                    return s3Service.generatePresignedViewUrl(
                                        photo.getS3Location().getKey(),
                                        Duration.ofMinutes(5)
                                    ).map(presignedUrl -> {
                                        // Create DTO with presigned URL instead of LocalStack URL
                                        return new PhotoWithTagsDTO(
                                            photo.getId(),
                                            photo.getUserId(),
                                            photo.getSessionId(),
                                            photo.getFilename(),
                                            photo.getFileSize(),
                                            photo.getS3Location().getKey(),
                                            presignedUrl,  // Use presigned URL instead of LocalStack URL
                                            photo.getUploadStatus(),
                                            photo.getProgress(),
                                            photo.getMetadata(),
                                            tags,
                                            photo.getCreatedAt(),
                                            photo.getUpdatedAt()
                                        );
                                    });
                                } else {
                                    // No S3 location - return DTO with null storageUrl
                                    return Mono.just(PhotoWithTagsDTO.fromDomain(photo, tags));
                                }
                            })
                    );
            });
    }

    /**
     * Legacy method for backward compatibility.
     * Returns PhotoDTO without tags.
     */
    public Flux<PhotoDTO> handleLegacy(GetPhotosForUserQuery query) {
        int skip = query.page() * query.size();

        return photoRepository.findByUserId(query.userId())
            .skip(skip)
            .take(query.size())
            .map(PhotoDTO::fromDomain);
    }
}
