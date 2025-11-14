/**
 * Upload Service - Client-Side Upload Engine
 * Story 2.4: Client-Side Upload Engine
 * Handles uploading photos directly to S3 using pre-signed URLs
 */

import axios, { AxiosProgressEvent } from 'axios';
import { SelectedPhoto } from '../types/upload';

export interface UploadProgress {
  photoId: string;
  status: 'queued' | 'uploading' | 'completed' | 'failed';
  progress: number; // 0-100
  bytesUploaded: number;
  totalBytes: number;
  error?: string;
}

export interface UploadSession {
  sessionId: string;
  photos: UploadProgress[];
  onProgress?: (progress: UploadProgress) => void;
  onComplete?: (sessionId: string) => void;
  onError?: (photoId: string, error: string) => void;
}

class UploadService {
  private maxConcurrent = 10;
  private activeUploads = 0;
  private uploadQueue: Array<() => Promise<void>> = [];
  private sessions: Map<string, UploadSession> = new Map();

  /**
   * Set maximum concurrent uploads (user preference)
   */
  setMaxConcurrent(max: number) {
    this.maxConcurrent = Math.max(1, Math.min(20, max));
  }

  /**
   * Start upload session
   */
  async startUploadSession(
    photos: SelectedPhoto[],
    apiBaseUrl: string,
    authToken: string,
    onProgress?: (progress: UploadProgress) => void,
    onComplete?: (sessionId: string) => void,
    onError?: (photoId: string, error: string) => void
  ): Promise<string> {
    try {
      // Create upload session on backend
      const response = await axios.post(
        `${apiBaseUrl}/api/upload/sessions`,
        {
          totalPhotos: photos.length,
          totalSizeBytes: photos.reduce((sum, p) => sum + p.size, 0),
        },
        {
          headers: { Authorization: `Bearer ${authToken}` },
        }
      );

      const sessionId = response.data.sessionId;

      // Initialize session
      const session: UploadSession = {
        sessionId,
        photos: photos.map((photo) => ({
          photoId: photo.id,
          status: 'queued',
          progress: 0,
          bytesUploaded: 0,
          totalBytes: photo.size,
        })),
        onProgress,
        onComplete,
        onError,
      };

      this.sessions.set(sessionId, session);

      // Queue all uploads
      for (const photo of photos) {
        this.queueUpload(sessionId, photo, apiBaseUrl, authToken);
      }

      return sessionId;
    } catch (error) {
      throw new Error('Failed to start upload session');
    }
  }

  /**
   * Queue a photo upload
   */
  private queueUpload(
    sessionId: string,
    photo: SelectedPhoto,
    apiBaseUrl: string,
    authToken: string
  ) {
    const uploadTask = async () => {
      const session = this.sessions.get(sessionId);
      if (!session) return;

      try {
        // Update status to uploading
        this.updatePhotoProgress(sessionId, photo.id, 'uploading', 0);

        // Get pre-signed URL from backend
        const initiateResponse = await axios.post(
          `${apiBaseUrl}/api/upload/sessions/${sessionId}/photos/initiate`,
          {
            filename: photo.name,
            fileSizeBytes: photo.size,
            mimeType: photo.type,
          },
          {
            headers: { Authorization: `Bearer ${authToken}` },
          }
        );

        const { uploadUrl, partUrls } = initiateResponse.data;

        if (uploadUrl) {
          // Single upload for small files
          await this.uploadSingle(sessionId, photo, uploadUrl);
        } else if (partUrls) {
          // Multipart upload for large files
          await this.uploadMultipart(sessionId, photo, partUrls);
        }

        // Mark as completed
        this.updatePhotoProgress(sessionId, photo.id, 'completed', 100);

        // Notify backend that photo was uploaded to S3
        try {
          const photoId = initiateResponse.data.photoId;
          const s3Key = initiateResponse.data.s3Key;
          await axios.post(
            `${apiBaseUrl}/api/photos/${photoId}/uploaded`,
            { s3Key },
            {
              headers: { Authorization: `Bearer ${authToken}` },
            }
          );
          console.log(`Successfully notified backend that photo ${photoId} was uploaded`);
        } catch (notifyError: any) {
          console.error('Failed to notify backend of upload completion:', notifyError);
          // Don't fail the upload - photo is in S3, we can manually mark it later
        }

        // Check if all photos completed
        this.checkSessionComplete(sessionId);
      } catch (error: any) {
        const errorMsg = error.response?.data?.error || error.message || 'Upload failed';
        this.updatePhotoProgress(sessionId, photo.id, 'failed', 0, errorMsg);
        session.onError?.(photo.id, errorMsg);
      } finally {
        this.activeUploads--;
        this.processQueue();
      }
    };

    this.uploadQueue.push(uploadTask);
    this.processQueue();
  }

