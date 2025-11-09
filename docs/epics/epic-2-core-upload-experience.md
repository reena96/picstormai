# Epic 2: Core Upload Experience

**Goal**: Users can reliably upload 100 photos with real-time progress visibility

**Duration**: 6 weeks
**Dependencies**: Epic 0, Epic 1 completed
**Scope**: Batch upload, S3 direct upload, WebSocket real-time updates, network resilience

## Phase A: Basic Upload (Weeks 1-2)

---

## Story 2.1: Photo Selection & Validation UI

**As a** user
**I want to** select up to 100 photos via drag-drop or file picker
**So that** I can prepare them for upload

### Acceptance Criteria

**Given** I am on upload screen
**When** I drag photos onto dropzone OR click "Select Photos"
**Then** selected photos appear in grid with thumbnails

**Given** I select more than 100 photos
**Then** I see error "Maximum 100 photos per upload"

**Given** I select non-image file
**Then** I see error "Only image files allowed (JPG, PNG, GIF, WebP)"

**Given** I select photo over 50MB
**Then** I see warning "Large files may take longer to upload"

**Prerequisites:** Story 0.5 (Design System), Story 1.3 (Login)

**Technical Notes:**
- React Native file picker (react-native-document-picker)
- Web: HTML5 drag-drop API
- Client-side validation: file type, count, size
- Generate client-side thumbnails (Canvas API / React Native Image)
- Supported formats: image/jpeg, image/png, image/gif, image/webp
- Max 100 photos per session
- Individual file size limit: 50MB

**Testing:**
- E2E test: Select 10 photos, verify thumbnails
- Test: Select 101 photos, see error
- Test: Select PDF file, see error
- Test: Drag-drop photos (web only)
- Accessibility test: Keyboard navigation

---

## Story 2.2: Start Upload Session (Backend)

**As a** backend developer
**I want to** create upload session when user starts batch upload
**So that** progress can be tracked across all photos

### Acceptance Criteria

**Given** user initiates upload of N photos
**When** StartUploadSessionCommand is executed
**Then** UploadSession aggregate is created with status=ACTIVE

**Session tracks:**
- Total photos count
- Total size (bytes)
- Uploaded photos count
- Failed photos count
- Progress percentage

**Prerequisites:** Story 0.3 (Domain Model), Story 0.4 (CQRS)

**Technical Notes:**
- POST /api/upload/sessions
- Request body: {totalPhotos: 50, totalSizeBytes: 104857600}
- Response: {sessionId: "uuid", status: "ACTIVE"}
- Store in upload_sessions table
- Return session ID to client

**Testing:**
- Unit test: StartUploadSessionCommandHandler
- Integration test: POST /api/upload/sessions creates session
- Test: Reject if totalPhotos > 100
- Test: Calculate initial progress (0%)

---

## Story 2.3: S3 Pre-Signed URL Generation

**As a** backend developer
**I want to** generate S3 pre-signed URLs for direct client uploads
**So that** files upload directly to S3 without passing through backend

### Acceptance Criteria

**Given** user starts photo upload
**When** InitiatePhotoUploadCommand is executed
**Then** S3 pre-signed PUT URL is generated (valid 15 minutes)

**For files >5MB:**
- Generate multipart upload ID
- Return pre-signed URLs for each part

**Prerequisites:** Story 0.1 (AWS S3), Story 2.2 (Upload Session)

**Technical Notes:**
- POST /api/upload/sessions/{sessionId}/photos/initiate
- Request: {filename, fileSizeBytes, mimeType}
- Response: {photoId, uploadUrl, s3Key} OR {photoId, uploadId, partUrls[]}
- S3 key format: uploads/{userId}/{sessionId}/{photoId}-{filename}
- Pre-signed URL expires in 15 minutes
- For >5MB files: Use S3 multipart upload (5MB parts)
- Return array of pre-signed URLs for each part

**Testing:**
- Unit test: InitiatePhotoUploadCommandHandler
- Integration test: Generate pre-signed URL, verify signature
- Test: Multipart for 10MB file (2 parts)
- Test: URL expires after 15 minutes

