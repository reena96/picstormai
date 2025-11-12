# Story 2.4: Client-Side Upload Engine

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase A - Basic Upload (Weeks 1-2)
**Status**: Draft
**Priority**: High
**Estimated Effort**: 5 days

---

## User Story

**As a** frontend developer
**I want to** upload photos directly to S3 using pre-signed URLs
**So that** uploads are fast and don't burden backend

---

## Acceptance Criteria

### AC1: Direct S3 Upload for Small Files
**Given** I have pre-signed URL for photo <5MB
**When** I PUT file to S3 URL
**Then** file uploads directly to S3
**And** upload progress is tracked (bytes uploaded / total bytes)

### AC2: Multipart Upload for Large Files
**Given** file is ≥5MB
**When** I upload via multipart
**Then** file is split into 5MB chunks
**And** chunks upload in parallel (max 3 concurrent chunks)
**And** upload completes when all chunks finish

### AC3: Upload Concurrency Management
**Given** I select 20 photos to upload
**When** upload starts
**Then** max 10 photos upload simultaneously (configurable)
**And** remaining photos are queued
**And** queued photos start when slots free up

### AC4: Progress Tracking Per Photo
**Given** photo is uploading
**When** bytes are transferred
**Then** progress event emits with: {photoId, bytesUploaded, totalBytes, percent}
**And** UI can subscribe to progress updates

### AC5: Automatic Retry on Failure
**Given** chunk upload fails with network error
**When** error occurs
**Then** chunk retries automatically (max 3 retries per chunk)
**And** retry uses exponential backoff (1s, 2s, 4s)

---

## Technical Notes

### Upload Manager Architecture

```typescript
// UploadManager.ts
export class UploadManager {
  private queue: UploadTask[] = [];
  private activeUploads: Map<string, UploadTask> = new Map();
  private maxConcurrentUploads = 10;
  private progressListeners: Map<string, ProgressCallback[]> = new Map();

  constructor(private apiClient: ApiClient) {}

  async uploadPhoto(
    photoSelection: PhotoSelection,
    sessionId: string
  ): Promise<UploadResult> {
    // 1. Get pre-signed URL from backend
    const initiateResponse = await this.apiClient.initiatePhotoUpload({
      sessionId,
      fileName: photoSelection.fileName,
      fileSizeBytes: photoSelection.fileSize,
      mimeType: photoSelection.mimeType,
    });

    // 2. Create upload task
    const task = new UploadTask(
      photoSelection,
      initiateResponse,
      this.onProgress.bind(this)
    );

    // 3. Queue or start immediately
    if (this.activeUploads.size < this.maxConcurrentUploads) {
      this.startUpload(task);
    } else {
      this.queue.push(task);
    }

    return task.promise;
  }

  private async startUpload(task: UploadTask): Promise<void> {
    this.activeUploads.set(task.photoId, task);

    try {
      if (task.isMultipart) {
        await this.uploadMultipart(task);
      } else {
        await this.uploadSinglePart(task);
      }

      // Mark completed and notify backend
      await this.apiClient.completePhotoUpload({
        sessionId: task.sessionId,
        photoId: task.photoId,
      });

      task.resolve({ success: true });
    } catch (error) {
      task.reject(error);
    } finally {
      this.activeUploads.delete(task.photoId);
      this.processQueue();
    }
  }

  private async uploadSinglePart(task: UploadTask): Promise<void> {
    const { uploadUrl, file } = task;

    await axios.put(uploadUrl, file, {
      headers: { 'Content-Type': file.type },
      onUploadProgress: (progressEvent) => {
        const percent = (progressEvent.loaded / progressEvent.total) * 100;
        this.emitProgress(task.photoId, progressEvent.loaded, progressEvent.total, percent);
      },
    });
  }

  private async uploadMultipart(task: UploadTask): Promise<void> {
    const { partUrls, file } = task;
    const chunkSize = 5 * 1024 * 1024; // 5MB
    const chunks = this.splitFileIntoChunks(file, chunkSize);

    // Upload chunks in parallel (max 3 concurrent)
    const uploadedParts: CompletedPart[] = [];
    const maxConcurrentChunks = 3;

    for (let i = 0; i < chunks.length; i += maxConcurrentChunks) {
      const batch = chunks.slice(i, i + maxConcurrentChunks);
      const batchResults = await Promise.all(
        batch.map((chunk, index) =>
          this.uploadChunkWithRetry(partUrls[i + index], chunk, i + index + 1)
        )
      );
      uploadedParts.push(...batchResults);

      // Emit progress after each batch
      const bytesUploaded = (i + batch.length) * chunkSize;
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
  }

  private async uploadChunkWithRetry(
    url: string,
    chunk: Blob,
    partNumber: number,
    attempt = 1
  ): Promise<CompletedPart> {
    const maxRetries = 3;

    try {
      const response = await axios.put(url, chunk, {
        headers: { 'Content-Type': 'application/octet-stream' },
      });

      return {
        partNumber,
        eTag: response.headers['etag'],
      };
    } catch (error) {
      if (attempt < maxRetries && this.isRetryableError(error)) {
        const backoffMs = Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s
        await this.sleep(backoffMs);
        return this.uploadChunkWithRetry(url, chunk, partNumber, attempt + 1);
      }
      throw error;
    }
  }

  private splitFileIntoChunks(file: File, chunkSize: number): Blob[] {
    const chunks: Blob[] = [];
    let offset = 0;

    while (offset < file.size) {
      const chunk = file.slice(offset, offset + chunkSize);
      chunks.push(chunk);
      offset += chunkSize;
    }

    return chunks;
  }

  private isRetryableError(error: any): boolean {
    // Retry on network errors, timeouts, 5xx errors
    return (
      error.code === 'ECONNABORTED' ||
      error.code === 'ERR_NETWORK' ||
      (error.response && error.response.status >= 500)
    );
  }

  private emitProgress(
    photoId: string,
    bytesUploaded: number,
    totalBytes: number,
    percent: number
  ): void {
    const listeners = this.progressListeners.get(photoId) || [];
    listeners.forEach((callback) =>
      callback({ photoId, bytesUploaded, totalBytes, percent })
    );
  }

  onProgress(photoId: string, callback: ProgressCallback): () => void {
    const listeners = this.progressListeners.get(photoId) || [];
    listeners.push(callback);
    this.progressListeners.set(photoId, listeners);

    // Return unsubscribe function
    return () => {
      const updated = listeners.filter((cb) => cb !== callback);
      this.progressListeners.set(photoId, updated);
    };
  }

  private processQueue(): void {
    while (this.queue.length > 0 && this.activeUploads.size < this.maxConcurrentUploads) {
      const task = this.queue.shift()!;
      this.startUpload(task);
    }
  }

  private sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}

interface ProgressCallback {
  (progress: UploadProgress): void;
}

interface UploadProgress {
  photoId: string;
  bytesUploaded: number;
  totalBytes: number;
  percent: number;
}

interface CompletedPart {
  partNumber: number;
  eTag: string;
}

class UploadTask {
  public promise: Promise<UploadResult>;
  public resolve!: (result: UploadResult) => void;
  public reject!: (error: any) => void;

  constructor(
    public photoSelection: PhotoSelection,
    public initiateResponse: InitiatePhotoUploadResponse,
    public onProgressCallback: (photoId: string, ...args: any[]) => void
  ) {
    this.promise = new Promise((resolve, reject) => {
      this.resolve = resolve;
      this.reject = reject;
    });
  }

  get photoId(): string {
    return this.initiateResponse.photoId;
  }

  get isMultipart(): boolean {
    return this.initiateResponse.uploadId != null;
  }

  get uploadUrl(): string {
    return this.initiateResponse.uploadUrl!;
  }

  get partUrls(): string[] {
    return this.initiateResponse.partUrls || [];
  }

  get uploadId(): string | undefined {
    return this.initiateResponse.uploadId;
  }

  get file(): File {
    return this.photoSelection.file;
  }

  get sessionId(): string {
    // Assume sessionId is stored in photoSelection or passed separately
    return this.photoSelection.sessionId;
  }
}

interface UploadResult {
  success: boolean;
  error?: string;
}
```

