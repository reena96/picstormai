package com.rapidphoto.api;

import com.rapidphoto.cqrs.commands.DeletePhotoCommand;
import com.rapidphoto.cqrs.commands.MarkPhotoUploadedCommand;
import com.rapidphoto.cqrs.commands.handlers.DeletePhotoCommandHandler;
import com.rapidphoto.cqrs.commands.handlers.MarkPhotoUploadedCommandHandler;
import com.rapidphoto.cqrs.dtos.DownloadUrlDTO;
import com.rapidphoto.cqrs.queries.GenerateBatchDownloadQuery;
import com.rapidphoto.cqrs.queries.GetDownloadUrlQuery;
import com.rapidphoto.cqrs.queries.GetPhotosForUserQuery;
import com.rapidphoto.cqrs.queries.handlers.GenerateBatchDownloadHandler;
import com.rapidphoto.cqrs.queries.handlers.GetDownloadUrlQueryHandler;
import com.rapidphoto.cqrs.queries.handlers.GetPhotosForUserQueryHandler;
import com.rapidphoto.exception.BatchDownloadLimitExceededException;
import com.rapidphoto.security.CurrentUser;
import com.rapidphoto.security.UserPrincipal;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Photo Controller - Gallery and Photo Management
 * Story 3.1: Photo Gallery API
 * Story 3.4: Tag Filter & Search - Added tagIds parameter
 * Story 3.5: Photo Download API
 */
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final GetPhotosForUserQueryHandler getPhotosHandler;
    private final GetDownloadUrlQueryHandler getDownloadUrlHandler;
    private final GenerateBatchDownloadHandler generateBatchDownloadHandler;
    private final MarkPhotoUploadedCommandHandler markPhotoUploadedHandler;
    private final DeletePhotoCommandHandler deletePhotoHandler;

    public PhotoController(
        GetPhotosForUserQueryHandler getPhotosHandler,
        GetDownloadUrlQueryHandler getDownloadUrlHandler,
        GenerateBatchDownloadHandler generateBatchDownloadHandler,
        MarkPhotoUploadedCommandHandler markPhotoUploadedHandler,
        DeletePhotoCommandHandler deletePhotoHandler
    ) {
        this.getPhotosHandler = getPhotosHandler;
        this.getDownloadUrlHandler = getDownloadUrlHandler;
        this.generateBatchDownloadHandler = generateBatchDownloadHandler;
        this.markPhotoUploadedHandler = markPhotoUploadedHandler;
        this.deletePhotoHandler = deletePhotoHandler;
    }

    /**
     * GET /api/photos - Get user's photos with pagination and optional tag filtering
     * Story 3.4: Added tagIds query parameter for filtering by tags (AND logic)
     *
     * @param page Current page (0-indexed)
     * @param size Page size (max 100)
     * @param sort Sort field and direction (e.g., "createdAt,desc")
     * @param tagIds Optional list of tag IDs to filter by (AND logic - photos must have ALL tags)
     * @param currentUser Current authenticated user
     * @return List of photos with tags
     */
    @GetMapping
    public Mono<ResponseEntity<?>> getPhotos(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @RequestParam(required = false) List<String> tagIds,
        @CurrentUser UserPrincipal currentUser
    ) {
        // Convert String UUIDs to UUID list
        List<UUID> tagUuids = null;
        if (tagIds != null && !tagIds.isEmpty()) {
            try {
                tagUuids = tagIds.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                return Mono.just(ResponseEntity.badRequest()
                    .body("Invalid tag ID format"));
            }
        }

        var query = new GetPhotosForUserQuery(
            currentUser.userId(),
            page,
            size,
            tagUuids
        );

        return getPhotosHandler.handle(query)
            .collectList()
            .map(ResponseEntity::ok);
    }

    /**
     * POST /api/photos/{photoId}/uploaded - Mark photo as uploaded
     * Called by frontend after successful S3 upload
     */
    @PostMapping("/{photoId}/uploaded")
    public Mono<ResponseEntity<Void>> markPhotoUploaded(
        @PathVariable String photoId,
        @RequestBody MarkUploadedRequest request,
        @CurrentUser UserPrincipal currentUser
    ) {
        try {
            UUID photoUuid = UUID.fromString(photoId);
            MarkPhotoUploadedCommand command = new MarkPhotoUploadedCommand(
                photoUuid,
                request.s3Key(),
                currentUser.userId()
            );

            return markPhotoUploadedHandler.handle(command)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(IllegalArgumentException.class, e ->
                    Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build())
                );
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    public record MarkUploadedRequest(String s3Key) {}

    /**
     * GET /api/photos/{photoId}/download - Get download URL
     * Story 3.5: Individual Photo Download
     *
     * @param photoId Photo UUID
     * @param currentUser Current authenticated user
     * @return DownloadUrlDTO with presigned URL, filename, fileSize, and expiration
     */
    @GetMapping("/{photoId}/download")
    public Mono<ResponseEntity<DownloadUrlDTO>> getDownloadUrl(
        @PathVariable String photoId,
        @CurrentUser UserPrincipal currentUser
    ) {
        try {
            UUID photoUuid = UUID.fromString(photoId);
            GetDownloadUrlQuery query = new GetDownloadUrlQuery(photoUuid, currentUser.userId());

            return getDownloadUrlHandler.handle(query)
                .map(ResponseEntity::ok)
                .onErrorResume(GetDownloadUrlQueryHandler.PhotoNotFoundException.class, e ->
                    Mono.just(ResponseEntity.notFound().build())
                )
                .onErrorResume(GetDownloadUrlQueryHandler.UnauthorizedException.class, e ->
                    Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
                );
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * POST /api/photos/download-batch - Download multiple photos as ZIP
     * Story 3.6: Batch Photo Download
     *
     * @param request Batch download request with photo IDs
     * @param currentUser Current authenticated user
     * @return Streaming ZIP file with selected photos
     */
    @PostMapping("/download-batch")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadBatch(
        @RequestBody BatchDownloadRequest request,
        @CurrentUser UserPrincipal currentUser
    ) {
        try {
            // Convert String UUIDs to UUID list
            List<UUID> photoUuids = request.photoIds().stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());

            GenerateBatchDownloadQuery query = new GenerateBatchDownloadQuery(
                photoUuids,
                currentUser.userId()
            );

            Flux<DataBuffer> zipStream = generateBatchDownloadHandler.handle(query);

            // Generate ZIP filename with date and count
            String filename = String.format(
                "photos-%s-%d-items.zip",
                LocalDate.now().toString(),
                request.photoIds().size()
            );

            return Mono.just(
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                    .body(zipStream)
            );
        } catch (IllegalArgumentException e) {
            // Invalid UUID format or validation error
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * DELETE /api/photos/{photoId} - Delete a photo
     * Soft deletes the photo by setting deletedAt timestamp
     *
     * @param photoId Photo UUID
     * @param currentUser Current authenticated user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{photoId}")
    public Mono<ResponseEntity<Void>> deletePhoto(
        @PathVariable String photoId,
        @CurrentUser UserPrincipal currentUser
    ) {
        try {
            UUID photoUuid = UUID.fromString(photoId);
            DeletePhotoCommand command = new DeletePhotoCommand(photoUuid, currentUser.userId());

            return deletePhotoHandler.handle(command)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(DeletePhotoCommandHandler.PhotoNotFoundException.class, e ->
                    Mono.just(ResponseEntity.notFound().build())
                )
                .onErrorResume(DeletePhotoCommandHandler.UnauthorizedException.class, e ->
                    Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
                );
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * POST /api/photos/delete-batch - Delete multiple photos
     * Soft deletes multiple photos by setting deletedAt timestamp
     *
     * @param request Batch delete request with photo IDs
     * @param currentUser Current authenticated user
     * @return 200 OK with deleted count, or error response
     */
    @PostMapping("/delete-batch")
    public Mono<ResponseEntity<BatchDeleteResponse>> deleteBatch(
        @RequestBody BatchDeleteRequest request,
        @CurrentUser UserPrincipal currentUser
    ) {
        try {
            // Convert String UUIDs to UUID list
            List<UUID> photoUuids = request.photoIds().stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());

            // Delete photos one by one and count successes
            return Flux.fromIterable(photoUuids)
                .flatMap(photoId -> {
                    DeletePhotoCommand command = new DeletePhotoCommand(photoId, currentUser.userId());
                    return deletePhotoHandler.handle(command)
                        .thenReturn(1) // Success
                        .onErrorReturn(0); // Failure (not found or unauthorized)
                })
                .reduce(0, Integer::sum)
                .map(deletedCount -> ResponseEntity.ok(
                    new BatchDeleteResponse(deletedCount, photoUuids.size())
                ));
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * Request DTO for batch photo deletion.
     */
    public record BatchDeleteRequest(List<String> photoIds) {
        public BatchDeleteRequest {
            if (photoIds == null || photoIds.isEmpty()) {
                throw new IllegalArgumentException("photoIds cannot be empty");
            }
            if (photoIds.size() > 50) {
                throw new IllegalArgumentException("Maximum 50 photos per batch");
            }
        }
    }

    /**
     * Response DTO for batch photo deletion.
     */
    public record BatchDeleteResponse(int deletedCount, int totalRequested) {}

    /**
     * Request DTO for batch photo download.
     * Story 3.6: Batch Photo Download
     */
    public record BatchDownloadRequest(List<String> photoIds) {
        public BatchDownloadRequest {
            if (photoIds == null || photoIds.isEmpty()) {
                throw new IllegalArgumentException("photoIds cannot be empty");
            }
            if (photoIds.size() > 50) {
                throw new IllegalArgumentException("Maximum 50 photos per batch");
            }
        }
    }
}
