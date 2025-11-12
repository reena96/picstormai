# Story 2.12: Upload Retry & Resume

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase C - Network Resilience (Weeks 5-6)
**Status**: Draft
**Priority**: High
**Estimated Effort**: 4 days

---

## User Story

**As a** user
**I want to** automatically retry failed uploads and resume interrupted ones
**So that** I don't lose progress from temporary failures

---

## Acceptance Criteria

### AC1: Automatic Retry on Network Errors
**Given** photo upload fails with network-related error
**When** error is retryable (timeout, 5xx, network loss)
**Then** upload retries automatically after 1 second
**And** uses exponential backoff (1s, 2s, 4s)
**And** retries max 3 times before marking as failed

### AC2: Manual Retry Button
**Given** photo upload failed (after 3 auto-retries)
**When** I click "Retry" button on photo card
**Then** upload restarts from beginning
**And** photo status changes to "uploading"

### AC3: Bulk Retry Failed Photos
**Given** 15 photos failed in upload session
**When** I click "Retry All Failed" button in dashboard header
**Then** all 15 failed photos retry simultaneously
**And** photos are added to upload queue

### AC4: Resume After App Close
**Given** I close app during upload
**When** I reopen app
**Then** I see prompt "Resume 15 pending uploads?"
**And** if I click "Resume", uploads restart from last checkpoint

### AC5: Resume Multipart Upload from Last Chunk
**Given** 10MB file uploading via multipart (2 chunks)
**When** upload interrupted after chunk 1 completes
**Then** on resume, only chunk 2 is uploaded
**And** chunk 1 is not re-uploaded (saves bandwidth)

---

## Technical Notes

### Retry Logic in Upload Manager

```typescript
// UploadManager.ts (with retry logic)
export class UploadManager {
  private readonly MAX_RETRIES = 3;

  async uploadPhotoWithRetry(
    photoSelection: PhotoSelection,
    sessionId: string,
    userId: string,
    attempt = 1
  ): Promise<UploadResult> {
    try {
      // Attempt upload
      return await this.uploadPhoto(photoSelection, sessionId, userId);
    } catch (error) {
      const userError = ErrorMapper.mapError(error);

      // Check if error is retryable
      if (!userError.retryable || attempt >= this.MAX_RETRIES) {
        // Max retries exhausted or non-retryable error
        await this.errorLogger.logError(error, {
          userId,
          sessionId,
          photoId: photoSelection.id,
        });

        return {
          success: false,
          error: userError,
          retriesExhausted: true,
        };
      }

      // Calculate backoff delay: 1s, 2s, 4s
      const backoffMs = Math.pow(2, attempt - 1) * 1000;
      console.log(
        `Upload failed for ${photoSelection.fileName}, retrying in ${backoffMs}ms (attempt ${attempt}/${this.MAX_RETRIES})`
      );

      // Emit retry event for UI
      this.emit('uploadRetrying', {
        photoId: photoSelection.id,
        attempt,
        maxRetries: this.MAX_RETRIES,
        delayMs: backoffMs,
      });

      // Wait before retry
      await this.sleep(backoffMs);

      // Retry
      return this.uploadPhotoWithRetry(photoSelection, sessionId, userId, attempt + 1);
    }
  }

  manualRetry(photoId: string): void {
    const failedPhoto = this.failedPhotos.get(photoId);
    if (!failedPhoto) return;

    // Remove from failed map
    this.failedPhotos.delete(photoId);

    // Add back to queue
    this.uploadPhotoWithRetry(
      failedPhoto.photoSelection,
      failedPhoto.sessionId,
      failedPhoto.userId
    );
  }

  retryAllFailed(): void {
    const failedPhotos = Array.from(this.failedPhotos.values());

    console.log(`Retrying ${failedPhotos.length} failed uploads`);

    failedPhotos.forEach((failed) => {
      this.manualRetry(failed.photoSelection.id);
    });
  }
}
```

### Persistent Upload State

