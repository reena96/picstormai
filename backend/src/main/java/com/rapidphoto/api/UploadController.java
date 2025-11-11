package com.rapidphoto.api;

import com.rapidphoto.cqrs.commands.InitiatePhotoUploadCommand;
import com.rapidphoto.cqrs.commands.StartUploadSessionCommand;
import com.rapidphoto.cqrs.commands.handlers.InitiatePhotoUploadCommandHandler;
import com.rapidphoto.cqrs.commands.handlers.StartUploadSessionCommandHandler;
import com.rapidphoto.security.CurrentUser;
import com.rapidphoto.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for upload endpoints.
 * Story 2.2: Start Upload Session
 * Story 2.3: S3 Pre-Signed URL Generation
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final StartUploadSessionCommandHandler startUploadSessionCommandHandler;
    private final InitiatePhotoUploadCommandHandler initiatePhotoUploadCommandHandler;

    public UploadController(
        StartUploadSessionCommandHandler startUploadSessionCommandHandler,
        InitiatePhotoUploadCommandHandler initiatePhotoUploadCommandHandler
    ) {
        this.startUploadSessionCommandHandler = startUploadSessionCommandHandler;
        this.initiatePhotoUploadCommandHandler = initiatePhotoUploadCommandHandler;
    }

    /**
     * POST /api/upload/sessions
     * Start a new upload session.
     *
     * Request body:
     * {
     *   "totalPhotos": 50,
     *   "totalSizeBytes": 104857600
     * }
     *
     * Response:
     * {
     *   "sessionId": "uuid",
     *   "status": "IN_PROGRESS"
     * }
     */
    @PostMapping("/sessions")
    public Mono<ResponseEntity<Map<String, Object>>> startUploadSession(
        @Valid @RequestBody StartUploadSessionRequest request,
        @CurrentUser UserPrincipal currentUser
    ) {
        var command = new StartUploadSessionCommand(
            currentUser.userId(),
            request.totalPhotos
        );

        return startUploadSessionCommandHandler.handle(command)
            .map(sessionId -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                    "sessionId", sessionId.toString(),
                    "status", "IN_PROGRESS"
                ))
            )
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()))
                )
            );
    }

    /**
     * POST /api/upload/sessions/{sessionId}/photos/initiate
     * Initiate photo upload and get pre-signed URL.
     *
     * Request body:
     * {
     *   "filename": "photo.jpg",
     *   "fileSizeBytes": 2097152,
     *   "mimeType": "image/jpeg"
     * }
     *
     * Response (for files <5MB):
     * {
     *   "photoId": "uuid",
     *   "uploadUrl": "https://s3.amazonaws.com/...",
     *   "s3Key": "uploads/userId/sessionId/photoId-filename.jpg"
     * }
     *
     * Response (for files >=5MB - multipart):
     * {
     *   "photoId": "uuid",
     *   "uploadId": "multipart-upload-id",
     *   "partUrls": ["url1", "url2", ...],
     *   "s3Key": "uploads/userId/sessionId/photoId-filename.jpg"
     * }
     */
    @PostMapping("/sessions/{sessionId}/photos/initiate")
    public Mono<ResponseEntity<Map<String, Object>>> initiatePhotoUpload(
        @PathVariable UUID sessionId,
        @Valid @RequestBody InitiatePhotoUploadRequest request,
        @CurrentUser UserPrincipal currentUser
    ) {
        var command = new InitiatePhotoUploadCommand(
            sessionId,
            currentUser.userId(),
            request.filename,
            request.fileSizeBytes,
            request.mimeType
        );

        return initiatePhotoUploadCommandHandler.handle(command)
            .map(result -> ResponseEntity
                .ok()
                .body(result)
            )
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()))
                )
            )
            .onErrorResume(IllegalStateException.class, e ->
                Mono.just(ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()))
                )
            );
    }

    // Request DTOs

    public record StartUploadSessionRequest(
        @Min(1) @Max(100) int totalPhotos,
        @Min(1) long totalSizeBytes
    ) {}

    public record InitiatePhotoUploadRequest(
        @NotBlank String filename,
        @Min(1) long fileSizeBytes,
        @NotBlank String mimeType
    ) {}
}
