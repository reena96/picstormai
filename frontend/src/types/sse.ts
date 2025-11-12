/**
 * SSE (Server-Sent Events) message type definitions
 * Story 2.8: SSE Client Integration
 */

export type UploadProgressMessage =
  | PhotoUploadedMessage
  | PhotoFailedMessage
  | SessionCompletedMessage;

export interface PhotoUploadedMessage {
  type: 'PHOTO_UPLOADED';
  sessionId: string;
  photoId: string;
  uploadedCount: number;
  totalCount: number;
  progressPercent: number;
  timestamp: string;
}

export interface PhotoFailedMessage {
  type: 'PHOTO_FAILED';
  sessionId: string;
  photoId: string;
  reason: string;
  failedCount: number;
  totalCount: number;
  timestamp: string;
}

export interface SessionCompletedMessage {
  type: 'SESSION_COMPLETED';
  sessionId: string;
  uploadedCount: number;
  failedCount: number;
  totalCount: number;
  timestamp: string;
}

export interface UploadStatus {
  status: 'queued' | 'uploading' | 'completed' | 'failed';
  percent?: number;
  error?: string;
}

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected';
