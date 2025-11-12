# Story 2.13: Upload Cancellation

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase C - Network Resilience (Weeks 5-6)
**Status**: Draft
**Priority**: Medium
**Estimated Effort**: 3 days

---

## User Story

**As a** user
**I want to** cancel in-progress uploads (individual or entire batch)
**So that** I can stop unwanted uploads and free up bandwidth

---

## Acceptance Criteria

### AC1: Cancel Individual Photo
**Given** photo is uploading (progress 0-99%)
**When** I click "Cancel" button (X icon) on photo card
**Then** that specific upload is cancelled immediately
**And** photo is removed from upload queue
**And** photo status changes to "Cancelled"
**And** photo card shows gray "Cancelled" badge

### AC2: S3 Cleanup on Cancel
**Given** I cancel upload that's 50% complete
**When** cancellation occurs
**Then** incomplete file is deleted from S3
**And** multipart upload is aborted (if multipart)
**And** backend confirms cleanup completed

### AC3: Cancel All Uploads with Confirmation
**Given** I have 23 uploads in progress (5 uploading, 18 queued)
**When** I click "Cancel All" button in dashboard header
**Then** I see confirmation modal: "Cancel all 23 remaining uploads?"
**And** modal has "Keep Uploading" (primary) and "Cancel Uploads" (destructive, red) buttons

**Given** I confirm cancellation
**Then** all 23 uploads stop immediately
**And** all queued uploads are removed
**And** all partial uploads are cleaned up from S3

**Given** I dismiss confirmation modal
**Then** uploads continue normally

### AC4: Update Session Statistics
**Given** I cancel 3 photos from upload session
**When** cancellation completes
**Then** upload session failedPhotos count increments by 3
**And** session progress updates to reflect cancelled photos

### AC5: Cannot Cancel Completed Upload
**Given** photo upload is 100% complete
**When** I attempt to cancel
**Then** cancel button is disabled/hidden
**And** no cancellation occurs

---

## Technical Notes

### Cancel Upload Command

```java
// CancelPhotoUploadCommand.java
public record CancelPhotoUploadCommand(
    UploadSessionId sessionId,
    PhotoId photoId
) implements Command<Void> {}

@Service
public class CancelPhotoUploadCommandHandler
    implements CommandHandler<CancelPhotoUploadCommand, Void> {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private UploadSessionRepository sessionRepository;

    @Override
    @Transactional
    public Mono<Void> handle(CancelPhotoUploadCommand command) {
        return photoRepository.findById(command.photoId())
            .flatMap(photo -> {
                // 1. Validate photo can be cancelled
                if (photo.getUploadStatus() == UploadStatus.COMPLETED) {
                    return Mono.error(new IllegalStateException(
                        "Cannot cancel completed upload"
                    ));
                }

                if (photo.getUploadStatus() != UploadStatus.UPLOADING &&
                    photo.getUploadStatus() != UploadStatus.PENDING) {
                    return Mono.error(new IllegalStateException(
                        "Cannot cancel upload in state: " + photo.getUploadStatus()
                    ));
                }

                // 2. Clean up S3 objects
                return cleanupS3Upload(photo)
                    .then(Mono.fromCallable(() -> {
                        // 3. Mark photo as failed with "Cancelled" reason
                        photo.failUpload("Cancelled by user");
                        return photo;
                    }))
                    .flatMap(photoRepository::save)
                    .flatMap(savedPhoto -> {
                        // 4. Update session statistics
                        return sessionRepository.findById(command.sessionId())
                            .flatMap(session -> {
                                session.recordPhotoFailed();
                                return sessionRepository.save(session);
                            });
                    })
                    .then();
            });
    }

    private Mono<Void> cleanupS3Upload(Photo photo) {
        return Mono.fromRunnable(() -> {
            try {
                S3Location location = photo.getS3Location();

                if (photo.getMultipartUploadId() != null) {
                    // Abort multipart upload
                    AbortMultipartUploadRequest abortRequest =
                        AbortMultipartUploadRequest.builder()
                            .bucket(location.getBucket())
                            .key(location.getKey())
                            .uploadId(photo.getMultipartUploadId())
                            .build();

                    s3Client.abortMultipartUpload(abortRequest);
                    log.info("Aborted multipart upload for photo {}", photo.getId());
                } else {
                    // Delete single-part upload
                    DeleteObjectRequest deleteRequest =
                        DeleteObjectRequest.builder()
                            .bucket(location.getBucket())
                            .key(location.getKey())
                            .build();

                    s3Client.deleteObject(deleteRequest);
                    log.info("Deleted incomplete S3 object for photo {}", photo.getId());
                }
            } catch (Exception e) {
                // Log but don't fail - object may not exist yet
                log.warn("Failed to cleanup S3 object during cancel: {}", e.getMessage());
            }
        });
    }
}
```

### Cancel All Session Command

