package com.rapidphoto.cqrs.dtos;

import com.rapidphoto.domain.upload.SessionStatus;
import com.rapidphoto.domain.upload.UploadSession;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for UploadSession data.
 * Never expose domain entities directly - always use DTOs.
 */
public record UploadSessionDTO(
    UUID id,
    UUID userId,
    SessionStatus status,
    int totalPhotos,
    int completedPhotos,
    int failedPhotos,
    int progressPercentage,
    Instant createdAt,
    Instant completedAt
) {
    /**
     * Create DTO from domain entity.
     */
    public static UploadSessionDTO fromDomain(UploadSession session) {
        return new UploadSessionDTO(
            session.getId(),
            session.getUserId(),
            session.getStatus(),
            session.getTotalPhotos(),
            session.getCompletedPhotos(),
            session.getFailedPhotos(),
            session.getProgressPercentage(),
            session.getCreatedAt(),
            session.getCompletedAt()
        );
    }
}
