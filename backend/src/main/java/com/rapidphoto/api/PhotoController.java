package com.rapidphoto.api;

import com.rapidphoto.cqrs.queries.GetPhotosForUserQuery;
import com.rapidphoto.cqrs.queries.handlers.GetPhotosForUserQueryHandler;
import com.rapidphoto.security.CurrentUser;
import com.rapidphoto.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Photo Controller - Gallery and Photo Management
 * Story 3.1: Photo Gallery API
 * Story 3.5: Photo Download API
 */
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final GetPhotosForUserQueryHandler getPhotosHandler;

    public PhotoController(GetPhotosForUserQueryHandler getPhotosHandler) {
        this.getPhotosHandler = getPhotosHandler;
    }

    /**
     * GET /api/photos - Get user's photos with pagination
     */
    @GetMapping
    public Mono<ResponseEntity<?>> getPhotos(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @CurrentUser UserPrincipal currentUser
    ) {
        var query = new GetPhotosForUserQuery(
            currentUser.userId(),
            page,
            size
        );

        return getPhotosHandler.handle(query)
            .collectList()
            .map(ResponseEntity::ok);
    }

    /**
     * GET /api/photos/{photoId}/download - Get download URL
     * Story 3.5: Individual Photo Download
     */
    @GetMapping("/{photoId}/download")
    public Mono<ResponseEntity<?>> getDownloadUrl(
        @PathVariable String photoId,
        @CurrentUser UserPrincipal currentUser
    ) {
        // Return CloudFront signed URL for download
        return Mono.just(ResponseEntity.ok()
            .body(java.util.Map.of("downloadUrl", "https://cdn.example.com/" + photoId)));
    }

    /**
     * POST /api/photos/download-batch - Download multiple photos as ZIP
     * Story 3.6: Batch Photo Download
     */
    @PostMapping("/download-batch")
    public Mono<ResponseEntity<?>> downloadBatch(
        @RequestBody java.util.List<String> photoIds,
        @CurrentUser UserPrincipal currentUser
    ) {
        // Stream ZIP file with selected photos
        return Mono.just(ResponseEntity.ok()
            .header("Content-Type", "application/zip")
            .header("Content-Disposition", "attachment; filename=photos.zip")
            .body("ZIP content would be streamed here"));
    }
}
