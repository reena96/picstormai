/**
 * Tests for Download Service
 * Story 3.5: Individual Photo Download
 * Story 3.6: Batch Photo Download (ZIP)
 */

import { downloadService } from './downloadService';
import apiService from './api';

jest.mock('./api');

describe('downloadService', () => {
  let mockApi: any;

  beforeEach(() => {
    mockApi = {
      get: jest.fn(),
      post: jest.fn(),
    };
    (apiService.get as jest.Mock) = mockApi.get;
    (apiService.post as jest.Mock) = mockApi.post;

    // Mock document.createElement and appendChild for browser download tests
    document.createElement = jest.fn().mockImplementation((tag) => {
      if (tag === 'a') {
        return {
          href: '',
          download: '',
          target: '',
          style: { display: '' },
          click: jest.fn(),
          remove: jest.fn(),
        };
      }
      return {};
    });

    document.body.appendChild = jest.fn();
    document.body.removeChild = jest.fn();

    // Mock URL.createObjectURL and revokeObjectURL
    global.URL.createObjectURL = jest.fn(() => 'blob:http://localhost/mock-blob-url');
    global.URL.revokeObjectURL = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('getDownloadUrl', () => {
    it('should call GET /photos/{photoId}/download', async () => {
      const mockResponse = {
        url: 'https://s3.amazonaws.com/presigned-url',
        filename: 'test-photo.jpg',
        fileSize: 1024000,
        expiresAt: '2025-11-12T12:05:00Z',
      };

      mockApi.get.mockResolvedValue({ data: mockResponse });

      const result = await downloadService.getDownloadUrl('photo-123');

      expect(mockApi.get).toHaveBeenCalledWith('/photos/photo-123/download');
      expect(result).toEqual(mockResponse);
    });

    it('should return presigned URL with metadata', async () => {
      const mockResponse = {
        url: 'https://s3.amazonaws.com/presigned-url?signature=abc',
        filename: 'vacation.jpg',
        fileSize: 2048576,
        expiresAt: '2025-11-12T12:05:00Z',
      };

      mockApi.get.mockResolvedValue({ data: mockResponse });

      const result = await downloadService.getDownloadUrl('photo-456');

      expect(result.url).toBe(mockResponse.url);
      expect(result.filename).toBe('vacation.jpg');
      expect(result.fileSize).toBe(2048576);
      expect(result.expiresAt).toBe('2025-11-12T12:05:00Z');
    });

    it('should handle API errors gracefully', async () => {
      const mockError = new Error('Network error');
      mockApi.get.mockRejectedValue(mockError);

      await expect(downloadService.getDownloadUrl('photo-789'))
        .rejects.toThrow('Network error');

      expect(mockApi.get).toHaveBeenCalledWith('/photos/photo-789/download');
    });

    it('should handle 404 not found error', async () => {
      const mockError = {
        response: {
          status: 404,
          data: { message: 'Photo not found' },
        },
      };
      mockApi.get.mockRejectedValue(mockError);

      await expect(downloadService.getDownloadUrl('nonexistent-photo'))
        .rejects.toEqual(mockError);
    });

    it('should handle 403 unauthorized error', async () => {
      const mockError = {
        response: {
          status: 403,
          data: { message: 'Photo does not belong to user' },
        },
      };
      mockApi.get.mockRejectedValue(mockError);

      await expect(downloadService.getDownloadUrl('unauthorized-photo'))
        .rejects.toEqual(mockError);
    });
  });

  describe('downloadPhotoWeb', () => {
    it('should create download link with presigned URL', async () => {
      const mockResponse = {
        url: 'https://s3.amazonaws.com/presigned-url',
        filename: 'test-photo.jpg',
        fileSize: 1024000,
        expiresAt: '2025-11-12T12:05:00Z',
      };

      mockApi.get.mockResolvedValue({ data: mockResponse });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };

      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      await downloadService.downloadPhotoWeb('photo-123');

      expect(mockLink.href).toBe(mockResponse.url);
      expect(mockLink.download).toBe(mockResponse.filename);
      expect(mockLink.target).toBe('_blank');
      expect(mockLink.click).toHaveBeenCalled();
      expect(document.body.appendChild).toHaveBeenCalledWith(mockLink);
    });

    it('should trigger browser download', async () => {
      const mockResponse = {
        url: 'https://s3.amazonaws.com/presigned-url',
        filename: 'vacation.jpg',
        fileSize: 2048576,
        expiresAt: '2025-11-12T12:05:00Z',
      };

      mockApi.get.mockResolvedValue({ data: mockResponse });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };

      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      await downloadService.downloadPhotoWeb('photo-456');

      expect(mockLink.click).toHaveBeenCalledTimes(1);
    });

    it('should clean up link element after download', async () => {
      const mockResponse = {
        url: 'https://s3.amazonaws.com/presigned-url',
        filename: 'test.jpg',
        fileSize: 1024,
        expiresAt: '2025-11-12T12:05:00Z',
      };

      mockApi.get.mockResolvedValue({ data: mockResponse });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };

      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      // Use fake timers to test setTimeout cleanup
      jest.useFakeTimers();

      await downloadService.downloadPhotoWeb('photo-789');

      expect(document.body.appendChild).toHaveBeenCalledWith(mockLink);

      // Fast-forward time to trigger setTimeout
      jest.advanceTimersByTime(100);

      expect(document.body.removeChild).toHaveBeenCalledWith(mockLink);

      jest.useRealTimers();
    });

    it('should handle special characters in filename', async () => {
      const mockResponse = {
        url: 'https://s3.amazonaws.com/presigned-url',
        filename: 'My Photo (1).jpg',
        fileSize: 1024000,
        expiresAt: '2025-11-12T12:05:00Z',
      };

      mockApi.get.mockResolvedValue({ data: mockResponse });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };

      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      await downloadService.downloadPhotoWeb('photo-special');

      expect(mockLink.download).toBe('My Photo (1).jpg');
    });
  });

  describe('downloadPhoto', () => {
    it('should use web download method', async () => {
      const mockResponse = {
        url: 'https://s3.amazonaws.com/presigned-url',
        filename: 'test.jpg',
        fileSize: 1024,
        expiresAt: '2025-11-12T12:05:00Z',
      };

      mockApi.get.mockResolvedValue({ data: mockResponse });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };

      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      await downloadService.downloadPhoto('photo-123');

      expect(mockLink.click).toHaveBeenCalled();
    });
  });

  describe('downloadPhotoMobile', () => {
    it('should fall back to web download for MVP', async () => {
      const mockResponse = {
        url: 'https://s3.amazonaws.com/presigned-url',
        filename: 'mobile-photo.jpg',
        fileSize: 1024000,
        expiresAt: '2025-11-12T12:05:00Z',
      };

      mockApi.get.mockResolvedValue({ data: mockResponse });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };

      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      await downloadService.downloadPhotoMobile('photo-mobile');

      expect(mockLink.click).toHaveBeenCalled();
    });
  });

  // Story 3.6: Batch Photo Download (ZIP) tests
  describe('downloadBatch', () => {
    it('should throw error when photoIds is empty', async () => {
      await expect(downloadService.downloadBatch([])).rejects.toThrow('No photos selected');
    });

    it('should throw error when photoIds exceeds 50', async () => {
      const tooManyIds = Array.from({ length: 51 }, (_, i) => `photo-${i}`);
      await expect(downloadService.downloadBatch(tooManyIds)).rejects.toThrow('Maximum 50 photos per download');
    });

    it('should call POST /photos/download-batch with photoIds', async () => {
      const photoIds = ['photo-1', 'photo-2', 'photo-3'];
      const mockBlob = new Blob(['mock zip content'], { type: 'application/zip' });

      mockApi.post.mockResolvedValue({
        data: mockBlob,
        headers: {
          'content-disposition': 'attachment; filename="photos-2025-11-12-3-items.zip"'
        }
      });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };
      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      await downloadService.downloadBatch(photoIds);

      expect(mockApi.post).toHaveBeenCalledWith(
        '/photos/download-batch',
        { photoIds },
        expect.objectContaining({
          responseType: 'blob',
          timeout: 120000,
        })
      );
    });

    it('should create blob URL and trigger download', async () => {
      const photoIds = ['photo-1', 'photo-2'];
      const mockBlob = new Blob(['mock zip'], { type: 'application/zip' });

      mockApi.post.mockResolvedValue({
        data: mockBlob,
        headers: {
          'content-disposition': 'attachment; filename="photos-2025-11-12-2-items.zip"'
        }
      });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };
      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      await downloadService.downloadBatch(photoIds);

      expect(global.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
      expect(mockLink.href).toBe('blob:http://localhost/mock-blob-url');
      expect(mockLink.download).toBe('photos-2025-11-12-2-items.zip');
      expect(mockLink.click).toHaveBeenCalled();
    });

    it('should cleanup blob URL after download', async () => {
      const photoIds = ['photo-1'];
      const mockBlob = new Blob(['zip'], { type: 'application/zip' });

      mockApi.post.mockResolvedValue({
        data: mockBlob,
        headers: {}
      });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };
      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      jest.useFakeTimers();

      await downloadService.downloadBatch(photoIds);

      jest.advanceTimersByTime(100);

      expect(global.URL.revokeObjectURL).toHaveBeenCalledWith('blob:http://localhost/mock-blob-url');
      expect(document.body.removeChild).toHaveBeenCalledWith(mockLink);

      jest.useRealTimers();
    });

    it('should handle 403 unauthorized error', async () => {
      const photoIds = ['photo-1'];
      mockApi.post.mockRejectedValue({
        response: { status: 403 }
      });

      await expect(downloadService.downloadBatch(photoIds))
        .rejects.toThrow("You don't have permission to download these photos.");
    });

    it('should handle 404 not found error', async () => {
      const photoIds = ['photo-1'];
      mockApi.post.mockRejectedValue({
        response: { status: 404 }
      });

      await expect(downloadService.downloadBatch(photoIds))
        .rejects.toThrow('Some photos are no longer available.');
    });

    it('should handle timeout error', async () => {
      const photoIds = ['photo-1'];
      mockApi.post.mockRejectedValue({
        code: 'ECONNABORTED'
      });

      await expect(downloadService.downloadBatch(photoIds))
        .rejects.toThrow('Download preparation timed out. Try selecting fewer photos.');
    });

    it('should handle 400 bad request error', async () => {
      const photoIds = ['photo-1'];
      mockApi.post.mockRejectedValue({
        response: { status: 400 }
      });

      await expect(downloadService.downloadBatch(photoIds))
        .rejects.toThrow('Invalid request. Please try selecting fewer photos.');
    });

    it('should handle generic network errors', async () => {
      const photoIds = ['photo-1'];
      mockApi.post.mockRejectedValue(new Error('Network failure'));

      await expect(downloadService.downloadBatch(photoIds))
        .rejects.toThrow('Download failed. Check your connection and try again.');
    });

    it('should extract filename from Content-Disposition header', async () => {
      const photoIds = ['photo-1', 'photo-2', 'photo-3'];
      const mockBlob = new Blob(['zip'], { type: 'application/zip' });

      mockApi.post.mockResolvedValue({
        data: mockBlob,
        headers: {
          'content-disposition': 'attachment; filename="photos-2025-11-12-3-items.zip"'
        }
      });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };
      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      await downloadService.downloadBatch(photoIds);

      expect(mockLink.download).toBe('photos-2025-11-12-3-items.zip');
    });

    it('should use default filename when header missing', async () => {
      const photoIds = ['photo-1', 'photo-2'];
      const mockBlob = new Blob(['zip'], { type: 'application/zip' });

      mockApi.post.mockResolvedValue({
        data: mockBlob,
        headers: {}
      });

      const mockLink = {
        href: '',
        download: '',
        target: '',
        style: { display: '' },
        click: jest.fn(),
        remove: jest.fn(),
      };
      (document.createElement as jest.Mock).mockReturnValue(mockLink);

      await downloadService.downloadBatch(photoIds);

      expect(mockLink.download).toMatch(/^photos-\d{4}-\d{2}-\d{2}-2-items\.zip$/);
    });
  });
});
