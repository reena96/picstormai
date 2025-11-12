# Story 2.3: S3 Pre-Signed URL Generation

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase A - Basic Upload (Weeks 1-2)
**Status**: Draft
**Priority**: High
**Estimated Effort**: 3 days

---

## User Story

**As a** backend developer
**I want to** generate S3 pre-signed URLs for direct client uploads
**So that** files upload directly to S3 without passing through backend

---

## Acceptance Criteria

### AC1: Generate Pre-Signed URL for Small Files (<5MB)
**Given** user starts photo upload with file <5MB
**When** InitiatePhotoUploadCommand is executed
**Then** S3 pre-signed PUT URL is generated (valid 15 minutes)
**And** response includes: {photoId, uploadUrl, s3Key}

### AC2: Generate Multipart Upload for Large Files (≥5MB)
**Given** user starts photo upload with file ≥5MB
**When** InitiatePhotoUploadCommand is executed
**Then** S3 multipart upload ID is created
**And** response includes: {photoId, uploadId, partUrls: [url1, url2, ...]}
**And** each part is 5MB (except last part)

### AC3: S3 Key Format
**Given** upload initiated
**Then** S3 key follows format: `uploads/{userId}/{sessionId}/{photoId}-{filename}`
**Example**: `uploads/user-123/session-456/photo-789-sunset.jpg`

### AC4: Pre-Signed URL Expiration
**Given** pre-signed URL generated
**Then** URL expires in exactly 15 minutes
**And** expired URL returns 403 Forbidden

### AC5: Photo Metadata Tracking
**Given** upload initiated
**Then** Photo aggregate is created with:
- photoId (UUID)
- fileName
- fileSizeBytes
- mimeType
- uploadStatus = PENDING
- s3Location (bucket, key)
- uploadSessionId (foreign key)

---

## Technical Notes

### Domain Model

```java
@Entity
@Table(name = "photos")
public class Photo {
    @EmbeddedId
    private PhotoId id;

    @Embedded
    private UploadSessionId uploadSessionId;

    @Embedded
    private S3Location s3Location;

    private String fileName;
    private Long fileSizeBytes;
    private String mimeType;

    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus = UploadStatus.PENDING;

    private String multipartUploadId; // null for single-part uploads

    private Instant initiatedAt = Instant.now();
    private Instant completedAt;

    public void completeUpload() {
        this.uploadStatus = UploadStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void failUpload(String reason) {
        this.uploadStatus = UploadStatus.FAILED;
        this.failureReason = reason;
    }
}

public record S3Location(
    String bucket,
    String key
) {}

public enum UploadStatus {
    PENDING, UPLOADING, COMPLETED, FAILED
}
```

### Command & Handler

```java
public record InitiatePhotoUploadCommand(
    UploadSessionId sessionId,
    String fileName,
    Long fileSizeBytes,
    String mimeType,
    UserId userId
) implements Command<InitiatePhotoUploadResponse> {}

public record InitiatePhotoUploadResponse(
    PhotoId photoId,
    String uploadUrl, // null for multipart
    String s3Key,
    String uploadId, // null for single-part
    List<String> partUrls // empty for single-part
) {}

@Service
public class InitiatePhotoUploadCommandHandler
    implements CommandHandler<InitiatePhotoUploadCommand, InitiatePhotoUploadResponse> {

    private static final long MULTIPART_THRESHOLD = 5_000_000L; // 5MB
    private static final long PART_SIZE = 5_000_000L; // 5MB per part
    private static final Duration URL_EXPIRATION = Duration.ofMinutes(15);

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.upload-bucket}")
    private String uploadBucket;

    @Override
    @Transactional
    public Mono<InitiatePhotoUploadResponse> handle(InitiatePhotoUploadCommand command) {
        // 1. Generate S3 key
        String s3Key = generateS3Key(
            command.userId(),
            command.sessionId(),
            command.fileName()
        );

        PhotoId photoId = PhotoId.generate();

        // 2. Create Photo aggregate
        Photo photo = new Photo(
            photoId,
            command.sessionId(),
            new S3Location(uploadBucket, s3Key),
            command.fileName(),
            command.fileSizeBytes(),
            command.mimeType()
        );

        // 3. Generate pre-signed URL(s)
        if (command.fileSizeBytes() < MULTIPART_THRESHOLD) {
            return handleSinglePartUpload(photo);
        } else {
            return handleMultipartUpload(photo, command.fileSizeBytes());
        }
    }

    private Mono<InitiatePhotoUploadResponse> handleSinglePartUpload(Photo photo) {
        return Mono.fromCallable(() -> {
            // Generate pre-signed PUT URL
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(photo.getS3Location().bucket())
                .key(photo.getS3Location().key())
                .contentType(photo.getMimeType())
                .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(URL_EXPIRATION)
                .putObjectRequest(putRequest)
                .build();

            String presignedUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

            return presignedUrl;
        })
        .flatMap(presignedUrl ->
            photoRepository.save(photo)
                .map(savedPhoto -> new InitiatePhotoUploadResponse(
                    savedPhoto.getId(),
                    presignedUrl,
                    savedPhoto.getS3Location().key(),
                    null,
                    List.of()
                ))
        );
    }

    private Mono<InitiatePhotoUploadResponse> handleMultipartUpload(Photo photo, Long fileSize) {
        return Mono.fromCallable(() -> {
            // 1. Create multipart upload
            CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(photo.getS3Location().bucket())
                .key(photo.getS3Location().key())
                .contentType(photo.getMimeType())
                .build();

            CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
            String uploadId = createResponse.uploadId();

            // 2. Calculate number of parts
            int partCount = (int) Math.ceil((double) fileSize / PART_SIZE);

            // 3. Generate pre-signed URL for each part
            List<String> partUrls = new ArrayList<>();
            for (int partNumber = 1; partNumber <= partCount; partNumber++) {
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(photo.getS3Location().bucket())
                    .key(photo.getS3Location().key())
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

                UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(URL_EXPIRATION)
                    .uploadPartRequest(uploadPartRequest)
                    .build();

                String presignedUrl = s3Presigner.presignUploadPart(presignRequest)
                    .url()
                    .toString();

                partUrls.add(presignedUrl);
            }

            return new MultipartUploadData(uploadId, partUrls);
        })
        .flatMap(multipartData -> {
            photo.setMultipartUploadId(multipartData.uploadId());
            return photoRepository.save(photo)
                .map(savedPhoto -> new InitiatePhotoUploadResponse(
                    savedPhoto.getId(),
                    null,
                    savedPhoto.getS3Location().key(),
                    multipartData.uploadId(),
                    multipartData.partUrls()
                ));
        });
    }

    private String generateS3Key(UserId userId, UploadSessionId sessionId, String fileName) {
        PhotoId photoId = PhotoId.generate();
        String sanitizedFileName = sanitizeFileName(fileName);
        return String.format("uploads/%s/%s/%s-%s",
            userId.getValue(),
            sessionId.getValue(),
            photoId.getValue(),
            sanitizedFileName
        );
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private record MultipartUploadData(String uploadId, List<String> partUrls) {}
}
```

