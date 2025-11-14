package com.rapidphoto.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for Download features (Story 3.5, 3.6)
 * Tests individual download and batch ZIP download
 *
 * Story 3.7: Gallery Integration Tests
 */
class DownloadIntegrationTest extends IntegrationTestBase {

    @Test
    void testIndividualDownload_EndToEnd() {
        UUID photoId = createTestPhoto("vacation.jpg", 1024000L);

        // Request download URL
        String[] downloadUrl = new String[1];
        String[] expiresAt = new String[1];

        webTestClient.get()
            .uri("/api/photos/{photoId}/download", photoId)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.url").exists()
            .jsonPath("$.url").value(url -> downloadUrl[0] = (String) url)
            .jsonPath("$.filename").isEqualTo("vacation.jpg")
            .jsonPath("$.fileSize").isEqualTo(1024000)
            .jsonPath("$.expiresAt").exists()
            .jsonPath("$.expiresAt").value(expires -> expiresAt[0] = (String) expires);

        // Verify URL is presigned
        assertThat(downloadUrl[0]).contains("presigned=true");

        // Verify expiration is approximately 5 minutes from now
        Instant expiresAtTime = Instant.parse(expiresAt[0]);
        Instant now = Instant.now();
        long minutesUntilExpiration = now.until(expiresAtTime, ChronoUnit.MINUTES);
        assertThat(minutesUntilExpiration).isBetween(4L, 6L); // 5 minutes +/- 1 minute
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
            .expectHeader().contentType("application/zip")
            .returnResult(byte[].class)
            .getResponseBodyContent();

        // Verify ZIP contains files with unique names
        Set<String> filenames = extractFilenamesFromZip(zipBytes);
        assertThat(filenames).hasSize(3);
        assertThat(filenames).contains("photo.jpg");
        // Other files should have suffixes to make them unique
        assertThat(filenames.stream().filter(f -> f.startsWith("photo")).count()).isEqualTo(3);
    }

    @Test
    void testDownload_UnauthorizedUser_Returns403() {
        // Create photo for test user
        UUID photoId = createTestPhoto("private.jpg", 1024000L);

        // Create another user
        UUID otherUserId = UUID.randomUUID();
        String otherUserToken = jwtUtil.generateAccessToken(otherUserId, "other@example.com");

        // Other user tries to download test user's photo
        webTestClient.get()
            .uri("/api/photos/{photoId}/download", photoId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void testDownload_PhotoNotFound_Returns404() {
        UUID nonExistentPhotoId = UUID.randomUUID();

        webTestClient.get()
            .uri("/api/photos/{photoId}/download", nonExistentPhotoId)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isNotFound();
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

    @Test
    void testBatchDownload_MixedOwnership_OnlyDownloadsOwnedPhotos() throws IOException {
        // Create photos for test user
        UUID ownedPhoto1 = createTestPhoto("owned1.jpg", 100000L);
        UUID ownedPhoto2 = createTestPhoto("owned2.jpg", 200000L);

        // Create photo for another user
        UUID otherUserId = UUID.randomUUID();
        UUID originalUserId = testUserId;
        testUserId = otherUserId; // Temporarily switch user
        UUID otherUserPhoto = createTestPhoto("other.jpg", 300000L);
        testUserId = originalUserId; // Switch back

        // Try to download all photos (including one not owned)
        List<UUID> photoIds = List.of(ownedPhoto1, ownedPhoto2, otherUserPhoto);

        // Should return 403 or only download owned photos
        webTestClient.post()
            .uri("/api/photos/download-batch")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("photoIds", photoIds))
            .exchange()
            .expectStatus().isForbidden(); // Expected: authorization error for mixed ownership
    }

    @Test
    void testBatchDownload_ValidatesFileSize() {
        // Create photos with large file sizes (simulated)
        List<UUID> photoIds = new ArrayList<>();

        // Create 10 photos, each 60MB (total 600MB > 500MB limit)
        for (int i = 0; i < 10; i++) {
            photoIds.add(createTestPhoto("large" + i + ".jpg", 60_000_000L));
        }

        // Attempt batch download - should fail due to size limit
        webTestClient.post()
            .uri("/api/photos/download-batch")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("photoIds", photoIds))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").value(containsString("500MB"));
    }

    @Test
    void testIndividualDownload_NoAuthToken_Returns401() {
        UUID photoId = createTestPhoto("photo.jpg", 1024000L);

        // Try to download without authentication
        webTestClient.get()
            .uri("/api/photos/{photoId}/download", photoId)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    // Helper methods

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