```java
// CancelUploadSessionCommand.java
public record CancelUploadSessionCommand(
    UploadSessionId sessionId,
    UserId userId
) implements Command<Void> {}

@Service
public class CancelUploadSessionCommandHandler
    implements CommandHandler<CancelUploadSessionCommand, Void> {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private CancelPhotoUploadCommandHandler cancelPhotoHandler;

    @Override
    @Transactional
    public Mono<Void> handle(CancelUploadSessionCommand command) {
        // Find all in-progress/pending photos for session
        return photoRepository.findBySessionIdAndStatusIn(
                command.sessionId(),
                List.of(UploadStatus.PENDING, UploadStatus.UPLOADING)
            )
            .flatMap(photo -> {
                // Cancel each photo
                CancelPhotoUploadCommand cancelCommand = new CancelPhotoUploadCommand(
                    command.sessionId(),
                    photo.getId()
                );
                return cancelPhotoHandler.handle(cancelCommand);
            })
            .then();
    }
}
```

### REST API

```java
// PhotoUploadController.java
@RestController
@RequestMapping("/api/upload/sessions/{sessionId}/photos/{photoId}")
public class PhotoUploadController {

    @DeleteMapping("/cancel")
    public Mono<ResponseEntity<Void>> cancelUpload(
        @PathVariable String sessionId,
        @PathVariable String photoId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        CancelPhotoUploadCommand command = new CancelPhotoUploadCommand(
            new UploadSessionId(sessionId),
            new PhotoId(photoId)
        );

        return commandBus.execute(command)
            .then(Mono.just(ResponseEntity.noContent().build()))
            .onErrorResume(IllegalStateException.class, e ->
                Mono.just(ResponseEntity.badRequest().build())
            );
    }
}

@RestController
@RequestMapping("/api/upload/sessions/{sessionId}")
public class UploadSessionController {

    @PostMapping("/cancel")
    public Mono<ResponseEntity<Void>> cancelSession(
        @PathVariable String sessionId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        CancelUploadSessionCommand command = new CancelUploadSessionCommand(
            new UploadSessionId(sessionId),
            user.getUserId()
        );

        return commandBus.execute(command)
            .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
```

### Frontend Cancel Logic

```typescript
// UploadManager.ts
export class UploadManager {
  private cancelledPhotos: Set<string> = new Set();

  cancelUpload(photoId: string, sessionId: string): void {
    // Mark as cancelled
    this.cancelledPhotos.add(photoId);

    // Cancel active upload
    const activeTask = this.activeUploads.get(photoId);
    if (activeTask) {
      activeTask.abort();
      this.activeUploads.delete(photoId);
    }

    // Remove from queue
    this.queue = this.queue.filter((task) => task.photoId !== photoId);

    // Call backend to cleanup S3
    this.apiClient.cancelPhotoUpload(sessionId, photoId).catch((error) => {
      console.error('Failed to cancel upload on backend:', error);
    });

    // Emit event
    this.emit('uploadCancelled', { photoId });
  }

  cancelAll(sessionId: string): void {
    // Get all in-progress and queued photos
    const allPhotos = [
      ...Array.from(this.activeUploads.keys()),
      ...this.queue.map((t) => t.photoId),
    ];

    console.log(`Cancelling ${allPhotos.length} uploads`);

    // Cancel all
    allPhotos.forEach((photoId) => {
      this.cancelUpload(photoId, sessionId);
    });

    // Call backend to cancel session
    this.apiClient.cancelUploadSession(sessionId);
  }
}

class UploadTask {
  private abortController: AbortController;

  abort(): void {
    this.abortController.abort();
  }
}
```

### Cancel Button Component

```typescript
// PhotoCard.tsx (with cancel button)
export const PhotoCard: React.FC<PhotoCardProps> = ({ photo, progress, onCancel }) => {
  const isUploading = progress && progress.percent < 100;
  const isCancelled = photo.status === 'cancelled';

  return (
    <View style={styles.card}>
      <View style={styles.thumbnailContainer}>
        <Image source={{ uri: photo.thumbnail }} style={styles.thumbnail} />

        {/* Cancel button (X icon) on hover */}
        {isUploading && (
          <TouchableOpacity
            style={styles.cancelButton}
            onPress={() => onCancel(photo.id)}
            accessibilityLabel={`Cancel uploading ${photo.fileName}`}
          >
            <Text style={styles.cancelIcon}>✕</Text>
          </TouchableOpacity>
        )}
      </View>

      <View style={styles.info}>
        <Text style={styles.fileName} numberOfLines={1}>
          {photo.fileName}
        </Text>

        {isCancelled && <StatusBadge type="cancelled" text="Cancelled" />}

        {isUploading && <ProgressBar percent={progress.percent} />}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  cancelButton: {
    position: 'absolute',
    top: 8,
    right: 8,
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  cancelIcon: {
    fontSize: 14,
    color: 'white',
    fontWeight: 'bold',
  },
});
```

