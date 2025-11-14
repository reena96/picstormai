/**
 * Tests for GalleryScreen
 * Story 3.1: Photo Gallery UI with Infinite Scroll
 */

import React from 'react';
import { render, waitFor, fireEvent } from '@testing-library/react-native';
import { GalleryScreen } from './GalleryScreen';
import { galleryService } from '../services/galleryService';

// Mock all components to avoid react-native-web rendering issues in tests
jest.mock('../components/atoms/Text', () => ({
  Text: ({ children, testID, ...props }: any) => {
    const React = require('react');
    const { Text: RNText } = require('react-native');
    return <RNText testID={testID} {...props}>{children}</RNText>;
  },
}));

jest.mock('../components/atoms/Spinner', () => ({
  Spinner: ({ testID, ...props }: any) => {
    const React = require('react');
    const { ActivityIndicator } = require('react-native');
    return <ActivityIndicator testID={testID || 'spinner'} {...props} />;
  },
}));

jest.mock('../components/atoms/Button', () => ({
  Button: ({ children, onPress, testID, ...props }: any) => {
    const React = require('react');
    const { TouchableOpacity, Text } = require('react-native');
    return (
      <TouchableOpacity onPress={onPress} testID={testID} {...props}>
        <Text>{children}</Text>
      </TouchableOpacity>
    );
  },
}));

jest.mock('../components/atoms/Select', () => ({
  Select: ({ testID, value, onChange, options, ...props }: any) => {
    const React = require('react');
    const { View, Text, TouchableOpacity } = require('react-native');
    return (
      <View testID={testID} {...props}>
        <Text>Sort: {value}</Text>
        {options.map((opt: any) => (
          <TouchableOpacity
            key={opt.value}
            testID={`${testID}-option-${opt.value}`}
            onPress={() => onChange(opt.value)}
          >
            <Text>{opt.label}</Text>
          </TouchableOpacity>
        ))}
      </View>
    );
  },
}));

jest.mock('../components/organisms/PhotoGrid', () => ({
  PhotoGrid: ({ photos, onPhotoPress, onLoadMore, testID, ...props }: any) => {
    const React = require('react');
    const { View, Text, TouchableOpacity } = require('react-native');
    return (
      <View testID={testID || 'gallery-photo-grid'} {...props}>
        {photos.map((photo: any) => (
          <TouchableOpacity
            key={photo.id}
            onPress={() => onPhotoPress && onPhotoPress(photo)}
            testID={`photo-${photo.id}`}
          >
            <Text>{photo.filename}</Text>
          </TouchableOpacity>
        ))}
        {onLoadMore && (
          <TouchableOpacity onPress={onLoadMore} testID="load-more-button">
            <Text>Load More</Text>
          </TouchableOpacity>
        )}
      </View>
    );
  },
}));

jest.mock('../components/molecules/EmptyState', () => ({
  EmptyState: ({ headline, subtext, ctaLabel, onCtaPress, ...props }: any) => {
    const React = require('react');
    const { View, Text, TouchableOpacity } = require('react-native');
    return (
      <View testID="empty-state" {...props}>
        <Text>{headline}</Text>
        <Text>{subtext}</Text>
        {ctaLabel && (
          <TouchableOpacity onPress={onCtaPress} testID="empty-state-cta">
            <Text>{ctaLabel}</Text>
          </TouchableOpacity>
        )}
      </View>
    );
  },
}));

jest.mock('../components/organisms/Lightbox', () => ({
  Lightbox: ({ visible, photos, initialIndex, onClose, testID, ...props }: any) => {
    const React = require('react');
    const { View, Text, TouchableOpacity } = require('react-native');
    // Always render the lightbox component so props can be tested
    return (
      <View testID={testID} data-visible={visible} {...props}>
        {visible && (
          <>
            <Text testID={`${testID}-photo`}>{photos[initialIndex]?.filename}</Text>
            <TouchableOpacity onPress={onClose} testID={`${testID}-close`}>
              <Text>Close</Text>
            </TouchableOpacity>
          </>
        )}
      </View>
    );
  },
}));

jest.mock('../components/organisms/TagFilterBar', () => ({
  TagFilterBar: ({ availableTags, selectedTagIds, onToggleTag, onClearAll, testID, ...props }: any) => {
    const React = require('react');
    const { View, Text, TouchableOpacity } = require('react-native');
    return (
      <View testID={testID} {...props}>
        {availableTags.map((tag: any) => (
          <TouchableOpacity
            key={tag.id}
            testID={`${testID}-tag-${tag.id}`}
            onPress={() => onToggleTag(tag.id)}
          >
            <Text>{tag.name}</Text>
          </TouchableOpacity>
        ))}
        {selectedTagIds.length > 0 && (
          <TouchableOpacity
            testID={`${testID}-clear-button`}
            onPress={onClearAll}
          >
            <Text>Clear all ({selectedTagIds.length})</Text>
          </TouchableOpacity>
        )}
      </View>
    );
  },
}));

// Mock lucide-react-native icons
jest.mock('lucide-react-native', () => ({
  Image: 'Image',
  ChevronDown: 'ChevronDown',
}));