```typescript
// UploadStateStorage.ts
import AsyncStorage from '@react-native-async-storage/async-storage';

export interface PendingUpload {
  photoSelection: PhotoSelection;
  sessionId: string;
  userId: string;
  completedChunks?: number[]; // For multipart uploads
  uploadedBytes?: number; // For single-part uploads
  timestamp: string;
}

export class UploadStateStorage {
  private readonly STORAGE_KEY = 'pending_uploads';

  async savePendingUpload(upload: PendingUpload): Promise<void> {
    const pending = await this.getPendingUploads();
    pending.push(upload);
    await AsyncStorage.setItem(this.STORAGE_KEY, JSON.stringify(pending));
  }

  async getPendingUploads(): Promise<PendingUpload[]> {
    const data = await AsyncStorage.getItem(this.STORAGE_KEY);
    return data ? JSON.parse(data) : [];
  }

  async removePendingUpload(photoId: string): Promise<void> {
    const pending = await this.getPendingUploads();
    const filtered = pending.filter((p) => p.photoSelection.id !== photoId);
    await AsyncStorage.setItem(this.STORAGE_KEY, JSON.stringify(filtered));
  }

  async clearPendingUploads(): Promise<void> {
    await AsyncStorage.removeItem(this.STORAGE_KEY);
  }

  async updateCompletedChunks(photoId: string, chunkNumber: number): Promise<void> {
    const pending = await this.getPendingUploads();
    const upload = pending.find((p) => p.photoSelection.id === photoId);

    if (upload) {
      upload.completedChunks = upload.completedChunks || [];
      if (!upload.completedChunks.includes(chunkNumber)) {
        upload.completedChunks.push(chunkNumber);
      }
      await AsyncStorage.setItem(this.STORAGE_KEY, JSON.stringify(pending));
    }
  }
}
```

### Resume Prompt Component

```typescript
// ResumeUploadsPrompt.tsx
import React from 'react';
import { Modal, View, Text, Button, StyleSheet } from 'react-native';

interface ResumeUploadsPromptProps {
  visible: boolean;
  pendingCount: number;
  onResume: () => void;
  onCancel: () => void;
}

export const ResumeUploadsPrompt: React.FC<ResumeUploadsPromptProps> = ({
  visible,
  pendingCount,
  onResume,
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
          <Text style={styles.icon}>⏸️</Text>

          <Text style={styles.title}>Resume Uploads?</Text>

          <Text style={styles.message}>
            You have {pendingCount} pending {pendingCount === 1 ? 'upload' : 'uploads'} from
            a previous session.
          </Text>

          <View style={styles.actions}>
            <Button
              title="Cancel"
              onPress={onCancel}
              variant="secondary"
              accessibilityLabel="Cancel and discard pending uploads"
            />
            <Button
              title="Resume"
              onPress={onResume}
              variant="primary"
              accessibilityLabel={`Resume ${pendingCount} pending uploads`}
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
    flexDirection: 'row',
    gap: 12,
    width: '100%',
  },
});
```

### Multipart Resume Logic

```typescript
// UploadManager.ts (multipart resume)
private async uploadMultipartWithResume(
  task: UploadTask,
  completedChunks: number[] = []
): Promise<void> {
  const { partUrls, file } = task;
  const chunkSize = 5 * 1024 * 1024; // 5MB
  const chunks = this.splitFileIntoChunks(file, chunkSize);

  const uploadedParts: CompletedPart[] = [];

  // Upload only remaining chunks
  for (let i = 0; i < chunks.length; i++) {
    const partNumber = i + 1;

    // Skip already completed chunks
    if (completedChunks.includes(partNumber)) {
      console.log(`Skipping chunk ${partNumber} (already uploaded)`);
      // We still need the ETag, fetch from backend
      const etag = await this.getCompletedPartETag(task.photoId, partNumber);
      uploadedParts.push({ partNumber, eTag: etag });
      continue;
    }

    // Upload chunk
    const part = await this.uploadChunkWithRetry(
      partUrls[i],
      chunks[i],
      partNumber
    );
    uploadedParts.push(part);

    // Save progress
    await this.stateStorage.updateCompletedChunks(task.photoId, partNumber);

    // Emit progress
    const bytesUploaded = (i + 1) * chunkSize;
    const percent = (bytesUploaded / file.size) * 100;
    this.emitProgress(task.photoId, bytesUploaded, file.size, percent);
  }

  // Complete multipart upload
  await this.apiClient.completeMultipartUpload({
    sessionId: task.sessionId,
    photoId: task.photoId,
    uploadId: task.uploadId!,
    parts: uploadedParts,
  });

  // Remove from pending
  await this.stateStorage.removePendingUpload(task.photoId);
}
```

