package com.rapidphoto.integration;

import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.photo.PhotoStatus;
import com.rapidphoto.domain.tag.Tag;
import com.rapidphoto.domain.tag.TagRepository;
import com.rapidphoto.infrastructure.S3Service;
import com.rapidphoto.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Base class for integration tests.
 * Provides:
 * - Spring Boot application context
 * - WebTestClient for reactive endpoint testing
 * - Test user authentication
 * - Database cleanup between tests
 * - Mocked S3 service
 *
 * Story 3.7: Gallery Integration Tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public abstract class IntegrationTestBase {

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected PhotoRepository photoRepository;

    @Autowired
    protected TagRepository tagRepository;

    @Autowired
    protected JwtUtil jwtUtil;

    @MockBean
    protected S3Service s3Service;

    protected String authToken;
    protected UUID testUserId;
    protected List<UUID> createdPhotoIds;
    protected List<UUID> createdTagIds;

    @BeforeEach
    void baseSetUp() {
        // Create test user ID
        testUserId = UUID.randomUUID();

        // Generate JWT token for test user
        authToken = jwtUtil.generateAccessToken(testUserId, "test@example.com");

        // Initialize tracking lists
        createdPhotoIds = new ArrayList<>();
        createdTagIds = new ArrayList<>();

        // Clean up test data
        photoRepository.deleteAll().block();
        tagRepository.deleteAll().block();

        // Mock S3 service responses
        mockS3Responses();
    }

    /**
     * Mock S3 service to return fake URLs and avoid actual S3 calls
     */
    protected void mockS3Responses() {
        // Mock presigned download URL generation
        when(s3Service.generatePresignedDownloadUrl(anyString(), anyString(), any(Duration.class)))
            .thenAnswer(invocation -> {
                String s3Key = invocation.getArgument(0);
                return "https://test-bucket.s3.amazonaws.com/" + s3Key + "?presigned=true";
            });
    }

    /**
     * Create a test photo in the database
     * Uses reflection to bypass domain validation for test purposes
     */
    protected UUID createTestPhoto(String filename, long fileSize) {
        // Create photo using factory method
        Photo photo = Photo.initiate(testUserId, UUID.randomUUID(), filename, fileSize);

        // Complete the upload using business methods
        String s3Key = "photos/" + testUserId + "/" + photo.getId() + "/" + filename;
        String storageUrl = "https://test-bucket.s3.amazonaws.com/" + s3Key;

        // Use reflection to set s3Key and storageUrl since startUpload needs S3Location
        try {
            java.lang.reflect.Field s3KeyField = Photo.class.getDeclaredField("s3Key");
            s3KeyField.setAccessible(true);
            s3KeyField.set(photo, s3Key);

            java.lang.reflect.Field storageUrlField = Photo.class.getDeclaredField("storageUrl");
            storageUrlField.setAccessible(true);
            storageUrlField.set(photo, storageUrl);

            java.lang.reflect.Field statusField = Photo.class.getDeclaredField("uploadStatus");
            statusField.setAccessible(true);
            statusField.set(photo, PhotoStatus.COMPLETED);

            java.lang.reflect.Field progressField = Photo.class.getDeclaredField("progress");
            progressField.setAccessible(true);
            progressField.set(photo, 100);
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup test photo", e);
        }

        Photo saved = photoRepository.save(photo).block();
        createdPhotoIds.add(saved.getId());
        return saved.getId();
    }

    /**
     * Create multiple test photos
     */
    protected List<UUID> createTestPhotos(int count) {
        List<UUID> photoIds = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            UUID photoId = createTestPhoto("photo" + i + ".jpg", 1024000L + (i * 100000L));
            photoIds.add(photoId);
        }
        return photoIds;
    }

    /**
     * Create a test tag
     */
    protected UUID createTestTag(String name, String color) {
        Tag tag = Tag.create(testUserId, name, color);
        Tag saved = tagRepository.save(tag).block();
        createdTagIds.add(saved.getId());
        return saved.getId();
    }

    /**
     * Add tag to photo via API
     */
    protected UUID addTagToPhoto(UUID photoId, String tagName) {
        String[] tagId = new String[1];

        webTestClient.post()
            .uri("/api/tags/photos/{photoId}/tags", photoId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
            .bodyValue(new AddTagRequest(tagName))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").value(id -> tagId[0] = (String) id);

        return UUID.fromString(tagId[0]);
    }

    /**
     * Helper method to get Authorization header
     */
    protected String authHeader() {
        return "Bearer " + authToken;
    }

    /**
     * Request DTO for adding tags
     */
    protected record AddTagRequest(String name) {}
}