---

## Story 2.4: Client-Side Upload Engine

**As a** frontend developer
**I want to** upload photos directly to S3 using pre-signed URLs
**So that** uploads are fast and don't burden backend

### Acceptance Criteria

**Given** I have pre-signed URL for photo
**When** I PUT file to S3 URL
**Then** file uploads directly to S3

**Given** file is >5MB
**When** I upload via multipart
**Then** file is split into 5MB chunks and uploaded in parallel (max 3 concurrent chunks)

**Upload concurrency:**
- Max 10 photos uploading simultaneously (configurable in user settings)
- Photos queued if limit exceeded

**Prerequisites:** Story 2.3 (Pre-signed URLs)

**Technical Notes:**
- Use axios for uploads with progress tracking
- For multipart: Upload chunks in parallel, then complete multipart upload
- Retry failed chunks (max 3 retries per chunk)
- Track upload progress per photo (bytes uploaded / total bytes)
- Emit progress events for UI updates

**Testing:**
- Unit test: Upload manager queues 20 photos, uploads 10 concurrently
- Test: Multipart upload for 10MB file
- Test: Progress tracking for each photo
- Test: Retry failed chunk

---

## Story 2.5: Upload Progress UI

**As a** user
**I want to** see real-time progress for each photo and overall batch
**So that** I know how the upload is progressing

### Acceptance Criteria

**Given** photos are uploading
**When** I view upload dashboard
**Then** I see:
- Overall progress bar (e.g., "45/100 photos uploaded - 67%")
- Grid of photo cards with individual progress bars
- Estimated time remaining

**Photo states:**
- Queued (gray, no progress bar)
- Uploading (blue progress bar animating)
- Completed (green checkmark)
- Failed (red X icon)

**Prerequisites:** Story 2.4 (Upload Engine), Story 0.5 (Design System)

**Technical Notes:**
- UploadDashboard organism component
- PhotoCard molecule shows individual progress
- ProgressBar molecule for overall progress
- Calculate ETA: (remaining bytes) / (average upload speed)
- Update UI every 500ms (throttled)
- Use React state for upload status

**Testing:**
- E2E test: Upload 10 photos, verify progress updates
- Test: Individual photo progress displays correctly
- Test: Overall progress aggregates correctly
- Test: ETA calculation is reasonable
- Accessibility test: Progress announced to screen readers

---

## Phase B: Real-Time Updates (Weeks 3-4)

---

## Story 2.6: WebSocket Server Setup

**As a** backend developer
**I want to** configure WebSocket server with STOMP protocol
**So that** real-time progress updates can be pushed to clients

### Acceptance Criteria

**Given** Spring WebSocket is configured
**When** client connects to ws://api/ws
**Then** WebSocket handshake succeeds

**Topic structure:**
- `/topic/upload-sessions/{sessionId}` - Session-level updates
- `/user/queue/notifications` - User-specific notifications

**Prerequisites:** Story 0.1 (Infrastructure)

**Technical Notes:**
- Spring WebSocket with STOMP over SockJS
- Configuration class with @EnableWebSocketMessageBroker
- MessageBrokerRegistry: simple in-memory broker (or Redis for scale)
- Enable SockJS fallback for old browsers
- Authentication: Intercept STOMP CONNECT with JWT token
- Allow origins: Web and mobile clients

**Testing:**
- Integration test: WebSocket connection succeeds
- Test: Subscribe to /topic/upload-sessions/{id}
- Test: Receive message on topic
- Test: Unauthenticated connection rejected

---

## Story 2.7: Real-Time Progress Broadcasting

**As a** backend developer
**I want to** broadcast upload progress to WebSocket subscribers
**So that** clients receive real-time updates without polling

### Acceptance Criteria

**Given** photo upload completes
**When** CompletePhotoUploadCommand is executed
**Then** progress message is broadcast to WebSocket topic

**Message format:**
```json
{
  "type": "PHOTO_UPLOADED",
  "sessionId": "uuid",
  "photoId": "uuid",
  "uploadedCount": 47,
  "totalCount": 100,
  "progressPercent": 47.0
}
```

