package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.DownloadUrlDTO;
import com.rapidphoto.cqrs.queries.GetDownloadUrlQuery;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.infrastructure.S3Service;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Query handler for GetDownloadUrlQuery.
 * Story 3.5: Individual Photo Download
 */
@Service
public class GetDownloadUrlQueryHandler {

    private final PhotoRepository photoRepository;
    private final S3Service s3Service;

    public GetDownloadUrlQueryHandler(
        PhotoRepository photoRepository,
        S3Service s3Service
    ) {
        this.photoRepository = photoRepository;
        this.s3Service = s3Service;
    }

    /**
     * Handle query to generate presigned download URL.
     * Verifies photo ownership before generating URL.
     *
     * @param query Query containing photoId and userId
     * @return Mono of DownloadUrlDTO with presigned URL
     */
    public Mono<DownloadUrlDTO> handle(GetDownloadUrlQuery query) {
        return photoRepository.findById(query.photoId())
            .switchIfEmpty(Mono.error(new PhotoNotFoundException("Photo not found: " + query.photoId())))
            .filter(photo -> photo.getUserId().equals(query.userId()))
            .switchIfEmpty(Mono.error(new UnauthorizedException("Photo does not belong to user")))
            .map(photo -> {
                // Extract S3 key from photo location
                var s3Location = photo.getS3Location();
                if (s3Location == null) {
                    throw new IllegalStateException("Photo has no S3 location");
                }

                String s3Key = s3Location.getKey();
                if (s3Key == null || s3Key.isEmpty()) {
                    throw new IllegalStateException("Photo has no S3 key");
                }

                // Generate presigned URL with 5-minute expiration
                Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
                String presignedUrl = s3Service.generatePresignedDownloadUrl(
                    s3Key,
                    photo.getFilename(),
                    Duration.ofMinutes(5)
                );

                // Return DTO with download information
                return DownloadUrlDTO.create(
                    presignedUrl,
                    photo.getFilename(),
                    photo.getFileSize(),
                    expiresAt
                );
            });
    }

    /**
     * Exception thrown when photo is not found.
     */
    public static class PhotoNotFoundException extends RuntimeException {
        public PhotoNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when user is not authorized to download photo.
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
