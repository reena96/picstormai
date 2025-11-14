/**
 * Download Service - Photo Download API calls
 * Story 3.5: Individual Photo Download
 */

import apiService from './api';

export interface DownloadUrlResponse {
  url: string;
  filename: string;
  fileSize: number;
  expiresAt: string;
}

class DownloadService {
  /**
   * Get presigned download URL for a photo
   * GET /api/photos/{photoId}/download
   */
  async getDownloadUrl(photoId: string): Promise<DownloadUrlResponse> {
    const response = await apiService.getInstance().get<DownloadUrlResponse>(`/photos/${photoId}/download`);
    return response.data;
  }

  /**
   * Download photo to browser (Web)
   * Creates a temporary anchor element to trigger browser download
   */
  async downloadPhotoWeb(photoId: string): Promise<void> {
    const downloadData = await this.getDownloadUrl(photoId);

    // Create temporary anchor element to trigger download
    const link = document.createElement('a');
    link.href = downloadData.url;
    link.download = downloadData.filename;
    link.target = '_blank';
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();

    // Clean up after a short delay to ensure download starts
    setTimeout(() => {
      document.body.removeChild(link);
    }, 100);
  }

  /**
   * Download photo to device (Mobile)
   * For web build, this falls back to browser download
   * For native builds, this would use platform-specific APIs
   */
  async downloadPhotoMobile(photoId: string): Promise<void> {
    // For MVP on web, use the same browser download
    // In a full native app, this would use react-native-fs or expo-file-system
    return this.downloadPhotoWeb(photoId);
  }

  /**
   * Universal download method - detects platform automatically
   */
  async downloadPhoto(photoId: string): Promise<void> {
    // Platform detection happens at import time, so we just use web download
    // In a native build, Platform.OS would be 'ios' or 'android'
    return this.downloadPhotoWeb(photoId);
  }

  /**
   * Download multiple photos as ZIP file
   * Story 3.6: Batch Photo Download
   * POST /api/photos/download-batch
   *
   * @param photoIds Array of photo IDs to download (max 50)
   * @throws Error if photoIds is empty, exceeds 50 items, or download fails
   */
  async downloadBatch(photoIds: string[]): Promise<void> {
    if (photoIds.length === 0) {
      throw new Error('No photos selected');
    }
    if (photoIds.length > 50) {
      throw new Error('Maximum 50 photos per download');
    }

    try {
      // Request ZIP file as blob
      const response = await apiService.getInstance().post<Blob>(
        '/photos/download-batch',
        { photoIds },
        {
          responseType: 'blob',
          timeout: 120000, // 2 minute timeout for large ZIPs
        }
      );

      // Get filename from Content-Disposition header or generate default
      const contentDisposition = response.headers['content-disposition'];
      let filename = `photos-${new Date().toISOString().split('T')[0]}-${photoIds.length}-items.zip`;

      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+)"/);
        if (filenameMatch) {
          filename = filenameMatch[1];
        }
      }

      // Create download link and trigger download (web only)
      const blob = response.data;
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.target = '_blank';
      link.style.display = 'none';
      document.body.appendChild(link);
      link.click();

      // Clean up
      setTimeout(() => {
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      }, 100);
    } catch (error: any) {
      console.error('Batch download failed:', error);

      // Handle specific error cases
      if (error.response?.status === 403) {
        throw new Error("You don't have permission to download these photos.");
      } else if (error.response?.status === 404) {
        throw new Error('Some photos are no longer available.');
      } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
        throw new Error('Download preparation timed out. Try selecting fewer photos.');
      } else if (error.response?.status === 400) {
        throw new Error('Invalid request. Please try selecting fewer photos.');
      }

      throw new Error('Download failed. Check your connection and try again.');
    }
  }
}

export const downloadService = new DownloadService();
export default downloadService;