**Prerequisites:** Story 2.6 (WebSocket Server)

**Technical Notes:**
- Use SimpMessagingTemplate to broadcast
- Broadcast on: photo uploaded, photo failed, session completed
- Message types: PHOTO_UPLOADED, PHOTO_FAILED, SESSION_COMPLETED
- Include timestamp for ordering
- Broadcast after database commit (TransactionalEventListener)

**Testing:**
- Integration test: Complete photo upload, verify WebSocket message received
- Test: Multiple subscribers receive message
- Test: Message includes correct progress data
- Load test: 1000 concurrent WebSocket connections

---

## Story 2.8: WebSocket Client Integration

**As a** frontend developer
**I want to** connect to WebSocket and receive real-time progress updates
**So that** UI updates without polling

### Acceptance Criteria

**Given** I start upload session
**When** I subscribe to WebSocket topic
**Then** I receive real-time progress messages

**Given** I receive PHOTO_UPLOADED message
**Then** UI updates to show photo as completed with animation

**Given** WebSocket disconnects
**When** connection is restored
**Then** I re-subscribe and sync current state

**Prerequisites:** Story 2.7 (WebSocket Broadcasting)

**Technical Notes:**
- Use @stomp/stompjs for WebSocket client
- SockJS for fallback
- Connect with JWT token in STOMP headers
- Subscribe to /topic/upload-sessions/{sessionId}
- Handle reconnection with exponential backoff
- Sync state on reconnect (fetch latest progress from REST API)

**Testing:**
- E2E test: Upload photo, receive WebSocket update
- Test: WebSocket disconnects, reconnects, re-subscribes
- Test: Progress updates reflected in UI
- Test: Multiple tabs receive updates

---

## Story 2.9: Upload Completion Notification

**As a** user
**I want to** see a celebration notification when upload completes
**So that** I feel rewarded and know upload succeeded

### Acceptance Criteria

**Given** all photos finish uploading
**When** final photo completes
**Then** I see:
- Modal overlay with "Upload Complete! 🎉"
- Confetti animation
- Success sound effect (if enabled in settings)
- Button "View Photos"

**Given** some photos failed
**Then** I see:
- "Upload completed with X failures"
- Button "Retry Failed" and "View Photos"

**Prerequisites:** Story 2.8 (WebSocket Client), Story 1.4 (User Settings)

**Technical Notes:**
- Trigger modal when session status changes to COMPLETED
- Confetti animation: react-native-confetti-cannon or web canvas
- Sound effect: play from local asset if user settings allow
- Haptic feedback on mobile (Vibration API)
- Auto-dismiss after 5 seconds OR user clicks button

**Testing:**
- E2E test: Upload completes, see confetti
- Test: Sound plays if enabled in settings
- Test: Partial failure shows retry button
- Accessibility test: Screen reader announces completion

---

## Phase C: Network Resilience (Weeks 5-6)

---

## Story 2.10: Network Loss Detection

**As a** user
**I want to** be notified immediately when network connection is lost
**So that** I understand why uploads paused

### Acceptance Criteria

**Given** uploads are in progress
**When** network connection is lost
**Then** within 5 seconds I see banner "Connection lost. Uploads paused."

**Given** connection is restored
**When** network is back online
**Then** I see banner "Connection restored. Resuming uploads."
**And** uploads automatically resume

**Prerequisites:** Story 2.4 (Upload Engine)

**Technical Notes:**
- Use NetInfo library (@react-native-community/netinfo)
- Subscribe to network state changes
- Detect: navigator.onLine (web) or NetInfo.fetch() (mobile)
- Pause upload queue when offline
- Resume queue when online
- Show persistent banner at top of screen (dismissible)

**Testing:**
- E2E test: Simulate network loss, verify banner appears
- Test: Network restored, uploads resume
- Test: Detection happens <5 seconds
- Test: WebSocket reconnects when network restored

---

## Story 2.11: Upload Error Handling & User-Friendly Messages