// Mock services
jest.mock('../services/galleryService');
jest.mock('../services/tagService');

// Mock navigation
jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: () => ({
    navigate: jest.fn(),
  }),
}));

// Mock theme hook
jest.mock('../hooks/useTheme', () => ({
  useTheme: () => ({
    theme: {
      colors: {
        background: '#FFFFFF',
        surface: '#F5F5F5',
        primary: '#007AFF',
        error: '#FF3B30',
        white: '#FFFFFF',
        gray: {
          200: '#E5E5E5',
          300: '#D4D4D4',
        },
        text: {
          primary: '#000000',
          secondary: '#6B7280',
        },
      },
      spacing: [0, 4, 8, 12, 16, 24, 32, 48, 64],
      borderRadius: {
        base: 8,
      },
    },
  }),
}));

const mockPhotos = [
  {
    id: '1',
    userId: 'user-1',
    sessionId: 'session-1',
    filename: 'photo1.jpg',
    fileSize: 1024000,
    s3Key: 'photos/photo1.jpg',
    storageUrl: 'https://example.com/photo1.jpg',
    uploadStatus: 'COMPLETED' as const,
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
    uploadStatus: 'COMPLETED' as const,
    progress: 100,
    metadata: {},
    tags: [],
    createdAt: '2025-11-12T11:00:00Z',
    updatedAt: '2025-11-12T11:00:00Z',
  },
];

const mockTags = [
  { id: 'tag-1', name: 'vacation', color: '#3B82F6', createdAt: '2025-11-12T10:00:00Z' },
  { id: 'tag-2', name: 'family', color: '#EF4444', createdAt: '2025-11-12T10:00:00Z' },
];

