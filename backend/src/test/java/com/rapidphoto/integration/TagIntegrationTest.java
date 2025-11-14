package com.rapidphoto.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for Tag Management (Story 3.3, 3.4)
 * Tests tag creation, reuse, filtering, and limits
 *
 * Story 3.7: Gallery Integration Tests
 */
class TagIntegrationTest extends IntegrationTestBase {

    @Test
    void testAddTagToPhoto_TagAppearsInFilter() {
        UUID photoId = createTestPhoto("photo.jpg", 1024000L);

        // Add tag to photo
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

        // Verify tag appears in user's tag list
        webTestClient.get()
            .uri("/api/tags")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[?(@.name=='vacation')]").exists();

        // Verify photo appears in tag filter results
        String tagId = getTagIdByName("vacation");
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/photos")
                .queryParam("tagIds", tagId)
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].id").isEqualTo(photoId.toString());
    }

    @Test
    void testRemoveTag_PhotoDisappearsFromFilter() {
        UUID photoId = createTestPhoto("photo.jpg", 1024000L);

        // Add tag to photo
        UUID tagId = addTagToPhoto(photoId, "vacation");

        // Verify photo appears in filter
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/photos")
                .queryParam("tagIds", tagId)
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1);

        // Remove tag from photo
        webTestClient.delete()
            .uri("/api/tags/photos/{photoId}/tags/{tagId}", photoId, tagId)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isNoContent();

        // Verify photo no longer appears in filter
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/photos")
                .queryParam("tagIds", tagId)
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void testTagReuse_SameColorForExistingTag() {
        UUID photo1 = createTestPhoto("photo1.jpg", 1024000L);
        UUID photo2 = createTestPhoto("photo2.jpg", 2048000L);

        // Add "vacation" tag to first photo
        String[] tag1Color = new String[1];
        webTestClient.post()
            .uri("/api/tags/photos/{photoId}/tags", photo1)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("name", "vacation"))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.color").value(color -> tag1Color[0] = (String) color);

        // Add "vacation" tag to second photo (should reuse)
        String[] tag2Color = new String[1];
        String[] tag2Id = new String[1];
        webTestClient.post()
            .uri("/api/tags/photos/{photoId}/tags", photo2)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("name", "vacation"))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.color").value(color -> tag2Color[0] = (String) color)
            .jsonPath("$.id").value(id -> tag2Id[0] = (String) id);

        // Verify same color (tag reused, not duplicated)
        assertThat(tag2Color[0]).isEqualTo(tag1Color[0]);
    }

    @Test
    void testMaxTagsEnforcement_Returns400() {
        UUID photoId = createTestPhoto("photo.jpg", 1024000L);

        // Add 10 tags (maximum allowed)
        for (int i = 1; i <= 10; i++) {
            webTestClient.post()
                .uri("/api/tags/photos/{photoId}/tags", photoId)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "tag" + i))
                .exchange()
                .expectStatus().isOk();
        }

        // Attempt to add 11th tag - should fail
        webTestClient.post()
            .uri("/api/tags/photos/{photoId}/tags", photoId)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("name", "tag11"))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").value(containsString("Maximum 10 tags"));
    }

    @Test
    void testMultipleTagFilter_ANDLogic() {
        // Create 3 photos with different tag combinations
        UUID photo1 = createTestPhoto("photo1.jpg", 1024000L);
        UUID photo2 = createTestPhoto("photo2.jpg", 2048000L);
        UUID photo3 = createTestPhoto("photo3.jpg", 3072000L);

        // Photo1: vacation + family
        UUID vacationTagId = addTagToPhoto(photo1, "vacation");
        UUID familyTagId = addTagToPhoto(photo1, "family");

        // Photo2: vacation only
        addTagToPhoto(photo2, "vacation");

        // Photo3: family only
        addTagToPhoto(photo3, "family");

        // Filter by vacation AND family - should only return photo1
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/photos")
                .queryParam("tagIds", vacationTagId, familyTagId)
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].id").isEqualTo(photo1.toString());

        // Filter by vacation only - should return photo1 and photo2
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/photos")
                .queryParam("tagIds", vacationTagId)
                .build())
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void testTagAutocomplete_ReturnsExistingTags() {
        // Create tags by adding them to photos
        UUID photoId = createTestPhoto("photo.jpg", 1024000L);
        addTagToPhoto(photoId, "vacation");
        addTagToPhoto(photoId, "family");
        addTagToPhoto(photoId, "summer");

        // Get all tags for user
        webTestClient.get()
            .uri("/api/tags")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[?(@.name=='vacation')]").exists()
            .jsonPath("$[?(@.name=='family')]").exists()
            .jsonPath("$[?(@.name=='summer')]").exists();
    }

    @Test
    void testTagCaseSensitivity() {
        UUID photo1 = createTestPhoto("photo1.jpg", 1024000L);
        UUID photo2 = createTestPhoto("photo2.jpg", 2048000L);

        // Add "Vacation" (capital V) to photo1
        addTagToPhoto(photo1, "Vacation");

        // Add "vacation" (lowercase) to photo2
        addTagToPhoto(photo2, "vacation");

        // Verify tags are treated as separate (case-sensitive)
        webTestClient.get()
            .uri("/api/tags")
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(2)
            .jsonPath("$[?(@.name=='Vacation')]").exists()
            .jsonPath("$[?(@.name=='vacation')]").exists();
    }

    // Helper methods

    private String getTagIdByName(String tagName) {
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
}