  /**
   * Process upload queue
   */
  private async processQueue() {
    while (this.activeUploads < this.maxConcurrent && this.uploadQueue.length > 0) {
      const task = this.uploadQueue.shift();
      if (task) {
        this.activeUploads++;
        task(); // Fire and forget
      }
    }
  }

  /**
   * Upload single file to S3
   */
  private async uploadSingle(
    sessionId: string,
    photo: SelectedPhoto,
    uploadUrl: string
  ): Promise<void> {
    // Fetch the file blob
    const blob = await fetch(photo.uri).then((r) => r.blob());

    // Upload to S3
    await axios.put(uploadUrl, blob, {
      headers: {
        'Content-Type': photo.type,
      },
      onUploadProgress: (progressEvent: AxiosProgressEvent) => {
        if (progressEvent.total) {
          const progress = Math.round((progressEvent.loaded / progressEvent.total) * 100);
          this.updatePhotoProgress(sessionId, photo.id, 'uploading', progress);
        }
      },
    });
  }

  /**
   * Upload file in multiple parts (for large files)
   */
  private async uploadMultipart(
    sessionId: string,
    photo: SelectedPhoto,
    partUrls: string[]
  ): Promise<void> {
    const blob = await fetch(photo.uri).then((r) => r.blob());
    const partSize = 5 * 1024 * 1024; // 5MB per part

    // Upload each part
    const partPromises = partUrls.map(async (partUrl, index) => {
      const start = index * partSize;
      const end = Math.min(start + partSize, blob.size);
      const partBlob = blob.slice(start, end);

      await axios.put(partUrl, partBlob, {
        headers: {
          'Content-Type': photo.type,
        },
      });

      // Update progress based on parts completed
      const progress = Math.round(((index + 1) / partUrls.length) * 100);
      this.updatePhotoProgress(sessionId, photo.id, 'uploading', progress);
    });

    // Upload max 3 parts concurrently
    const chunks = [];
    for (let i = 0; i < partPromises.length; i += 3) {
      chunks.push(partPromises.slice(i, i + 3));
    }

    for (const chunk of chunks) {
      await Promise.all(chunk);
    }
  }

  /**
   * Update photo progress
   */
  private updatePhotoProgress(
    sessionId: string,
    photoId: string,
    status: UploadProgress['status'],
    progress: number,
    error?: string
  ) {
    const session = this.sessions.get(sessionId);
    if (!session) return;

    const photoProgress = session.photos.find((p) => p.photoId === photoId);
    if (!photoProgress) return;

    photoProgress.status = status;
    photoProgress.progress = progress;
    photoProgress.bytesUploaded = Math.round(
      (progress / 100) * photoProgress.totalBytes
    );
    if (error) {
      photoProgress.error = error;
    }

    session.onProgress?.(photoProgress);
  }

  /**
   * Check if session is complete
   */
  private checkSessionComplete(sessionId: string) {
    const session = this.sessions.get(sessionId);
    if (!session) return;

    const allComplete = session.photos.every(
      (p) => p.status === 'completed' || p.status === 'failed'
    );

    if (allComplete) {
      session.onComplete?.(sessionId);
    }
  }

  /**
   * Cancel upload session
   */
  cancelSession(sessionId: string) {
    const session = this.sessions.get(sessionId);
    if (!session) return;

    // Remove from queue
    this.uploadQueue = this.uploadQueue.filter(() => false);

    // Mark all as failed
    session.photos.forEach((photo) => {
      if (photo.status === 'queued' || photo.status === 'uploading') {
        photo.status = 'failed';
        photo.error = 'Cancelled by user';
      }
    });

    this.sessions.delete(sessionId);
  }

  /**
   * Get session status
   */
  getSessionStatus(sessionId: string): UploadProgress[] | null {
    const session = this.sessions.get(sessionId);
    return session ? session.photos : null;
  }
}

export const uploadService = new UploadService();
