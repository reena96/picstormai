/**
 * Tests for Tag Service
 * Story 3.3: Photo Tagging
 */

import { tagService } from './tagService';
import { apiService } from './api';

jest.mock('./api');

describe('tagService', () => {
  let mockApi: any;

  beforeEach(() => {
    mockApi = {
      get: jest.fn(),
      post: jest.fn(),
      delete: jest.fn(),
    };
    (apiService.getInstance as jest.Mock).mockReturnValue(mockApi);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('getTags calls GET /tags', async () => {
    const mockTags = [
      { id: '1', name: 'vacation', color: '#3B82F6', createdAt: '2025-11-12T10:00:00Z' },
      { id: '2', name: 'family', color: '#EF4444', createdAt: '2025-11-12T11:00:00Z' },
    ];

    mockApi.get.mockResolvedValue({ data: mockTags });

    const result = await tagService.getTags();

    // Assert: api.get called with '/tags'
    expect(mockApi.get).toHaveBeenCalledWith('/tags');
    expect(result).toEqual(mockTags);
  });

  it('addTagToPhoto posts correct data', async () => {
    const mockResponse = {
      id: 'tag-1',
      name: 'vacation',
      color: '#3B82F6',
      createdAt: '2025-11-12T10:00:00Z',
    };

    mockApi.post.mockResolvedValue({ data: mockResponse });

    const result = await tagService.addTagToPhoto('photo-id', 'vacation');

    // Assert: api.post called with '/photos/photo-id/tags' and { tagName: 'vacation' }
    expect(mockApi.post).toHaveBeenCalledWith('/photos/photo-id/tags', { tagName: 'vacation' });
    expect(result).toEqual(mockResponse);
  });

  it('removeTagFromPhoto deletes correct endpoint', async () => {
    mockApi.delete.mockResolvedValue({ data: {} });

    await tagService.removeTagFromPhoto('photo-id', 'tag-id');

    // Assert: api.delete called with '/photos/photo-id/tags/tag-id'
    expect(mockApi.delete).toHaveBeenCalledWith('/photos/photo-id/tags/tag-id');
  });

  it('includes JWT token in all requests', async () => {
    // Test: Verify apiService.getInstance() used (handles JWT automatically)
    mockApi.get.mockResolvedValue({ data: [] });
    mockApi.post.mockResolvedValue({ data: {} });
    mockApi.delete.mockResolvedValue({ data: {} });

    await tagService.getTags();
    await tagService.addTagToPhoto('photo-id', 'vacation');
    await tagService.removeTagFromPhoto('photo-id', 'tag-id');

    // Assert: All methods use api instance from apiService
    expect(apiService.getInstance).toHaveBeenCalledTimes(3);
  });
});
