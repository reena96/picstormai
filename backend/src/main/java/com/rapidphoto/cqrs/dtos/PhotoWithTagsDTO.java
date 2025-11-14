package com.rapidphoto.cqrs.dtos;

import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Photo data with tags.
 * Used for gallery views where tags need to be displayed.
 */
public record PhotoWithTagsDTO(
    UUID id,
    UUID userId,
    UUID sessionId,
    String filename,
    long fileSize,
    String s3Key,
    String storageUrl,
    PhotoStatus uploadStatus,
    int progress,
    Map<String, Object> metadata,
    List<TagDTO> tags,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Create DTO from domain entity and tags.
     */
    public static PhotoWithTagsDTO fromDomain(Photo photo, List<TagDTO> tags) {
        return new PhotoWithTagsDTO(
            photo.getId(),
            photo.getUserId(),
            photo.getSessionId(),
            photo.getFilename(),
            photo.getFileSize(),
            photo.getS3Location() != null ? photo.getS3Location().getKey() : null,
            photo.getS3Location() != null ? photo.getS3Location().toUri() : null,
            photo.getUploadStatus(),
            photo.getProgress(),
            photo.getMetadata(),
            tags != null ? tags : List.of(),
            photo.getCreatedAt(),
            photo.getUpdatedAt()
        );
    }

    /**
     * Create DTO from PhotoDTO and tags.
     */
    public static PhotoWithTagsDTO fromPhotoDTO(PhotoDTO photoDTO, List<TagDTO> tags) {
        return new PhotoWithTagsDTO(
            photoDTO.id(),
            photoDTO.userId(),
            photoDTO.sessionId(),
            photoDTO.filename(),
            photoDTO.fileSize(),
            photoDTO.s3Key(),
            photoDTO.storageUrl(),
            photoDTO.uploadStatus(),
            photoDTO.progress(),
            photoDTO.metadata(),
            tags != null ? tags : List.of(),
            photoDTO.createdAt(),
            photoDTO.updatedAt()
        );
    }
}
