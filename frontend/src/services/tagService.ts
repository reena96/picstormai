/**
 * Tag Service
 * Handles tag-related API calls
 * Story 3.3: Photo Tagging
 */

import { apiService } from './api';

export interface Tag {
  id: string;
  name: string;
  color: string;
  createdAt: string;
}

export interface AddTagRequest {
  tagName: string;
}

export interface AddTagResponse {
  id: string;
  name: string;
  color: string;
  createdAt: string;
}

class TagService {
  /**
   * Get all tags for current user
   * GET /api/tags
   */
  async getTags(): Promise<Tag[]> {
    try {
      const api = apiService.getInstance();
      const response = await api.get<Tag[]>('/tags');
      return response.data;
    } catch (error) {
      console.error('Failed to get tags:', error);
      throw new Error('Failed to load tags');
    }
  }

  /**
   * Add tag to photo (find-or-create)
   * POST /api/photos/{photoId}/tags
   */
  async addTagToPhoto(photoId: string, tagName: string): Promise<Tag> {
    try {
      const api = apiService.getInstance();
      const response = await api.post<AddTagResponse>(
        `/photos/${photoId}/tags`,
        { tagName }
      );
      return response.data;
    } catch (error: any) {
      console.error('Failed to add tag:', error);

      // Handle specific error cases
      if (error.response?.status === 400) {
        throw new Error('Maximum 10 tags per photo');
      }
      if (error.response?.status === 404) {
        throw new Error('Photo not found');
      }
      if (error.response?.status === 403) {
        throw new Error('You do not have permission to tag this photo');
      }

      throw new Error('Failed to add tag');
    }
  }

  /**
   * Remove tag from photo
   * DELETE /api/photos/{photoId}/tags/{tagId}
   */
  async removeTagFromPhoto(photoId: string, tagId: string): Promise<void> {
    try {
      const api = apiService.getInstance();
      await api.delete(`/photos/${photoId}/tags/${tagId}`);
    } catch (error: any) {
      console.error('Failed to remove tag:', error);

      // Handle specific error cases
      if (error.response?.status === 404) {
        throw new Error('Photo or tag not found');
      }
      if (error.response?.status === 403) {
        throw new Error('You do not have permission to modify this photo');
      }

      throw new Error('Failed to remove tag');
    }
  }
}

export const tagService = new TagService();