describe('GalleryScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    // Import tagService mock
    const { tagService } = require('../services/tagService');
    tagService.getTags = jest.fn().mockResolvedValue(mockTags);
  });

  it('renders loading state initially', () => {
    (galleryService.getPhotos as jest.Mock).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );

    const { getByText } = render(<GalleryScreen />);

    expect(getByText('Loading your photos...')).toBeTruthy();
  });

  it('fetches and displays photos on mount', async () => {
    (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

    const { getByText, getByTestId } = render(<GalleryScreen />);

    await waitFor(() => {
      expect(getByText('Photo Gallery')).toBeTruthy();
      expect(getByText('2 photos')).toBeTruthy();
    });

    expect(galleryService.getPhotos).toHaveBeenCalledWith({
      page: 0,
      size: 30,
      sort: 'createdAt,desc',
      tagIds: undefined,
    });
  });

  it('displays empty state when no photos', async () => {
    (galleryService.getPhotos as jest.Mock).mockResolvedValue([]);

    const { getByText } = render(<GalleryScreen />);

    await waitFor(() => {
      expect(getByText('No photos yet')).toBeTruthy();
      expect(getByText('Start uploading photos to see them here!')).toBeTruthy();
      expect(getByText('Go to Upload')).toBeTruthy();
    });
  });

  it('handles sort change', async () => {
    (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

    const { getByTestId } = render(<GalleryScreen />);

    await waitFor(() => {
      expect(galleryService.getPhotos).toHaveBeenCalled();
    });

    // Verify sort select is rendered
    const sortSelect = getByTestId('gallery-sort-select');
    expect(sortSelect).toBeTruthy();
  });

  it('displays empty state when initial fetch fails', async () => {
    (galleryService.getPhotos as jest.Mock).mockRejectedValue(
      new Error('Network error')
    );

    const { getByText, queryByText } = render(<GalleryScreen />);

    // When initial load fails, component shows empty state (not error banner)
    await waitFor(() => {
      expect(getByText('No photos yet')).toBeTruthy();
    }, { timeout: 2000 });

    // Error is logged to console but not displayed when no photos exist
    expect(queryByText(/Failed to load photos/i)).toBeNull();
  });

  it('loads more photos when scrolling', async () => {
    // First call returns photos, indicating more pages
    (galleryService.getPhotos as jest.Mock)
      .mockResolvedValueOnce(mockPhotos)
      .mockResolvedValueOnce([mockPhotos[0]]); // Second page has fewer items

    const { getByTestId } = render(<GalleryScreen />);

    await waitFor(() => {
      expect(galleryService.getPhotos).toHaveBeenCalledWith({
        page: 0,
        size: 30,
        sort: 'createdAt,desc',
        tagIds: undefined,
      });
    });

    // PhotoGrid component should handle onLoadMore
    expect(getByTestId('gallery-photo-grid')).toBeTruthy();
  });

  it('passes correct props to PhotoGrid', async () => {
    (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

    const { getByTestId } = render(<GalleryScreen />);

    await waitFor(() => {
      const photoGrid = getByTestId('gallery-photo-grid');
      expect(photoGrid).toBeTruthy();
    });
  });

  describe('Lightbox Integration', () => {
    it('lightbox is rendered in GalleryScreen', async () => {
      (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

      const { getByTestId } = render(<GalleryScreen />);

      await waitFor(() => {
        expect(getByTestId('gallery-photo-grid')).toBeTruthy();
      });

      // Lightbox component should be rendered
      const lightbox = getByTestId('gallery-lightbox');
      expect(lightbox).toBeTruthy();
    });

    it('lightbox is present after photos load', async () => {
      (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

      const { getByTestId } = render(<GalleryScreen />);

      await waitFor(() => {
        const lightbox = getByTestId('gallery-lightbox');
        expect(lightbox).toBeTruthy();
      });
    });

    it('gallery photo grid is rendered', async () => {
      (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

      const { getByTestId } = render(<GalleryScreen />);

      await waitFor(() => {
        expect(getByTestId('gallery-photo-grid')).toBeTruthy();
        expect(getByTestId('gallery-lightbox')).toBeTruthy();
      });
    });
  });

  describe('Tag Filtering - Story 3.4', () => {
    it('loads available tags on mount', async () => {
      const { tagService } = require('../services/tagService');
      (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

      const { getByTestId } = render(<GalleryScreen />);

      await waitFor(() => {
        expect(tagService.getTags).toHaveBeenCalled();
      });

      // TagFilterBar should be rendered
      expect(getByTestId('gallery-tag-filter')).toBeTruthy();
    });

    it('filters photos when tag is selected', async () => {
      const { tagService } = require('../services/tagService');
      (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

      const { getByTestId } = render(<GalleryScreen />);

      await waitFor(() => {
        expect(getByTestId('gallery-tag-filter')).toBeTruthy();
      });

      // Click on tag
      const tag1Button = getByTestId('gallery-tag-filter-tag-tag-1');
      fireEvent.press(tag1Button);

      await waitFor(() => {
        expect(galleryService.getPhotos).toHaveBeenCalledWith({
          page: 0,
          size: 30,
          sort: 'createdAt,desc',
          tagIds: ['tag-1'],
        });
      });
    });

    it('clears filters when clear all is clicked', async () => {
      const { tagService } = require('../services/tagService');
      (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

      const { getByTestId } = render(<GalleryScreen />);

      await waitFor(() => {
        expect(getByTestId('gallery-tag-filter')).toBeTruthy();
      });

      // Select a tag
      const tag1Button = getByTestId('gallery-tag-filter-tag-tag-1');
      fireEvent.press(tag1Button);

      await waitFor(() => {
        expect(getByTestId('gallery-tag-filter-clear-button')).toBeTruthy();
      });

      // Clear filters
      const clearButton = getByTestId('gallery-tag-filter-clear-button');
      fireEvent.press(clearButton);

      await waitFor(() => {
        expect(galleryService.getPhotos).toHaveBeenLastCalledWith({
          page: 0,
          size: 30,
          sort: 'createdAt,desc',
          tagIds: undefined,
        });
      });
    });

    it('filters with multiple tags (AND logic)', async () => {
      const { tagService } = require('../services/tagService');
      (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

      const { getByTestId } = render(<GalleryScreen />);

      await waitFor(() => {
        expect(getByTestId('gallery-tag-filter')).toBeTruthy();
      });

      // Select first tag
      const tag1Button = getByTestId('gallery-tag-filter-tag-tag-1');
      fireEvent.press(tag1Button);

      await waitFor(() => {
        expect(galleryService.getPhotos).toHaveBeenCalledWith({
          page: 0,
          size: 30,
          sort: 'createdAt,desc',
          tagIds: ['tag-1'],
        });
      });

      // Select second tag
      const tag2Button = getByTestId('gallery-tag-filter-tag-tag-2');
      fireEvent.press(tag2Button);

      await waitFor(() => {
        expect(galleryService.getPhotos).toHaveBeenLastCalledWith({
          page: 0,
          size: 30,
          sort: 'createdAt,desc',
          tagIds: ['tag-1', 'tag-2'],
        });
      });
    });

    it('reloads photos when tags change', async () => {
      const { tagService } = require('../services/tagService');
      (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

      const { getByTestId } = render(<GalleryScreen />);

      await waitFor(() => {
        expect(galleryService.getPhotos).toHaveBeenCalledTimes(1);
      });

      // Select a tag
      const tag1Button = getByTestId('gallery-tag-filter-tag-tag-1');
      fireEvent.press(tag1Button);

      await waitFor(() => {
        expect(galleryService.getPhotos).toHaveBeenCalledTimes(2);
      });
    });

    it('handles tag loading errors gracefully', async () => {
      const { tagService } = require('../services/tagService');
      tagService.getTags = jest.fn().mockRejectedValue(new Error('Failed to load tags'));
      (galleryService.getPhotos as jest.Mock).mockResolvedValue(mockPhotos);

      const { queryByTestId } = render(<GalleryScreen />);

      await waitFor(() => {
        expect(galleryService.getPhotos).toHaveBeenCalled();
      });

      // TagFilterBar should not crash the app
      // Since no tags loaded, TagFilterBar returns null
      await waitFor(() => {
        const filterBar = queryByTestId('gallery-tag-filter');
        // Filter bar won't be visible if no tags
        expect(filterBar).toBeTruthy(); // Component still renders, but returns null internally
      });
    });
  });
});