### Cancel All Confirmation Modal

```typescript
// CancelAllModal.tsx
import React from 'react';
import { Modal, View, Text, Button, StyleSheet } from 'react-native';

interface CancelAllModalProps {
  visible: boolean;
  uploadCount: number;
  onConfirm: () => void;
  onCancel: () => void;
}

export const CancelAllModal: React.FC<CancelAllModalProps> = ({
  visible,
  uploadCount,
  onConfirm,
  onCancel,
}) => {
  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onCancel}
    >
      <View style={styles.overlay}>
        <View style={styles.modal}>
          <Text style={styles.icon}>⚠️</Text>

          <Text style={styles.title}>Cancel uploads?</Text>

          <Text style={styles.message}>
            Are you sure you want to cancel {uploadCount} remaining{' '}
            {uploadCount === 1 ? 'upload' : 'uploads'}? This cannot be undone.
          </Text>

          <View style={styles.actions}>
            <Button
              title="Keep Uploading"
              onPress={onCancel}
              variant="primary"
              accessibilityLabel="Keep uploading, do not cancel"
            />
            <Button
              title="Cancel Uploads"
              onPress={onConfirm}
              variant="destructive"
              accessibilityLabel={`Cancel ${uploadCount} uploads`}
            />
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modal: {
    backgroundColor: 'white',
    borderRadius: 16,
    padding: 24,
    width: '90%',
    maxWidth: 400,
    alignItems: 'center',
  },
  icon: {
    fontSize: 48,
    marginBottom: 16,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
    marginBottom: 8,
  },
  message: {
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'center',
    marginBottom: 24,
  },
  actions: {
    flexDirection: 'column',
    gap: 12,
    width: '100%',
  },
});
```

### Integration in Upload Dashboard

```typescript
// UploadDashboard.tsx
export const UploadDashboard: React.FC<UploadDashboardProps> = ({
  sessionId,
  photos,
}) => {
  const [cancelAllModalVisible, setCancelAllModalVisible] = useState(false);
  const uploadManager = useUploadManager();

  const inProgressPhotos = photos.filter(
    (p) => p.status === 'uploading' || p.status === 'queued'
  );

  const handleCancelPhoto = (photoId: string) => {
    uploadManager.cancelUpload(photoId, sessionId);
  };

  const handleCancelAll = () => {
    setCancelAllModalVisible(false);
    uploadManager.cancelAll(sessionId);
  };

  return (
    <View>
      <View style={styles.header}>
        <Text style={styles.title}>Upload Dashboard</Text>

        {inProgressPhotos.length > 0 && (
          <Button
            title="Cancel All"
            onPress={() => setCancelAllModalVisible(true)}
            variant="secondary"
            icon="x"
            accessibilityLabel="Cancel all uploads"
          />
        )}
      </View>

      <PhotoGrid photos={photos} onCancelPhoto={handleCancelPhoto} />

      <CancelAllModal
        visible={cancelAllModalVisible}
        uploadCount={inProgressPhotos.length}
        onConfirm={handleCancelAll}
        onCancel={() => setCancelAllModalVisible(false)}
      />
    </View>
  );
};
```

---

## Prerequisites
- Story 2.5 (Upload Progress UI) - MUST BE COMPLETE
- Story 2.11 (Error Handling) - COMPLETE

---

## Testing Requirements

### Unit Tests
- [ ] CancelPhotoUploadCommandHandler cancels upload
- [ ] S3 multipart upload aborted correctly
- [ ] S3 single-part object deleted correctly
- [ ] Session statistics updated (failedPhotos incremented)
- [ ] Cannot cancel completed upload (throws exception)

### Integration Tests
- [ ] Cancel upload via API, verify S3 object deleted
- [ ] Cancel multipart upload, verify abort called
- [ ] Cancel all uploads, verify all cleaned up
- [ ] Session failedPhotos count correct after cancellations

### E2E Tests
- [ ] Cancel individual upload from UI, verify removed
- [ ] Cancel all uploads with confirmation modal
- [ ] Dismiss confirmation modal, uploads continue
- [ ] Cannot cancel completed upload (button disabled)

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] Backend command handlers implemented
- [ ] S3 cleanup (abort multipart, delete object) working
- [ ] Individual cancel button on photo card
- [ ] Cancel All button with confirmation modal
- [ ] Session statistics updated correctly
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] E2E tests passing
- [ ] Code reviewed and approved

---

## Notes
- Cancellation is destructive and cannot be undone (must re-upload if needed)
- S3 cleanup prevents orphaned objects (cost optimization)
- Confirmation modal for "Cancel All" prevents accidental clicks
- Cancelled photos count as "failed" in session statistics
- AbortMultipartUpload API ensures no partial chunks remain in S3

---

**Status Log:**
- 2025-11-11: Story created (Draft)