### REST API

```java
@RestController
@RequestMapping("/api/upload/sessions/{sessionId}/photos")
public class PhotoUploadController {

    @PostMapping("/initiate")
    public Mono<ResponseEntity<InitiatePhotoUploadResponse>> initiateUpload(
        @PathVariable String sessionId,
        @RequestBody @Valid InitiatePhotoUploadRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        InitiatePhotoUploadCommand command = new InitiatePhotoUploadCommand(
            new UploadSessionId(sessionId),
            request.fileName(),
            request.fileSizeBytes(),
            request.mimeType(),
            user.getUserId()
        );

        return commandBus.execute(command)
            .map(ResponseEntity::ok);
    }
}

record InitiatePhotoUploadRequest(
    @NotBlank String fileName,
    @Min(1) @Max(50_000_000) Long fileSizeBytes, // max 50MB
    @NotBlank String mimeType
) {}
```

### Database Migration

```sql
-- V3__create_photos_table.sql
CREATE TABLE photos (
    id UUID PRIMARY KEY,
    upload_session_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    upload_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    s3_bucket VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    multipart_upload_id VARCHAR(255), -- null for single-part uploads
    failure_reason TEXT,
    initiated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,

    CONSTRAINT fk_photos_upload_session FOREIGN KEY (upload_session_id)
        REFERENCES upload_sessions(id) ON DELETE CASCADE,
    CONSTRAINT chk_file_size CHECK (file_size_bytes > 0)
);

CREATE INDEX idx_photos_upload_session ON photos(upload_session_id);
CREATE INDEX idx_photos_status ON photos(upload_status);
CREATE UNIQUE INDEX idx_photos_s3_key ON photos(s3_bucket, s3_key);
```

---

## Prerequisites
- Story 0.1 (AWS S3) - COMPLETE
- Story 2.2 (Upload Session) - MUST BE COMPLETE

---

## Testing Requirements

### Unit Tests
- [ ] generateS3Key() creates correct format
- [ ] sanitizeFileName() removes special characters
- [ ] Single-part upload for 2MB file
- [ ] Multipart upload for 10MB file (2 parts)
- [ ] Multipart upload for 15MB file (3 parts)
- [ ] URL expiration set to 15 minutes

### Integration Tests
- [ ] POST /api/upload/sessions/{id}/photos/initiate creates Photo
- [ ] Response includes valid pre-signed URL
- [ ] Pre-signed URL works for PUT request
- [ ] Multipart response includes correct number of part URLs
- [ ] Expired URL returns 403 after 15 minutes
- [ ] Verify Photo stored in database with PENDING status

### Load Tests
- [ ] Generate 100 pre-signed URLs in <5 seconds
- [ ] Multipart URLs generated correctly under concurrent load

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] Photo domain model implemented
- [ ] Command handler for single-part uploads
- [ ] Command handler for multipart uploads
- [ ] Database migration executed
- [ ] REST API endpoint working
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Code reviewed and approved
- [ ] S3 bucket configured with correct CORS policy

---

## Notes
- Pre-signed URLs allow direct client-to-S3 uploads (no backend proxy)
- 5MB threshold chosen to optimize for network efficiency
- Multipart uploads enable parallel chunk uploads (Story 2.4)
- S3 key includes userId for security (users can't guess other keys)

---

**Status Log:**
- 2025-11-11: Story created (Draft)
