/**
 * Gallery Service - Photo Gallery API calls
 * Story 3.1: Photo Gallery
 * Story 3.3: Photo Tagging (added tags to Photo interface)
 */

import apiService from './api';

export interface Tag {
  id: string;
  name: string;
  color: string;
  createdAt: string;
}

export interface Photo {
  id: string;
  userId: string;
  sessionId: string;
  filename: string;
  fileSize: number;
  s3Key: string | null;
  storageUrl: string | null;
  uploadStatus: 'PENDING' | 'UPLOADING' | 'COMPLETED' | 'FAILED';
  progress: number;
  metadata: Record<string, any>;
  tags: Tag[]; // NEW: Tags array instead of tagIds
  createdAt: string;
  updatedAt: string;
}

export interface PhotoDTO {
  id: string;
  userId: string;
  sessionId: string;
  filename: string;
  fileSize: number;
  s3Key: string | null;
  storageUrl: string | null;
  uploadStatus: 'PENDING' | 'UPLOADING' | 'COMPLETED' | 'FAILED';
  progress: number;
  metadata: Record<string, any>;
  tags: Tag[]; // NEW: Tags array instead of tagIds
  createdAt: string;
  updatedAt: string;
}

export type SortOption = 'createdAt,desc' | 'createdAt,asc' | 'fileSize,desc' | 'filename,asc';

export interface GetPhotosParams {
  page?: number;
  size?: number;
  sort?: SortOption;
  tagIds?: string[]; // Story 3.4: Optional tag filter (AND logic)
}

export interface GetPhotosResponse {
  photos: Photo[];
  totalElements?: number;
  totalPages?: number;
  currentPage?: number;
}

/**
 * Get photos for current user with pagination, sorting, and optional tag filtering
 * Story 3.4: Added tagIds parameter for filtering by tags (AND logic)
 */
export const getPhotos = async (params: GetPhotosParams = {}): Promise<Photo[]> => {
  const { page = 0, size = 30, sort = 'createdAt,desc', tagIds = [] } = params;

  const queryParams: any = { page, size, sort };

  // Add tagIds if provided (sent as multiple query params: ?tagIds=uuid1&tagIds=uuid2)
  if (tagIds.length > 0) {
    queryParams.tagIds = tagIds;
  }

  const response = await apiService.getInstance().get<Photo[]>('/photos', {
    params: queryParams,
    paramsSerializer: (params) => {
      // Manually serialize to handle array params correctly
      const parts: string[] = [];
      Object.keys(params).forEach((key) => {
        const value = params[key];
        if (Array.isArray(value)) {
          value.forEach((v) => parts.push(`${key}=${encodeURIComponent(v)}`));
        } else {
          parts.push(`${key}=${encodeURIComponent(value)}`);
        }
      });
      return parts.join('&');
    },
  });

  return response.data;
};

/**
 * Get photo by ID
 */
export const getPhotoById = async (photoId: string): Promise<Photo> => {
  const response = await apiService.getInstance().get<Photo>(`/photos/${photoId}`);
  return response.data;
};

/**
 * Get download URL for a photo
 */
export const getPhotoDownloadUrl = async (photoId: string): Promise<string> => {
  const response = await apiService.getInstance().get<{ downloadUrl: string }>(`/photos/${photoId}/download`);
  return response.data.downloadUrl;
};

/**
 * Download multiple photos as ZIP
 */
export const downloadPhotosAsZip = async (photoIds: string[]): Promise<Blob> => {
  const response = await apiService.getInstance().post('/photos/download-batch', photoIds, {
    responseType: 'blob'
  });
  return response.data;
};

/**
 * Delete a photo (soft delete)
 */
export const deletePhoto = async (photoId: string): Promise<void> => {
  await apiService.getInstance().delete(`/photos/${photoId}`);
};

/**
 * Delete multiple photos (soft delete)
 */
export const deletePhotos = async (photoIds: string[]): Promise<{ deletedCount: number; totalRequested: number }> => {
  const response = await apiService.getInstance().post('/photos/delete-batch', { photoIds });
  return response.data;
};

export const galleryService = {
  getPhotos,
  getPhotoById,
  getPhotoDownloadUrl,
  downloadPhotosAsZip,
  deletePhoto,
  deletePhotos
};