**As a** user
**I want to** see clear error messages when uploads fail
**So that** I understand what went wrong and can fix it

### Acceptance Criteria

**Given** photo upload fails
**When** error occurs
**Then** I see user-friendly error message (not technical stack trace)

**Error scenarios:**
- Network timeout → "Network issue. Upload will retry automatically."
- S3 permission denied → "Upload failed. Please contact support."
- File too large → "File exceeds 50MB limit."
- Invalid file type → "Only image files are supported."

**Prerequisites:** Story 2.4 (Upload Engine)

**Technical Notes:**
- Map technical errors to user messages:
  - 403 Forbidden → "Upload not authorized"
  - 408 Timeout → "Network timeout. Retrying..."
  - 413 Payload Too Large → "File too large"
  - 500 Server Error → "Server error. Please try again."
- Display error in PhotoCard with retry button
- Log full error to backend for debugging
- Show error toast notification

**Testing:**
- Test: Timeout error shows retry message
- Test: 403 error shows authorization message
- Test: 500 error logs to backend
- Test: User can dismiss error and retry

---

## Story 2.12: Upload Retry & Resume

**As a** user
**I want to** automatically retry failed uploads and resume interrupted ones
**So that** I don't lose progress from temporary failures

### Acceptance Criteria

**Given** photo upload fails
**When** error is network-related
**Then** upload retries automatically (max 3 retries, exponential backoff)

**Given** I close app during upload
**When** I reopen app
**Then** I see option "Resume 15 pending uploads?"

**Given** upload is interrupted mid-file
**When** I resume
**Then** upload resumes from last completed chunk (multipart)

**Prerequisites:** Story 2.4 (Upload Engine), Story 2.11 (Error Handling)

**Technical Notes:**
- Retry strategy: 1s, 2s, 4s exponential backoff
- Max 3 retries per photo
- Store pending uploads in AsyncStorage (mobile) or IndexedDB (web)
- For multipart uploads: Track completed parts, resume from next part
- Manual retry button on failed photos
- Bulk "Retry All Failed" button in dashboard

**Testing:**
- Test: Network error triggers auto-retry
- Test: 3 retries exhaust, marked as failed
- Test: Close app during upload, reopen shows resume prompt
- Test: Resume from last completed multipart part
- E2E test: Upload 20 photos with 5 intermittent failures, all eventually succeed

---

## Story 2.13: Upload Cancellation

**As a** user
**I want to** cancel in-progress uploads (individual or entire batch)
**So that** I can stop unwanted uploads and free up bandwidth

### Acceptance Criteria

**Given** photos are uploading
**When** I click "Cancel" button on individual photo
**Then** that specific upload is cancelled immediately
**And** partial upload is cleaned up from S3
**And** cancelled photo is removed from queue

**Given** I have 23 uploads in progress
**When** I click "Cancel All" button in dashboard header
**Then** I see confirmation modal: "Cancel all 23 remaining uploads?"
**And** if I confirm, all uploads stop immediately
**And** all partial uploads are cleaned up

**Given** I cancel an upload that's 50% complete
**Then** the incomplete file is deleted from S3
**And** metadata is NOT saved to database
**And** upload session count is updated (failed_photos++)

**Prerequisites:** Story 2.5 (Upload Progress UI), Story 2.11 (Error Handling)

**Technical Notes:**
- Individual cancel: X icon on PhotoCard with upload progress
- Batch cancel: "Cancel All" button in UploadDashboard header (only visible during uploads)
- Confirmation modal for batch cancel (prevents accidental clicks)
- Backend: DELETE /api/upload/sessions/{sessionId}/photos/{photoId}/cancel
- Backend: POST /api/upload/sessions/{sessionId}/cancel (cancel all)
- S3 cleanup: If multipart upload in progress, call AbortMultipartUpload API
- S3 cleanup: If regular upload in progress, delete incomplete object
- Update UploadSession aggregate: increment failedPhotos count
- Cancel button states: "Cancel" → "Cancelling..." → "Cancelled"

