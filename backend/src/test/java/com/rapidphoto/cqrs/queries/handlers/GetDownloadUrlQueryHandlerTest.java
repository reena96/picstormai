package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.DownloadUrlDTO;
import com.rapidphoto.cqrs.queries.GetDownloadUrlQuery;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.photo.S3Location;
import com.rapidphoto.infrastructure.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for GetDownloadUrlQueryHandler.
 * Story 3.5: Individual Photo Download
 */
@ExtendWith(MockitoExtension.class)
class GetDownloadUrlQueryHandlerTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private S3Service s3Service;

    private GetDownloadUrlQueryHandler handler;

    private UUID userId;
    private UUID sessionId;
    private UUID photoId;
    private Photo photo;

    @BeforeEach
    void setUp() {
        handler = new GetDownloadUrlQueryHandler(photoRepository, s3Service);

        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        photoId = UUID.randomUUID();

        // Create a photo with S3 location
        photo = Photo.initiate(userId, sessionId, "test-photo.jpg", 1024000L);
        S3Location s3Location = S3Location.of(
            "test-bucket",
            "uploads/" + userId + "/" + sessionId + "/test-photo.jpg",
            null
        );
        photo.startUpload(s3Location);
    }

    @Test
    void shouldGeneratePresignedUrl_ForValidPhoto() {
        // Given
        GetDownloadUrlQuery query = new GetDownloadUrlQuery(photoId, userId);
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/uploads/test-photo.jpg?X-Amz-Signature=...";

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(s3Service.generatePresignedDownloadUrl(
            eq(photo.getS3Location().getKey()),
            eq(photo.getFilename()),
            any(Duration.class)
        )).thenReturn(expectedUrl);

        // When
        Mono<DownloadUrlDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.url()).isEqualTo(expectedUrl);
                assertThat(dto.filename()).isEqualTo("test-photo.jpg");
                assertThat(dto.fileSize()).isEqualTo(1024000L);
                assertThat(dto.expiresAt()).isNotNull();

                // Verify expiration is approximately 5 minutes from now
                Instant expiresAt = Instant.parse(dto.expiresAt());
                Instant expectedExpiry = Instant.now().plus(Duration.ofMinutes(5));
                assertThat(expiresAt).isBetween(
                    expectedExpiry.minus(Duration.ofSeconds(5)),
                    expectedExpiry.plus(Duration.ofSeconds(5))
                );
            })
            .verifyComplete();

        // Verify S3Service was called with correct parameters
        verify(s3Service).generatePresignedDownloadUrl(
            eq(photo.getS3Location().getKey()),
            eq("test-photo.jpg"),
            eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void shouldThrowPhotoNotFoundException_WhenPhotoNotFound() {
        // Given
        GetDownloadUrlQuery query = new GetDownloadUrlQuery(photoId, userId);

        when(photoRepository.findById(photoId)).thenReturn(Mono.empty());

        // When
        Mono<DownloadUrlDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof GetDownloadUrlQueryHandler.PhotoNotFoundException &&
                error.getMessage().contains("Photo not found")
            )
            .verify();
    }

    @Test
    void shouldThrowUnauthorizedException_WhenUserDoesNotOwnPhoto() {
        // Given
        UUID differentUserId = UUID.randomUUID();
        GetDownloadUrlQuery query = new GetDownloadUrlQuery(photoId, differentUserId);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));

        // When
        Mono<DownloadUrlDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof GetDownloadUrlQueryHandler.UnauthorizedException &&
                error.getMessage().contains("Photo does not belong to user")
            )
            .verify();
    }

    @Test
    void shouldReturnCorrectMetadata_FilenameAndFileSize() {
        // Given
        Photo photoWithSpecialFilename = Photo.initiate(userId, sessionId, "My Photo (1).jpg", 5242880L);
        S3Location s3Location = S3Location.of(
            "test-bucket",
            "uploads/" + userId + "/" + sessionId + "/photo.jpg",
            null
        );
        photoWithSpecialFilename.startUpload(s3Location);

        GetDownloadUrlQuery query = new GetDownloadUrlQuery(photoId, userId);
        String presignedUrl = "https://example.com/presigned-url";

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photoWithSpecialFilename));
        when(s3Service.generatePresignedDownloadUrl(any(), any(), any())).thenReturn(presignedUrl);

        // When
        Mono<DownloadUrlDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.filename()).isEqualTo("My Photo (1).jpg");
                assertThat(dto.fileSize()).isEqualTo(5242880L);
            })
            .verifyComplete();
    }

    @Test
    void shouldHandlePhotosWithSpecialCharactersInFilename() {
        // Given
        Photo photoWithSpecialChars = Photo.initiate(userId, sessionId, "Café & Restaurant #1.jpg", 2048L);
        S3Location s3Location = S3Location.of(
            "test-bucket",
            "uploads/" + userId + "/" + sessionId + "/photo.jpg",
            null
        );
        photoWithSpecialChars.startUpload(s3Location);

        GetDownloadUrlQuery query = new GetDownloadUrlQuery(photoId, userId);
        String presignedUrl = "https://example.com/presigned-url";

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photoWithSpecialChars));
        when(s3Service.generatePresignedDownloadUrl(any(), eq("Café & Restaurant #1.jpg"), any()))
            .thenReturn(presignedUrl);

        // When
        Mono<DownloadUrlDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.filename()).isEqualTo("Café & Restaurant #1.jpg");
                assertThat(dto.url()).isEqualTo(presignedUrl);
            })
            .verifyComplete();

        // Verify S3Service received the filename with special characters
        verify(s3Service).generatePresignedDownloadUrl(
            any(),
            eq("Café & Restaurant #1.jpg"),
            any()
        );
    }

    @Test
    void shouldThrowException_WhenPhotoHasNoS3Location() {
        // Given
        Photo photoWithoutS3Key = Photo.initiate(userId, sessionId, "no-s3-key.jpg", 1024L);
        // Don't call startUpload, so s3Location remains null

        GetDownloadUrlQuery query = new GetDownloadUrlQuery(photoId, userId);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photoWithoutS3Key));

        // When
        Mono<DownloadUrlDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof IllegalStateException &&
                error.getMessage().contains("Photo has no S3 location")
            )
            .verify();
    }

    @Test
    void shouldGenerateUrlWithFiveMinuteExpiration() {
        // Given
        GetDownloadUrlQuery query = new GetDownloadUrlQuery(photoId, userId);
        String presignedUrl = "https://example.com/presigned-url";

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(s3Service.generatePresignedDownloadUrl(any(), any(), any())).thenReturn(presignedUrl);

        // When
        Mono<DownloadUrlDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                Instant expiresAt = Instant.parse(dto.expiresAt());
                Instant now = Instant.now();
                Instant fiveMinutesLater = now.plus(Duration.ofMinutes(5));

                // Verify expiration is within 5 minutes (with 10 second tolerance)
                assertThat(expiresAt).isBetween(
                    fiveMinutesLater.minus(Duration.ofSeconds(10)),
                    fiveMinutesLater.plus(Duration.ofSeconds(10))
                );
            })
            .verifyComplete();

        // Verify S3Service was called with 5-minute duration
        verify(s3Service).generatePresignedDownloadUrl(
            any(),
            any(),
            eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void shouldIncludeOriginalFilenameInPresignedUrlRequest() {
        // Given
        GetDownloadUrlQuery query = new GetDownloadUrlQuery(photoId, userId);
        String presignedUrl = "https://example.com/presigned-url";

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(s3Service.generatePresignedDownloadUrl(any(), any(), any())).thenReturn(presignedUrl);

        // When
        handler.handle(query).block();

        // Then - Verify S3Service received the original filename
        verify(s3Service).generatePresignedDownloadUrl(
            eq(photo.getS3Location().getKey()),
            eq(photo.getFilename()),
            any()
        );
    }
}
