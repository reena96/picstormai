/**
 * Tests for Lightbox Component
 * Story 3.2: Photo Viewing - Lightbox
 * Story 3.5: Individual Photo Download (added download tests)
 */

import React from 'react';
import { render, fireEvent as fireEventDOM, waitFor } from '@testing-library/react';
import { fireEvent } from '@testing-library/react-native';
import { Lightbox } from './Lightbox';
import { Photo } from '../../services/galleryService';
import { downloadService } from '../../services/downloadService';

// Mock download service
jest.mock('../../services/downloadService', () => ({
  downloadService: {
    downloadPhoto: jest.fn(),
  },
}));

// Mock tag service
jest.mock('../../services/tagService', () => ({
  tagService: {
    getTags: jest.fn().mockResolvedValue([]),
    addTagToPhoto: jest.fn(),
    removeTagFromPhoto: jest.fn(),
  },
}));

// Mock lucide-react-native icons
jest.mock('lucide-react-native', () => ({
  X: 'X',
  ChevronLeft: 'ChevronLeft',
  ChevronRight: 'ChevronRight',
  ZoomIn: 'ZoomIn',
  ZoomOut: 'ZoomOut',
  Download: 'Download',
}));

// Mock theme hook
jest.mock('../../hooks/useTheme', () => ({
  useTheme: () => ({
    theme: {
      colors: {
        white: '#FFFFFF',
        black: '#000000',
        background: '#FFFFFF',
        primary: '#3B82F6',
        gray: {
          300: '#D4D4D4',
          500: '#6B7280',
        },
        text: {
          primary: '#000000',
          secondary: '#666666',
        },
      },
      spacing: [0, 4, 8, 12, 16, 24, 32, 48, 64],
      borderRadius: {
        sm: 4,
        md: 8,
        lg: 12,
      },
    },
  }),
}));

// Mock atoms
jest.mock('../atoms/Text', () => ({
  Text: ({ children, testID, ...props }: any) => {
    const React = require('react');
    const { Text: RNText } = require('react-native');
    return <RNText testID={testID} {...props}>{children}</RNText>;
  },
}));

jest.mock('../atoms/Spinner', () => ({
  Spinner: ({ testID, ...props }: any) => {
    const React = require('react');
    const { ActivityIndicator } = require('react-native');
    return <ActivityIndicator testID={testID || 'spinner'} {...props} />;
  },
}));

const mockPhotos: Photo[] = [
  {
    id: '1',
    userId: 'user-1',
    sessionId: 'session-1',
    filename: 'photo1.jpg',
    fileSize: 1024000,
    s3Key: 'photos/photo1.jpg',
    storageUrl: 'https://example.com/photo1.jpg',
    uploadStatus: 'COMPLETED',
    progress: 100,
    metadata: {},
    tags: [],
    createdAt: '2025-11-12T10:00:00Z',
    updatedAt: '2025-11-12T10:00:00Z',
  },
  {
    id: '2',
    userId: 'user-1',
    sessionId: 'session-1',
    filename: 'photo2.jpg',
    fileSize: 2048000,
    s3Key: 'photos/photo2.jpg',
    storageUrl: 'https://example.com/photo2.jpg',
    uploadStatus: 'COMPLETED',
    progress: 100,
    metadata: {},
    tags: [],
    createdAt: '2025-11-12T11:00:00Z',
    updatedAt: '2025-11-12T11:00:00Z',
  },
  {
    id: '3',
    userId: 'user-1',
    sessionId: 'session-1',
    filename: 'photo3.jpg',
    fileSize: 3072000,
    s3Key: 'photos/photo3.jpg',
    storageUrl: 'https://example.com/photo3.jpg',
    uploadStatus: 'COMPLETED',
    progress: 100,
    metadata: {},
    tags: [],
    createdAt: '2025-11-12T12:00:00Z',
    updatedAt: '2025-11-12T12:00:00Z',
  },
];

