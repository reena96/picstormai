# Story 2.2: Start Upload Session (Backend)

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase A - Basic Upload (Weeks 1-2)
**Status**: Draft
**Priority**: High
**Estimated Effort**: 2 days

---

## User Story

**As a** backend developer
**I want to** create upload session when user starts batch upload
**So that** progress can be tracked across all photos

---

## Acceptance Criteria

### AC1: Create Upload Session
**Given** user initiates upload of N photos
**When** StartUploadSessionCommand is executed
**Then** UploadSession aggregate is created with status=ACTIVE
**And** sessionId (UUID) is returned to client

### AC2: Session Tracks Required Metadata
**Given** upload session is created
**Then** session tracks:
- Total photos count (totalPhotos)
- Total size in bytes (totalSizeBytes)
- Uploaded photos count (uploadedPhotos) - initially 0
- Failed photos count (failedPhotos) - initially 0
- Progress percentage (progressPercent) - initially 0.0
- Status (ACTIVE, COMPLETED, CANCELLED)
- Created timestamp
- User ID (owner)

### AC3: Prevent Excessive Upload Sessions
**Given** user has 5 ACTIVE upload sessions
**When** user attempts to create 6th session
**Then** request is rejected with error "Maximum 5 concurrent upload sessions allowed"

### AC4: Validate Input Constraints
**Given** StartUploadSessionCommand with totalPhotos > 100
**When** command is executed
**Then** validation fails with "Maximum 100 photos per session"

**Given** totalSizeBytes > 5GB
**Then** validation fails with "Maximum total upload size is 5GB"

---

## Technical Notes

### Domain Model

```java
@Entity
@Table(name = "upload_sessions")
public class UploadSession {
    @EmbeddedId
    private UploadSessionId id;

    @Embedded
    private UserId userId;

    private Integer totalPhotos;
    private Long totalSizeBytes;
    private Integer uploadedPhotos = 0;
    private Integer failedPhotos = 0;
    private Double progressPercent = 0.0;

    @Enumerated(EnumType.STRING)
    private UploadSessionStatus status = UploadSessionStatus.ACTIVE;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public void recordPhotoUploaded() {
        this.uploadedPhotos++;
        this.progressPercent = calculateProgress();
        this.updatedAt = Instant.now();
    }

    public void recordPhotoFailed() {
        this.failedPhotos++;
        this.progressPercent = calculateProgress();
        this.updatedAt = Instant.now();
    }

    private Double calculateProgress() {
        int processed = uploadedPhotos + failedPhotos;
        return (processed * 100.0) / totalPhotos;
    }
}

public enum UploadSessionStatus {
    ACTIVE, COMPLETED, CANCELLED
}
```

### Command & Handler

```java
public record StartUploadSessionCommand(
    UserId userId,
    Integer totalPhotos,
    Long totalSizeBytes
) implements Command<UploadSessionId> {}

@Service
public class StartUploadSessionCommandHandler implements CommandHandler<StartUploadSessionCommand, UploadSessionId> {

    @Autowired
    private UploadSessionRepository sessionRepository;

    @Override
    @Transactional
    public Mono<UploadSessionId> handle(StartUploadSessionCommand command) {
        // 1. Validate constraints
        validatePhotoCount(command.totalPhotos());
        validateTotalSize(command.totalSizeBytes());

        // 2. Check active session count
        return sessionRepository.countActiveSessionsByUserId(command.userId())
            .flatMap(count -> {
                if (count >= 5) {
                    return Mono.error(new TooManyActiveSessionsException());
                }

                // 3. Create session
                UploadSession session = new UploadSession(
                    UploadSessionId.generate(),
                    command.userId(),
                    command.totalPhotos(),
                    command.totalSizeBytes()
                );

                // 4. Save and return ID
                return sessionRepository.save(session)
                    .map(UploadSession::getId);
            });
    }

    private void validatePhotoCount(Integer count) {
        if (count == null || count <= 0) {
            throw new InvalidParameterException("Total photos must be > 0");
        }
        if (count > 100) {
            throw new InvalidParameterException("Maximum 100 photos per session");
        }
    }

    private void validateTotalSize(Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new InvalidParameterException("Total size must be > 0");
        }
        if (sizeBytes > 5_000_000_000L) { // 5GB
            throw new InvalidParameterException("Maximum total upload size is 5GB");
        }
    }
}
```

### REST API

```java
@RestController
@RequestMapping("/api/upload/sessions")
public class UploadSessionController {

    @PostMapping
    public Mono<ResponseEntity<StartUploadSessionResponse>> startSession(
        @RequestBody @Valid StartUploadSessionRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        StartUploadSessionCommand command = new StartUploadSessionCommand(
            user.getUserId(),
            request.totalPhotos(),
            request.totalSizeBytes()
        );

        return commandBus.execute(command)
            .map(sessionId -> ResponseEntity.ok(new StartUploadSessionResponse(
                sessionId.getValue(),
                UploadSessionStatus.ACTIVE.name()
            )));
    }
}

record StartUploadSessionRequest(
    @Min(1) @Max(100) Integer totalPhotos,
    @Min(1) @Max(5_000_000_000L) Long totalSizeBytes
) {}

record StartUploadSessionResponse(
    String sessionId,
    String status
) {}
```

### Database Migration

```sql
-- V2__create_upload_sessions_table.sql
CREATE TABLE upload_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    total_photos INTEGER NOT NULL,
    total_size_bytes BIGINT NOT NULL,
    uploaded_photos INTEGER NOT NULL DEFAULT 0,
    failed_photos INTEGER NOT NULL DEFAULT 0,
    progress_percent NUMERIC(5,2) NOT NULL DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_upload_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_total_photos CHECK (total_photos > 0 AND total_photos <= 100),
    CONSTRAINT chk_total_size CHECK (total_size_bytes > 0),
    CONSTRAINT chk_progress CHECK (progress_percent >= 0 AND progress_percent <= 100)
);

CREATE INDEX idx_upload_sessions_user_status ON upload_sessions(user_id, status);
CREATE INDEX idx_upload_sessions_created_at ON upload_sessions(created_at DESC);
```

---

## Prerequisites
- Story 0.3 (Domain Model) - COMPLETE
- Story 0.4 (CQRS) - COMPLETE
- Story 0.2 (Database Schema) - COMPLETE

---

## Testing Requirements

### Unit Tests
- [ ] StartUploadSessionCommandHandler creates session successfully
- [ ] Validate totalPhotos > 100 throws exception
- [ ] Validate totalSizeBytes > 5GB throws exception
- [ ] recordPhotoUploaded() increments count and updates progress
- [ ] recordPhotoFailed() increments failed count and updates progress
- [ ] calculateProgress() returns correct percentage

### Integration Tests
- [ ] POST /api/upload/sessions creates session in database
- [ ] Session response includes sessionId and status=ACTIVE
- [ ] Reject request with totalPhotos=101
- [ ] Reject request when user has 5 active sessions
- [ ] Verify session stored with correct initial values

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] Domain model implemented with DDD patterns
- [ ] Command handler with reactive Mono<T> return type
- [ ] Database migration executed successfully
- [ ] REST API endpoint working
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Code reviewed and approved
- [ ] API documentation updated (Swagger)

---

## Notes
- This story is backend-only, no frontend changes
- Session tracking is foundation for Stories 2.3-2.5
- Progress calculation is simple: (uploaded + failed) / total
- Maximum 5 concurrent sessions prevents resource exhaustion

---

**Status Log:**
- 2025-11-11: Story created (Draft)
