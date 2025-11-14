package com.rapidphoto.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for Gallery features (Stories 3.1-3.6)
 * Tests complete photo lifecycle: Upload → View → Tag → Filter → Download
 *
 * Story 3.7: Gallery Integration Tests
 */
class GalleryIntegrationTest extends IntegrationTestBase {

    @Test
    void testCompletePhotoLifecycle_UploadToDownload() {
        // 1. Create (simulate upload) a photo
        UUID photoId = createTestPhoto("vacation.jpg", 1024000L);

        // 2. Verify photo appears in gallery
        webTestClient.get()
            .uri("/api/photos")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$").isArray()
            .jsonPath("$[0].id").isEqualTo(photoId.toString())
            .jsonPath("$[0].originalFilename").isEqualTo("vacation.jpg")
            .jsonPath("$[0].fileSize").isEqualTo(1024000)
            .jsonPath("$[0].uploadStatus").isEqualTo("COMPLETED");

        // 3. Add tag to photo
        webTestClient.post()
            .uri("/api/tags/photos/{photoId}/tags", photoId)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("name", "vacation"))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.name").isEqualTo("vacation")
            .jsonPath("$.color").exists();

        // 4. Filter by tag - photo should appear
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/photos")
                .queryParam("tagIds", getTagIdByName("vacation"))
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].id").isEqualTo(photoId.toString())
            .jsonPath("$[0].tags[0].name").isEqualTo("vacation");

        // 5. Download photo - generate presigned URL
        webTestClient.get()
            .uri("/api/photos/{photoId}/download", photoId)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.url").exists()
            .jsonPath("$.url").value(containsString("presigned=true"))
            .jsonPath("$.filename").isEqualTo("vacation.jpg")
            .jsonPath("$.fileSize").isEqualTo(1024000)
            .jsonPath("$.expiresAt").exists();
    }

    @Test
    void testBatchDownload_CreatesZipWithAllPhotos() throws IOException {
        // Upload 3 test photos
        List<UUID> photoIds = List.of(
            createTestPhoto("photo1.jpg", 100000L),
            createTestPhoto("photo2.jpg", 200000L),
            createTestPhoto("photo3.jpg", 300000L)
        );

        // Download as ZIP
        byte[] zipBytes = webTestClient.post()
            .uri("/api/photos/download-batch")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("photoIds", photoIds))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("application/zip")
            .expectHeader().exists("Content-Disposition")
            .returnResult(byte[].class)
            .getResponseBodyContent();

        // Verify ZIP contents
        assertThat(zipBytes).isNotNull();
        Set<String> filenames = extractFilenamesFromZip(zipBytes);
        assertThat(filenames).containsExactlyInAnyOrder("photo1.jpg", "photo2.jpg", "photo3.jpg");
    }

    @Test
    void testBatchDownload_Enforces50PhotoLimit() {
        // Create 51 photo IDs
        List<UUID> photoIds = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            photoIds.add(UUID.randomUUID());
        }

        // Attempt batch download - should fail
        webTestClient.post()
            .uri("/api/photos/download-batch")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("photoIds", photoIds))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").value(containsString("Maximum 50 photos"));
    }

    @Test
    void testBatchDownload_WithDuplicateFilenames() throws IOException {
        // Upload 3 photos with same filename
        UUID photo1 = createTestPhoto("photo.jpg", 100000L);
        UUID photo2 = createTestPhoto("photo.jpg", 200000L);
        UUID photo3 = createTestPhoto("photo.jpg", 300000L);

        List<UUID> photoIds = List.of(photo1, photo2, photo3);

        // Download as ZIP
        byte[] zipBytes = webTestClient.post()
            .uri("/api/photos/download-batch")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("photoIds", photoIds))
            .exchange()
            .expectStatus().isOk()
            .returnResult(byte[].class)
            .getResponseBodyContent();

        // Verify ZIP contains files with unique names
        Set<String> filenames = extractFilenamesFromZip(zipBytes);
        assertThat(filenames).hasSize(3);
        assertThat(filenames).contains("photo.jpg");
        // Other files should have suffixes like photo-1.jpg, photo-2.jpg
        assertThat(filenames.stream().filter(f -> f.startsWith("photo")).count()).isEqualTo(3);
    }

    @Test
    void testGalleryPagination_WithInfiniteScroll() {
        // Upload 35 photos
        for (int i = 1; i <= 35; i++) {
            createTestPhoto("photo" + i + ".jpg", 1000000L);
        }

        // Fetch first page (30 photos)
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/photos")
                .queryParam("page", 0)
                .queryParam("size", 30)
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(30);

        // Fetch second page (remaining 5 photos)
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/photos")
                .queryParam("page", 1)
                .queryParam("size", 30)
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(5);
    }

    @Test
    void testPhotoOrdering_ByUploadDate() {
        // Create photos with small delays to ensure different timestamps
        UUID photo1 = createTestPhoto("old.jpg", 100000L);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        UUID photo2 = createTestPhoto("recent.jpg", 200000L);

        // Fetch photos (default sort: createdAt,desc - newest first)
        webTestClient.get()
            .uri("/api/photos")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(photo2.toString()) // Most recent first
            .jsonPath("$[1].id").isEqualTo(photo1.toString());
    }

    @Test
    void testUnauthorizedAccess_ReturnsError() {
        // Create photo for test user
        UUID photoId = createTestPhoto("private.jpg", 100000L);

        // Try to access without authorization
        webTestClient.get()
            .uri("/api/photos/{photoId}/download", photoId)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void testPhotoNotFound_Returns404() {
        UUID nonExistentPhotoId = UUID.randomUUID();

        webTestClient.get()
            .uri("/api/photos/{photoId}/download", nonExistentPhotoId)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void testMultipleUsersIsolation() {
        // Create photo for test user
        UUID testUserPhotoId = createTestPhoto("my-photo.jpg", 100000L);

        // Create another user
        UUID otherUserId = UUID.randomUUID();
        String otherUserToken = jwtUtil.generateAccessToken(otherUserId, "other@example.com");

        // Test user should see their photo
        webTestClient.get()
            .uri("/api/photos")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].id").isEqualTo(testUserPhotoId.toString());

        // Other user should not see test user's photos
        webTestClient.get()
            .uri("/api/photos")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void testBatchDownload_EmptyPhotoIds_Returns400() {
        webTestClient.post()
            .uri("/api/photos/download-batch")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("photoIds", List.of()))
            .exchange()
            .expectStatus().isBadRequest();
    }

    // Helper methods

    private String getTagIdByName(String tagName) {
        // Query tags to find ID by name
        String[] tagId = new String[1];
        webTestClient.get()
            .uri("/api/tags")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[?(@.name=='" + tagName + "')].id")
            .value(ids -> {
                if (ids instanceof List && !((List<?>) ids).isEmpty()) {
                    tagId[0] = ((List<?>) ids).get(0).toString();
                }
            });
        return tagId[0];
    }

    private Set<String> extractFilenamesFromZip(byte[] zipBytes) throws IOException {
        Set<String> filenames = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                filenames.add(entry.getName());
                zipInputStream.closeEntry();
            }
        }
        return filenames;
    }
}