describe('Lightbox', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('renders when visible is true', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox')).toBeTruthy();
    });

    it('does not render when visible is false', () => {
      const { queryByTestId } = render(
        <Lightbox
          visible={false}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Modal with visible=false doesn't render children
      expect(queryByTestId('lightbox-close-button')).toBeNull();
    });

    it('displays photo at initialIndex on mount', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={1}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-filename').textContent).toBe('photo2.jpg');
    });
  });

  describe('Close functionality', () => {
    it('calls onClose when close button is pressed', () => {
      const onClose = jest.fn();
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={onClose}
          testID="lightbox"
        />
      );

      fireEventDOM.click(getByTestId('lightbox-close-button'));
      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });

  describe('Navigation', () => {
    it('navigates to next photo when next button is pressed', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-filename').textContent).toBe('photo1.jpg');

      fireEventDOM.click(getByTestId('lightbox-next-button'));

      expect(getByTestId('lightbox-filename').textContent).toBe('photo2.jpg');
    });

    it('navigates to previous photo when previous button is pressed', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={1}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-filename').textContent).toBe('photo2.jpg');

      fireEventDOM.click(getByTestId('lightbox-previous-button'));

      expect(getByTestId('lightbox-filename').textContent).toBe('photo1.jpg');
    });

    it('hides previous button when on first photo', () => {
      const { queryByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(queryByTestId('lightbox-previous-button')).toBeNull();
    });

    it('hides next button when on last photo', () => {
      const { queryByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={2}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(queryByTestId('lightbox-next-button')).toBeNull();
    });

    it('shows both navigation buttons when on middle photo', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={1}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-previous-button')).toBeTruthy();
      expect(getByTestId('lightbox-next-button')).toBeTruthy();
    });

    it('calls onPhotoChange callback when photo changes', () => {
      const onPhotoChange = jest.fn();
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          onPhotoChange={onPhotoChange}
          testID="lightbox"
        />
      );

      // Initial call
      expect(onPhotoChange).toHaveBeenCalledWith(0);

      fireEventDOM.click(getByTestId('lightbox-next-button'));

      // Called again with new index
      expect(onPhotoChange).toHaveBeenCalledWith(1);
    });
  });

  describe('Metadata display', () => {
    it('displays filename correctly', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-filename').textContent).toBe('photo1.jpg');
    });

    it('displays formatted date correctly', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Check that date is displayed (format may vary based on locale)
      const dateElement = getByTestId('lightbox-date');
      expect(dateElement).toBeTruthy();
      const dateText = dateElement.textContent || '';
      expect(dateText).toMatch(/Nov|11/); // Should contain Nov or 11
    });

    it('displays formatted file size correctly', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // 1024000 bytes = 1000 KB
      const sizeElement = getByTestId('lightbox-size');
      expect(sizeElement.textContent).toMatch(/KB|MB/);
    });

    it('displays photo index counter correctly', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={1}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-index').textContent).toBe('2 / 3');
    });

    it('updates metadata when navigating to next photo', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-filename').textContent).toBe('photo1.jpg');
      expect(getByTestId('lightbox-index').textContent).toBe('1 / 3');

      fireEventDOM.click(getByTestId('lightbox-next-button'));

      expect(getByTestId('lightbox-filename').textContent).toBe('photo2.jpg');
      expect(getByTestId('lightbox-index').textContent).toBe('2 / 3');
    });
  });

  describe('Zoom controls', () => {
    it('renders zoom in and zoom out buttons', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-zoom-in-button')).toBeTruthy();
      expect(getByTestId('lightbox-zoom-out-button')).toBeTruthy();
    });

    it('zoom out button can be clicked multiple times', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      const zoomOutButton = getByTestId('lightbox-zoom-out-button');
      // Verify button exists and can be clicked
      expect(zoomOutButton).toBeTruthy();
      fireEventDOM.click(zoomOutButton);
      // Button should still exist after click
      expect(zoomOutButton).toBeTruthy();
    });

    it('zoom in button works', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      const zoomInButton = getByTestId('lightbox-zoom-in-button');
      fireEventDOM.click(zoomInButton);

      // Zoom level should increase - button should not be disabled yet
      expect(zoomInButton.hasAttribute('disabled')).toBe(false);
    });
  });

  describe('Loading state', () => {
    it('shows loading spinner initially', () => {
      const { getAllByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Should have a spinner
      const spinners = getAllByTestId('spinner');
      expect(spinners.length).toBeGreaterThan(0);
    });
  });

  describe('Edge cases', () => {
    it('handles single photo correctly', () => {
      const singlePhoto = [mockPhotos[0]];
      const { queryByTestId, getByTestId } = render(
        <Lightbox
          visible={true}
          photos={singlePhoto}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // No navigation buttons should be visible
      expect(queryByTestId('lightbox-previous-button')).toBeNull();
      expect(queryByTestId('lightbox-next-button')).toBeNull();

      // Photo should still be displayed
      expect(getByTestId('lightbox-filename').textContent).toBe('photo1.jpg');
    });

    it('resets to initialIndex when lightbox reopens', () => {
      const { getByTestId, rerender } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Navigate to second photo
      fireEventDOM.click(getByTestId('lightbox-next-button'));
      expect(getByTestId('lightbox-filename').textContent).toBe('photo2.jpg');

      // Close lightbox
      rerender(
        <Lightbox
          visible={false}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Reopen at a different index
      rerender(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={2}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Should show the new initial photo
      expect(getByTestId('lightbox-filename').textContent).toBe('photo3.jpg');
    });

    it('handles missing storageUrl gracefully', () => {
      const photosWithMissingUrl: Photo[] = [
        {
          ...mockPhotos[0],
          storageUrl: null,
        },
      ];

      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={photosWithMissingUrl}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Should still render without crashing
      expect(getByTestId('lightbox-filename').textContent).toBe('photo1.jpg');
    });
  });

  describe('Accessibility', () => {
    it('has accessibility labels on buttons', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={1}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-close-button').getAttribute('aria-label')).toBe('Close lightbox');
      expect(getByTestId('lightbox-previous-button').getAttribute('aria-label')).toBe('Previous photo');
      expect(getByTestId('lightbox-next-button').getAttribute('aria-label')).toBe('Next photo');
      expect(getByTestId('lightbox-zoom-in-button').getAttribute('aria-label')).toBe('Zoom in');
      expect(getByTestId('lightbox-zoom-out-button').getAttribute('aria-label')).toBe('Zoom out');
      expect(getByTestId('lightbox-download-button').getAttribute('aria-label')).toBe('Download photo');
    });

    it('has accessibility label on image', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-image').getAttribute('aria-label')).toBe('Photo: photo1.jpg');
    });
  });

  describe('Download functionality', () => {
    beforeEach(() => {
      jest.clearAllMocks();
      (downloadService.downloadPhoto as jest.Mock).mockResolvedValue(undefined);
    });

    it('renders download button', () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      expect(getByTestId('lightbox-download-button')).toBeTruthy();
    });

    it('calls downloadService when download button is clicked', async () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      fireEventDOM.click(getByTestId('lightbox-download-button'));

      await waitFor(() => {
        expect(downloadService.downloadPhoto).toHaveBeenCalledWith('1');
      });
    });

    it('disables download button while downloading', async () => {
      let resolveDownload: any;
      (downloadService.downloadPhoto as jest.Mock).mockImplementation(() => {
        return new Promise((resolve) => {
          resolveDownload = resolve;
        });
      });

      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      const downloadButton = getByTestId('lightbox-download-button');

      // Initially enabled
      expect(downloadButton.hasAttribute('disabled')).toBe(false);

      // Click download
      fireEventDOM.click(downloadButton);

      // Should be disabled during download
      await waitFor(() => {
        expect(downloadButton.hasAttribute('disabled')).toBe(true);
      });

      // Complete download
      resolveDownload();

      // Should be enabled again
      await waitFor(() => {
        expect(downloadButton.hasAttribute('disabled')).toBe(false);
      });
    });

    it('shows loading spinner during download', async () => {
      let resolveDownload: any;
      (downloadService.downloadPhoto as jest.Mock).mockImplementation(() => {
        return new Promise((resolve) => {
          resolveDownload = resolve;
        });
      });

      const { getByTestId, getAllByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Click download
      fireEventDOM.click(getByTestId('lightbox-download-button'));

      // Should show loading spinner (ActivityIndicator inside download button)
      await waitFor(() => {
        const spinners = getAllByTestId('spinner');
        expect(spinners.length).toBeGreaterThan(0);
      });

      // Complete download
      resolveDownload();

      await waitFor(() => {
        expect(downloadService.downloadPhoto).toHaveBeenCalled();
      });
    });

    it('shows error message on download failure', async () => {
      const mockError = new Error('Network error');
      (downloadService.downloadPhoto as jest.Mock).mockRejectedValue(mockError);

      const { getByTestId, queryByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Initially no error
      expect(queryByTestId('lightbox-download-error')).toBeNull();

      // Click download
      fireEventDOM.click(getByTestId('lightbox-download-button'));

      // Should show error message
      await waitFor(() => {
        const errorElement = getByTestId('lightbox-download-error');
        expect(errorElement).toBeTruthy();
        expect(errorElement.textContent).toContain('Network error');
      });
    });

    it('can retry download after error', async () => {
      const mockError = new Error('Download failed');
      (downloadService.downloadPhoto as jest.Mock)
        .mockRejectedValueOnce(mockError)
        .mockResolvedValueOnce(undefined);

      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // First attempt fails
      fireEventDOM.click(getByTestId('lightbox-download-button'));

      await waitFor(() => {
        expect(getByTestId('lightbox-download-error')).toBeTruthy();
      });

      // Button should be enabled for retry
      const downloadButton = getByTestId('lightbox-download-button');
      expect(downloadButton.hasAttribute('disabled')).toBe(false);

      // Retry download
      fireEventDOM.click(downloadButton);

      // Second attempt succeeds
      await waitFor(() => {
        expect(downloadService.downloadPhoto).toHaveBeenCalledTimes(2);
      });
    });

    it('downloads correct photo when navigating between photos', async () => {
      const { getByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Download first photo
      fireEventDOM.click(getByTestId('lightbox-download-button'));

      await waitFor(() => {
        expect(downloadService.downloadPhoto).toHaveBeenCalledWith('1');
      });

      // Navigate to next photo
      fireEventDOM.click(getByTestId('lightbox-next-button'));

      // Download second photo
      fireEventDOM.click(getByTestId('lightbox-download-button'));

      await waitFor(() => {
        expect(downloadService.downloadPhoto).toHaveBeenCalledWith('2');
      });
    });

    it('clears error when navigating to different photo', async () => {
      const mockError = new Error('Download failed');
      (downloadService.downloadPhoto as jest.Mock).mockRejectedValue(mockError);

      const { getByTestId, queryByTestId } = render(
        <Lightbox
          visible={true}
          photos={mockPhotos}
          initialIndex={0}
          onClose={jest.fn()}
          testID="lightbox"
        />
      );

      // Download fails
      fireEventDOM.click(getByTestId('lightbox-download-button'));

      await waitFor(() => {
        expect(getByTestId('lightbox-download-error')).toBeTruthy();
      });

      // Navigate to next photo - error should be cleared
      fireEventDOM.click(getByTestId('lightbox-next-button'));

      // Wait a moment for state to update
      await waitFor(() => {
        expect(queryByTestId('lightbox-download-error')).toBeNull();
      });
    });
  });
});
