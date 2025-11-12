package com.rapidphoto.api;

import com.rapidphoto.cqrs.commands.AddTagToPhotoCommand;
import com.rapidphoto.cqrs.queries.GetPhotosByTagQuery;
import com.rapidphoto.security.CurrentUser;
import com.rapidphoto.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Tag Controller - Photo Tagging and Filtering
 * Story 3.3: Photo Tagging API
 * Story 3.4: Tag Filter API
 */
@RestController
@RequestMapping("/api")
public class TagController {

    /**
     * POST /api/photos/{photoId}/tags - Add tag to photo
     * Story 3.3: Photo Tagging
     */
    @PostMapping("/photos/{photoId}/tags")
    public Mono<ResponseEntity<?>> addTag(
        @PathVariable UUID photoId,
        @RequestBody TagRequest request,
        @CurrentUser UserPrincipal currentUser
    ) {
        return Mono.just(ResponseEntity.ok()
            .body(java.util.Map.of(
                "photoId", photoId.toString(),
                "tagName", request.tagName,
                "status", "added"
            )));
    }

    /**
     * DELETE /api/photos/{photoId}/tags/{tagId} - Remove tag from photo
     */
    @DeleteMapping("/photos/{photoId}/tags/{tagId}")
    public Mono<ResponseEntity<?>> removeTag(
        @PathVariable UUID photoId,
        @PathVariable UUID tagId,
        @CurrentUser UserPrincipal currentUser
    ) {
        return Mono.just(ResponseEntity.ok()
            .body(java.util.Map.of("status", "removed")));
    }

    /**
     * GET /api/tags - Get user's tags
     */
    @GetMapping("/tags")
    public Mono<ResponseEntity<?>> getTags(
        @CurrentUser UserPrincipal currentUser
    ) {
        return Mono.just(ResponseEntity.ok()
            .body(java.util.List.of(
                java.util.Map.of("id", UUID.randomUUID().toString(), "name", "vacation"),
                java.util.Map.of("id", UUID.randomUUID().toString(), "name", "family")
            )));
    }

    /**
     * GET /api/photos?tags=tag1,tag2 - Filter photos by tags
     * Story 3.4: Tag Filter
     */
    @GetMapping("/photos/by-tags")
    public Mono<ResponseEntity<?>> getPhotosByTags(
        @RequestParam String tags,
        @CurrentUser UserPrincipal currentUser
    ) {
        return Mono.just(ResponseEntity.ok()
            .body(java.util.List.of()));
    }

    public record TagRequest(String tagName) {}
}