### App Initialization with Resume Check

```typescript
// App.tsx
export const App: React.FC = () => {
  const [resumePromptVisible, setResumePromptVisible] = useState(false);
  const [pendingUploads, setPendingUploads] = useState<PendingUpload[]>([]);
  const stateStorage = new UploadStateStorage();

  useEffect(() => {
    // Check for pending uploads on app launch
    checkPendingUploads();
  }, []);

  const checkPendingUploads = async () => {
    const pending = await stateStorage.getPendingUploads();

    if (pending.length > 0) {
      setPendingUploads(pending);
      setResumePromptVisible(true);
    }
  };

  const handleResume = async () => {
    setResumePromptVisible(false);

    // Navigate to upload screen
    navigation.navigate('Upload');

    // Resume uploads
    pendingUploads.forEach((upload) => {
      uploadManager.uploadPhotoWithRetry(
        upload.photoSelection,
        upload.sessionId,
        upload.userId
      );
    });
  };

  const handleCancelResume = async () => {
    setResumePromptVisible(false);
    await stateStorage.clearPendingUploads();
  };

  return (
    <>
      <NavigationContainer>
        {/* ... app navigation ... */}
      </NavigationContainer>

      <ResumeUploadsPrompt
        visible={resumePromptVisible}
        pendingCount={pendingUploads.length}
        onResume={handleResume}
        onCancel={handleCancelResume}
      />
    </>
  );
};
```

### Retry All Failed Button

```typescript
// UploadDashboard.tsx
export const UploadDashboard: React.FC<UploadDashboardProps> = ({
  sessionId,
  photos,
}) => {
  const failedPhotos = photos.filter((p) => p.status === 'failed');

  return (
    <View>
      <View style={styles.header}>
        <Text style={styles.title}>Upload Dashboard</Text>

        {failedPhotos.length > 0 && (
          <Button
            title={`Retry All Failed (${failedPhotos.length})`}
            onPress={() => uploadManager.retryAllFailed()}
            variant="secondary"
            icon="refresh"
            accessibilityLabel={`Retry ${failedPhotos.length} failed uploads`}
          />
        )}
      </View>

      <PhotoGrid photos={photos} />
    </View>
  );
};
```

---

## Prerequisites
- Story 2.4 (Upload Engine) - MUST BE COMPLETE
- Story 2.11 (Error Handling) - MUST BE COMPLETE

---

## Testing Requirements

### Unit Tests
- [ ] Automatic retry with exponential backoff (1s, 2s, 4s)
- [ ] Max 3 retries, then mark as failed
- [ ] Manual retry button restarts upload
- [ ] UploadStateStorage saves/loads pending uploads
- [ ] Multipart resume skips completed chunks

### Integration Tests
- [ ] Upload fails with network error, retries 3 times, then fails
- [ ] Manual retry after failure succeeds
- [ ] Pending upload saved to storage on app close
- [ ] Resume prompt appears on app reopen with pending uploads
- [ ] Multipart upload resumes from last completed chunk

### E2E Tests
- [ ] Upload 20 photos with 5 intermittent failures, all eventually succeed
- [ ] Close app mid-upload, reopen, resume successfully
- [ ] Retry all failed (10 photos), verify all retry
- [ ] Multipart upload interrupted, resume from correct chunk
- [ ] Network error during upload, auto-retry succeeds

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] Automatic retry with exponential backoff implemented
- [ ] Manual retry button working
- [ ] Bulk "Retry All Failed" button working
- [ ] Persistent upload state (AsyncStorage/IndexedDB)
- [ ] Resume prompt on app launch
- [ ] Multipart resume from last chunk implemented
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] E2E tests passing
- [ ] Code reviewed and approved

---

## Notes
- Exponential backoff prevents overwhelming server during outages
- Persistent state allows resume after app crash or manual close
- Multipart resume saves bandwidth (don't re-upload completed chunks)
- Max 3 retries balances reliability and user wait time
- Bulk retry useful when network issue affects many photos simultaneously

---

**Status Log:**
- 2025-11-11: Story created (Draft)
