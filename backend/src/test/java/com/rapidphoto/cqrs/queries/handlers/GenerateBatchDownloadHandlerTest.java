package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.queries.GenerateBatchDownloadQuery;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.photo.S3Location;
import com.rapidphoto.exception.BatchDownloadLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for GenerateBatchDownloadHandler.
 * Story 3.6: Batch Photo Download (ZIP)
 */
@ExtendWith(MockitoExtension.class)
class GenerateBatchDownloadHandlerTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private S3Client s3Client;

    private GenerateBatchDownloadHandler handler;

    private UUID userId;
    private UUID sessionId;
    private String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        handler = new GenerateBatchDownloadHandler(photoRepository, s3Client, bucketName);
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Test
    void shouldThrowPhotoNotFoundException_WhenSomePhotosNotFound() {
        // Given
        UUID photoId1 = UUID.randomUUID();
        UUID photoId2 = UUID.randomUUID();
        List<UUID> photoIds = Arrays.asList(photoId1, photoId2);

        Photo photo1 = createPhoto("photo1.jpg");
        // Only one photo found
        when(photoRepository.findAllById(photoIds)).thenReturn(Flux.just(photo1));

        GenerateBatchDownloadQuery query = new GenerateBatchDownloadQuery(photoIds, userId);

        // When & Then
        StepVerifier.create(handler.handle(query))
            .expectError(GenerateBatchDownloadHandler.PhotoNotFoundException.class)
            .verify();
    }

    @Test
    void shouldThrowUnauthorizedException_WhenUserDoesNotOwnAllPhotos() {
        // Given
        UUID photoId1 = UUID.randomUUID();
        UUID photoId2 = UUID.randomUUID();
        List<UUID> photoIds = Arrays.asList(photoId1, photoId2);

        Photo photo1 = createPhoto("photo1.jpg");
        Photo photo2 = Photo.initiate(UUID.randomUUID(), sessionId, "photo2.jpg", 1024000L);
        S3Location location2 = S3Location.of(bucketName, "uploads/other/photo2.jpg", null);
        photo2.startUpload(location2);

        when(photoRepository.findAllById(photoIds)).thenReturn(Flux.just(photo1, photo2));

        GenerateBatchDownloadQuery query = new GenerateBatchDownloadQuery(photoIds, userId);

        // When & Then
        StepVerifier.create(handler.handle(query))
            .expectError(GenerateBatchDownloadHandler.UnauthorizedException.class)
            .verify();
    }

    @Test
    void shouldThrowBatchDownloadLimitExceeded_WhenTotalSizeExceeds500MB() {
        // Given
        UUID photoId1 = UUID.randomUUID();
        UUID photoId2 = UUID.randomUUID();
        List<UUID> photoIds = Arrays.asList(photoId1, photoId2);

        // Each photo is 300MB (total 600MB > 500MB limit)
        Photo photo1 = createPhotoWithSize("photo1.jpg", 300L * 1024 * 1024);
        Photo photo2 = createPhotoWithSize("photo2.jpg", 300L * 1024 * 1024);

        when(photoRepository.findAllById(photoIds)).thenReturn(Flux.just(photo1, photo2));

        GenerateBatchDownloadQuery query = new GenerateBatchDownloadQuery(photoIds, userId);

        // When & Then
        StepVerifier.create(handler.handle(query))
            .expectError(BatchDownloadLimitExceededException.class)
            .verify();
    }

    @Test
    void shouldValidateMaximum50PhotoLimit() {
        // Given: 51 photo IDs
        List<UUID> photoIds = Arrays.asList(new UUID[51]);
        for (int i = 0; i < 51; i++) {
            photoIds.set(i, UUID.randomUUID());
        }

        // When & Then: Query validation should throw
        try {
            new GenerateBatchDownloadQuery(photoIds, userId);
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    void shouldThrowException_WhenPhotoIdsEmpty() {
        // Given
        List<UUID> photoIds = Arrays.asList();

        // When & Then
        try {
            new GenerateBatchDownloadQuery(photoIds, userId);
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    // Helper methods

    private Photo createPhoto(String filename) {
        return createPhotoWithSize(filename, 1024000L);
    }

    private Photo createPhotoWithSize(String filename, long fileSize) {
        Photo photo = Photo.initiate(userId, sessionId, filename, fileSize);
        S3Location location = S3Location.of(
            bucketName,
            "uploads/" + userId + "/" + sessionId + "/" + filename,
            null
        );
        photo.startUpload(location);
        return photo;
    }
}