### Usage in Upload Screen

```typescript
// UploadScreen.tsx
export const UploadScreen: React.FC = () => {
  const [selectedPhotos, setSelectedPhotos] = useState<PhotoSelection[]>([]);
  const [uploadProgress, setUploadProgress] = useState<Map<string, number>>(new Map());
  const uploadManagerRef = useRef(new UploadManager(apiClient));

  const handleStartUpload = async () => {
    // 1. Create upload session
    const session = await apiClient.startUploadSession({
      totalPhotos: selectedPhotos.length,
      totalSizeBytes: selectedPhotos.reduce((sum, p) => sum + p.fileSize, 0),
    });

    // 2. Upload each photo
    selectedPhotos.forEach((photo) => {
      uploadManagerRef.current.onProgress(photo.id, (progress) => {
        setUploadProgress((prev) => new Map(prev).set(photo.id, progress.percent));
      });

      uploadManagerRef.current.uploadPhoto(photo, session.sessionId).catch((error) => {
        console.error(`Upload failed for ${photo.fileName}:`, error);
      });
    });
  };

  return (
    <View>
      <PhotoGrid photos={selectedPhotos} progress={uploadProgress} />
      <Button onPress={handleStartUpload}>Start Upload</Button>
    </View>
  );
};
```

---

## Prerequisites
- Story 2.3 (Pre-signed URLs) - MUST BE COMPLETE

---

## Testing Requirements

### Unit Tests
- [ ] UploadManager queues 20 photos, uploads 10 concurrently
- [ ] Multipart upload splits 10MB file into 2 chunks
- [ ] Chunk retry logic with exponential backoff
- [ ] Progress tracking for single-part upload
- [ ] Progress tracking for multipart upload
- [ ] isRetryableError() correctly identifies network errors

### Integration Tests
- [ ] Upload 5MB file via single-part, verify S3 object exists
- [ ] Upload 15MB file via multipart, verify S3 object exists
- [ ] Simulate network failure, verify retry succeeds
- [ ] Upload 10 photos concurrently, verify all complete

### E2E Tests
- [ ] User selects 10 photos, starts upload, all complete successfully
- [ ] Upload 1 large file (20MB), verify progress updates in UI
- [ ] Simulate network drop mid-upload, verify retry and completion

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] UploadManager class implemented with queue management
- [ ] Single-part upload working
- [ ] Multipart upload with parallel chunks working
- [ ] Retry logic with exponential backoff working
- [ ] Progress tracking emitting events correctly
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] E2E tests passing
- [ ] Code reviewed and approved

---

## Notes
- This is the most complex story in Epic 2
- Direct S3 uploads significantly reduce backend load
- Parallel chunk uploads improve large file performance
- Queue management prevents browser from being overwhelmed

---

**Status Log:**
- 2025-11-11: Story created (Draft)
