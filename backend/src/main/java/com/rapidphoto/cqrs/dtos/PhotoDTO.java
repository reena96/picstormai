package com.rapidphoto.cqrs.dtos;

import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoStatus;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for Photo data.
 * Never expose domain entities directly - always use DTOs.
 */
public record PhotoDTO(
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
    Set<UUID> tagIds,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Create DTO from domain entity.
     */
    public static PhotoDTO fromDomain(Photo photo) {
        return new PhotoDTO(
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
            photo.getTagIds(),
            photo.getCreatedAt(),
            photo.getUpdatedAt()
        );
    }
}