**CancelUploadCommand Implementation:**
```java
public record CancelPhotoUploadCommand(
    UploadSessionId sessionId,
    PhotoId photoId
) implements Command<Void> {}

@Service
public class CancelPhotoUploadCommandHandler implements CommandHandler<CancelPhotoUploadCommand, Void> {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private UploadSessionRepository sessionRepository;

    @Override
    @Transactional
    public Void handle(CancelPhotoUploadCommand command) {
        // 1. Find photo
        Photo photo = photoRepository.findById(command.photoId())
            .orElseThrow(() -> new PhotoNotFoundException("Photo not found"));

        // 2. Check if upload is in progress
        if (photo.getUploadStatus() != UploadStatus.UPLOADING &&
            photo.getUploadStatus() != UploadStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel completed or failed upload");
        }

        // 3. Delete from S3 (if exists)
        if (photo.getS3Location() != null) {
            try {
                s3Client.deleteObject(req -> req
                    .bucket(photo.getS3Location().getBucket())
                    .key(photo.getS3Location().getKey())
                );
            } catch (Exception e) {
                // Log but don't fail - object may not exist yet
                log.warn("Failed to delete S3 object during cancel", e);
            }
        }

        // 4. Mark photo as cancelled (use FAILED status with special error message)
        photo.failUpload("Cancelled by user");
        photoRepository.save(photo);

        // 5. Update session
        UploadSession session = sessionRepository.findById(command.sessionId())
            .orElseThrow(() -> new SessionNotFoundException("Session not found"));
        session.recordPhotoFailed();
        sessionRepository.save(session);

        return null;
    }
}
```

**UI/UX Specification:**
- Individual cancel button: Small X icon (16px), gray, appears on hover over PhotoCard
- Batch cancel button: Secondary button, gray, "Cancel All Uploads" (destructive action)
- Confirmation modal:
  - Title: "Cancel uploads?"
  - Body: "Are you sure you want to cancel 23 remaining uploads? This cannot be undone."
  - Actions: "Keep Uploading" (primary) | "Cancel Uploads" (destructive, red)
- Cancelling state: Replace progress bar with "Cancelling..." text, spinner
- Cancelled state: Gray badge "Cancelled", no retry button (permanent)

**Testing:**
- Unit test: CancelPhotoUploadCommandHandler cancels upload
- Unit test: Batch cancel stops all uploads
- Integration test: Cancel upload, verify S3 object deleted
- Integration test: Cancel updates session statistics
- E2E test: Cancel individual upload from UI
- E2E test: Cancel all uploads with confirmation modal
- E2E test: Dismiss confirmation modal, uploads continue
- Test: Cannot cancel completed upload

---

## Story 2.14: Upload Integration Tests

**As a** QA engineer
**I want to** validate complete upload flow from client to cloud
**So that** critical upload path is tested end-to-end

### Acceptance Criteria

**Given** upload system is complete
**When** I run integration tests
**Then** all upload scenarios are validated

**Test Coverage:**
1. **Basic Upload**: 10 photos → S3 → Database metadata
2. **Concurrent Upload**: 100 photos uploaded with 10 concurrent connections
3. **Multipart Upload**: 20MB file uploaded in chunks
4. **Performance**: 100 photos (2MB each) complete in <90 seconds
5. **WebSocket**: Progress updates received within 500ms
6. **Network Resilience**: Upload paused on disconnect, resumed on reconnect
7. **Retry Logic**: Failed upload retries 3 times, then marked failed
8. **Resume**: Interrupted upload resumes from correct chunk

**Prerequisites:** Stories 2.1-2.13 completed

**Technical Notes:**
- Use Testcontainers for PostgreSQL, Redis, LocalStack (S3)
- Mock network failures with proxy (Toxiproxy)
- Simulate file uploads with generated binary data
- Measure upload time for performance benchmarks
- Verify S3 objects exist and match uploaded data
- Verify database metadata matches S3 objects

**Testing:**
- All integration tests pass in CI/CD
- Performance benchmark: <90 seconds for 100 photos
- Load test: 50 concurrent users uploading
- Chaos test: Random network interruptions, all uploads eventually complete

---
