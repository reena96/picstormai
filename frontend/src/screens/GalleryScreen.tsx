/**
 * Gallery Screen - Photo Gallery with Infinite Scroll
 * Story 3.1: Photo Gallery UI with Infinite Scroll
 * Story 3.4: Tag Filter & Search - Added tag filtering
 * Story 3.6: Batch Photo Download - Added selection mode
 */

import React, { useState, useEffect, useCallback } from 'react';
import { View, ViewStyle, Platform, TouchableOpacity } from 'react-native';
import { useTheme } from '../hooks/useTheme';
import { useNavigation, useIsFocused, useRoute } from '@react-navigation/native';
import { galleryService, Photo, SortOption } from '../services/galleryService';
import { tagService, Tag } from '../services/tagService';
import { downloadService } from '../services/downloadService';
import { PhotoGrid } from '../components/organisms/PhotoGrid';
import { PhotoCardProps } from '../components/molecules/PhotoCard';
import { Select, SelectOption } from '../components/atoms/Select';
import { Text } from '../components/atoms/Text';
import { Spinner } from '../components/atoms/Spinner';
import { Button } from '../components/atoms/Button';
import { Image as ImageIcon, Download, Trash2, ArrowLeft } from 'lucide-react-native';
import { EmptyState } from '../components/molecules/EmptyState';
import { Lightbox } from '../components/organisms/Lightbox';
import { TagFilterBar } from '../components/organisms/TagFilterBar';

const SORT_OPTIONS: SelectOption[] = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'Largest First', value: 'fileSize,desc' },
  { label: 'Name (A-Z)', value: 'filename,asc' },
];

export const GalleryScreen: React.FC = () => {
  const { theme } = useTheme();
  const navigation = useNavigation();
  const route = useRoute();
  const isFocused = useIsFocused(); // Only load data when screen is visible
  const [hasBeenFocused, setHasBeenFocused] = useState(false); // Track if ever focused
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  const [sortBy, setSortBy] = useState<SortOption>('createdAt,desc');
  const [error, setError] = useState<string | null>(null);
  const [lightboxVisible, setLightboxVisible] = useState(false);
  const [lightboxIndex, setLightboxIndex] = useState(0);
  // Story 3.4: Tag filtering state
  const [availableTags, setAvailableTags] = useState<Tag[]>([]);
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);
  // Story 3.6: Selection mode state
  const [isSelectionMode, setIsSelectionMode] = useState(false);
  const [selectedPhotoIds, setSelectedPhotoIds] = useState<Set<string>>(new Set());
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [isDownloading, setIsDownloading] = useState(false);
  // Highlight newly uploaded photos
  const [highlightedSessionId, setHighlightedSessionId] = useState<string | null>(null);

  const MAX_BATCH_SELECTION = 50;

  // Get highlighted session ID from route params
  const params = route.params as { highlightSessionId?: string } | undefined;
  useEffect(() => {
    if (params?.highlightSessionId) {
      setHighlightedSessionId(params.highlightSessionId);
      // Clear highlight after 5 seconds
      const timeout = setTimeout(() => {
        setHighlightedSessionId(null);
      }, 5000);
      return () => clearTimeout(timeout);
    }
  }, [params?.highlightSessionId]);

  const loadPhotos = useCallback(async (
    pageNum: number,
    sort: SortOption,
    tagIds: string[],
    append: boolean = true
  ) => {
    if (loading) return;

    setLoading(true);
    setError(null);

    try {
      const fetchedPhotos = await galleryService.getPhotos({
        page: pageNum,
        size: 30,
        sort,
        tagIds: tagIds.length > 0 ? tagIds : undefined, // Story 3.4: Pass tag filter
      });

      if (append) {
        setPhotos((prev) => [...prev, ...fetchedPhotos]);
      } else {
        setPhotos(fetchedPhotos);
      }

      // Check if there are more photos to load
      setHasMore(fetchedPhotos.length === 30);
      setPage(pageNum);
    } catch (err) {
      console.error('Failed to load photos:', err);
      setError('Failed to load photos. Please try again.');
    } finally {
      setLoading(false);
      setInitialLoading(false);
    }
  }, [loading]);

  // Track when screen becomes focused for the first time
  useEffect(() => {
    if (isFocused && !hasBeenFocused) {
      setHasBeenFocused(true);
    }
  }, [isFocused, hasBeenFocused]);

  // Story 3.4: Load available tags only after screen has been focused
  useEffect(() => {
    if (!hasBeenFocused) return; // Don't load until screen has been visited

    const loadAvailableTags = async () => {
      try {
        const tags = await tagService.getTags();
        setAvailableTags(tags);
      } catch (err) {
        console.error('Failed to load tags:', err);
        // Don't show error to user - tags are optional
      }
    };

    loadAvailableTags();
  }, [hasBeenFocused]);

  // Load photos only after screen has been focused
  useEffect(() => {
    if (!hasBeenFocused) return; // Don't load until screen has been visited

    loadPhotos(0, sortBy, selectedTagIds, false);
  }, [hasBeenFocused]);

  const handleSortChange = (newSort: string) => {
    setSortBy(newSort as SortOption);
    setPhotos([]);
    setPage(0);
    setHasMore(true);
    loadPhotos(0, newSort as SortOption, selectedTagIds, false);
  };

  const handleLoadMore = useCallback(() => {
    if (hasMore && !loading) {
      loadPhotos(page + 1, sortBy, selectedTagIds, true);
    }
  }, [hasMore, loading, page, sortBy, selectedTagIds, loadPhotos]);

  // Story 3.4: Handle tag filter changes
  const handleToggleTag = (tagId: string) => {
    const newSelectedIds = selectedTagIds.includes(tagId)
      ? selectedTagIds.filter((id) => id !== tagId)
      : [...selectedTagIds, tagId];

    setSelectedTagIds(newSelectedIds);
    setPhotos([]);
    setPage(0);
    setHasMore(true);
    loadPhotos(0, sortBy, newSelectedIds, false);
  };

  const handleClearAllFilters = () => {
    setSelectedTagIds([]);
    setPhotos([]);
    setPage(0);
    setHasMore(true);
    loadPhotos(0, sortBy, [], false);
  };

  const handlePhotoPress = (index: number) => {
    // Story 3.6: In selection mode, toggle selection instead of opening lightbox
    // Use photosWithUrls to get the correct photo (after filtering)
    const photosWithValidUrls = photos.filter(photo => photo.storageUrl);
    if (isSelectionMode) {
      handleTogglePhotoSelection(photosWithValidUrls[index].id);
    } else {
      setLightboxIndex(index);
      setLightboxVisible(true);
    }
  };

  const handleCloseLightbox = () => {
    setLightboxVisible(false);
  };

  const handlePhotoDeleted = (photoId: string) => {
    // Remove the photo from the local state
    setPhotos(prev => prev.filter(photo => photo.id !== photoId));
  };

  const handleNavigateToUpload = () => {
    // Navigate to Upload tab
    navigation.navigate('Upload' as never);
  };

  // Story 3.6: Selection mode handlers
  const handleEnterSelectionMode = () => {
    setIsSelectionMode(true);
    setSelectedPhotoIds(new Set());
    setDownloadError(null);
  };

  const handleExitSelectionMode = () => {
    setIsSelectionMode(false);
    setSelectedPhotoIds(new Set());
    setDownloadError(null);
  };

  const handleTogglePhotoSelection = (photoId: string) => {
    setSelectedPhotoIds(prev => {
      const newSet = new Set(prev);
      if (newSet.has(photoId)) {
        newSet.delete(photoId);
      } else {
        if (newSet.size >= MAX_BATCH_SELECTION) {
          setDownloadError(`Maximum ${MAX_BATCH_SELECTION} photos per download. Please select fewer photos.`);
          return prev;
        }
        newSet.add(photoId);
      }
      // Clear error when selection changes successfully
      if (downloadError) {
        setDownloadError(null);
      }
      return newSet;
    });
  };

  const handleSelectAll = () => {
    // Select up to MAX_BATCH_SELECTION photos (only from photos with valid storageUrl)
    const photosWithValidUrls = photos.filter(photo => photo.storageUrl);
    const photosToSelect = photosWithValidUrls.slice(0, MAX_BATCH_SELECTION);
    const newSet = new Set(photosToSelect.map(photo => photo.id));
    setSelectedPhotoIds(newSet);
    setDownloadError(null);

    if (photosWithValidUrls.length > MAX_BATCH_SELECTION) {
      setDownloadError(`Selected first ${MAX_BATCH_SELECTION} photos. Maximum ${MAX_BATCH_SELECTION} photos per download.`);
    }
  };

  const handleDeselectAll = () => {
    setSelectedPhotoIds(new Set());
    setDownloadError(null);
  };

  const handleDownloadSelected = async () => {
    if (selectedPhotoIds.size === 0) return;

    setIsDownloading(true);
    setDownloadError(null);

    try {
      await downloadService.downloadBatch(Array.from(selectedPhotoIds));
      // Success - exit selection mode
      handleExitSelectionMode();
    } catch (error) {
      console.error('Batch download failed:', error);
      const errorMessage = error instanceof Error
        ? error.message
        : 'Download failed. Please try again.';
      setDownloadError(errorMessage);
    } finally {
      setIsDownloading(false);
    }
  };

  const handleDeleteSelected = async () => {
    if (selectedPhotoIds.size === 0) return;

    // Show confirmation dialog
    if (!confirm(`Are you sure you want to delete ${selectedPhotoIds.size} selected photo${selectedPhotoIds.size > 1 ? 's' : ''}?`)) {
      return;
    }

    setIsDownloading(true); // Reuse the loading state
    setDownloadError(null);

    try {
      const result = await galleryService.deletePhotos(Array.from(selectedPhotoIds));

      // Remove deleted photos from local state
      setPhotos(prev => prev.filter(photo => !selectedPhotoIds.has(photo.id)));

      // Exit selection mode
      handleExitSelectionMode();

      // Show success message if some failed
      if (result.deletedCount < result.totalRequested) {
        setDownloadError(`Deleted ${result.deletedCount} of ${result.totalRequested} photos. Some photos could not be deleted.`);
      }
    } catch (error) {
      console.error('Batch delete failed:', error);
      const errorMessage = error instanceof Error
        ? error.message
        : 'Delete failed. Please try again.';
      setDownloadError(errorMessage);
    } finally {
      setIsDownloading(false);
    }
  };

  // Convert photos to PhotoCardProps
  // FILTER OUT photos without storageUrl (still uploading to S3 or pending backend notification)
  const photosWithUrls = photos.filter(photo => photo.storageUrl);
  const photoCardProps: PhotoCardProps[] = photosWithUrls
    .map((photo) => {
      const isHighlighted = highlightedSessionId && photo.sessionId === highlightedSessionId;
      return {
        photoUrl: photo.storageUrl || '',
        thumbnailUrl: photo.storageUrl || '', // Use storageUrl as fallback for thumbnail
        filename: photo.filename,
        uploadDate: new Date(photo.createdAt),
        fileSize: photo.fileSize,
        tags: [], // Tags will be added in Story 3.3
        onPress: () => {}, // Handled by PhotoGrid
        loading: false,
        // Story 3.6: Selection mode props
        isSelectionMode,
        isSelected: selectedPhotoIds.has(photo.id),
        // Highlight newly uploaded photos
        style: isHighlighted ? {
          borderWidth: 3,
          borderColor: theme.colors.primary,
          borderRadius: theme.borderRadius.base,
        } : undefined,
      };
    });

  const containerStyle: ViewStyle = {
    flex: 1,
    backgroundColor: theme.colors.background,
  };

  const headerStyle: ViewStyle = {
    padding: theme.spacing[4],
    paddingBottom: theme.spacing[2],
    backgroundColor: theme.colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.gray[200],
  };

  const titleRowStyle: ViewStyle = {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: theme.spacing[4],
  };

  const backButtonStyle: ViewStyle = {
    padding: theme.spacing[2],
    marginRight: theme.spacing[2],
    marginLeft: -theme.spacing[2],
  };

  const errorContainerStyle: ViewStyle = {
    padding: theme.spacing[4],
    backgroundColor: '#FF3B30',
    marginHorizontal: theme.spacing[4],
    marginTop: theme.spacing[4],
    borderRadius: theme.borderRadius.base,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  };

  if (initialLoading) {
    return (
      <View style={[containerStyle, { justifyContent: 'center', alignItems: 'center' }]}>
        <Spinner size="large" />
        <View style={{ marginTop: theme.spacing[4] }}>
          <Text variant="body" color={theme.colors.text.secondary}>
            Loading your photos...
          </Text>
        </View>
      </View>
    );
  }

  if (photos.length === 0 && !loading) {
    return (
      <View style={containerStyle}>
        <View style={headerStyle}>
          <View style={titleRowStyle}>
            <TouchableOpacity
              onPress={() => navigation.navigate('Home' as never)}
              style={backButtonStyle}
              accessibilityLabel="Back to Home"
            >
              <ArrowLeft size={24} color={theme.colors.text.primary} />
            </TouchableOpacity>
            <Text variant="h2" weight="bold">
              Photo Gallery
            </Text>
          </View>
        </View>
        <EmptyState
          icon={ImageIcon}
          headline="No photos yet"
          subtext="Start uploading photos to see them here!"
          ctaLabel="Go to Upload"
          onCtaPress={handleNavigateToUpload}
        />
      </View>
    );
  }

  const selectionControlsStyle: ViewStyle = {
    flexDirection: 'row',
    gap: theme.spacing[2],
    alignItems: 'center',
  };

  return (
    <View style={containerStyle}>
      <View style={headerStyle}>
        <View style={titleRowStyle}>
          <TouchableOpacity
            onPress={() => navigation.navigate('Home' as never)}
            style={backButtonStyle}
            accessibilityLabel="Back to Home"
          >
            <ArrowLeft size={24} color={theme.colors.text.primary} />
          </TouchableOpacity>
          <Text variant="h2" weight="bold">
            Photo Gallery
          </Text>
          {/* Story 3.6: Selection mode controls */}
          {!isSelectionMode ? (
            <View style={selectionControlsStyle}>
              <Text variant="body" color={theme.colors.text.secondary}>
                {photosWithUrls.length}{hasMore ? '+' : ''} photo{photosWithUrls.length !== 1 ? 's' : ''}
              </Text>
              <Button
                onPress={handleEnterSelectionMode}
                variant="secondary"
                testID="select-photos-button"
              >
                <Text variant="body" color={theme.colors.primary}>Select Photos</Text>
              </Button>
            </View>
          ) : (
            <View style={selectionControlsStyle}>
              <Button
                onPress={selectedPhotoIds.size === photosWithUrls.length ? handleDeselectAll : handleSelectAll}
                variant="secondary"
                testID="select-all-button"
              >
                <Text variant="body" color={theme.colors.primary}>
                  {selectedPhotoIds.size === photosWithUrls.length ? 'Deselect All' : 'Select All'}
                </Text>
              </Button>
              <Text variant="body" color={theme.colors.text.primary}>
                {selectedPhotoIds.size} selected
              </Text>
              <TouchableOpacity
                onPress={handleDeleteSelected}
                disabled={selectedPhotoIds.size === 0 || isDownloading}
                style={{
                  padding: 8,
                  backgroundColor: selectedPhotoIds.size === 0 || isDownloading ? theme.colors.gray[300] : '#DC2626',
                  borderRadius: 8,
                  marginLeft: theme.spacing[2],
                }}
                testID="delete-selected-button"
              >
                <Trash2 size={20} color="#FFFFFF" />
              </TouchableOpacity>
              <TouchableOpacity
                onPress={handleDownloadSelected}
                disabled={selectedPhotoIds.size === 0 || isDownloading}
                style={{
                  padding: 8,
                  backgroundColor: selectedPhotoIds.size === 0 || isDownloading ? theme.colors.gray[300] : '#2563EB',
                  borderRadius: 8,
                }}
                testID="download-selected-button"
              >
                <Download size={20} color="#FFFFFF" />
              </TouchableOpacity>
              <Button
                onPress={handleExitSelectionMode}
                variant="secondary"
                testID="cancel-selection-button"
              >
                <Text variant="body" color={theme.colors.primary}>Cancel</Text>
              </Button>
            </View>
          )}
        </View>

        {!isSelectionMode && (
          <Select
            label="Sort by"
            value={sortBy}
            options={SORT_OPTIONS}
            onChange={handleSortChange}
            testID="gallery-sort-select"
            style={{ marginBottom: 0 }}
          />
        )}
      </View>

      {/* Story 3.4: Tag Filter Bar */}
      <TagFilterBar
        availableTags={availableTags}
        selectedTagIds={selectedTagIds}
        onToggleTag={handleToggleTag}
        onClearAll={handleClearAllFilters}
        filteredPhotoCount={photosWithUrls.length}
        testID="gallery-tag-filter"
      />

      {/* Story 3.6: Download error message */}
      {downloadError && (
        <View style={errorContainerStyle}>
          <Text variant="body" color={theme.colors.white}>
            {downloadError}
          </Text>
          <TouchableOpacity
            onPress={() => setDownloadError(null)}
            style={{ marginLeft: theme.spacing[2] }}
          >
            <Text variant="body" color={theme.colors.white} weight="bold">
              Dismiss
            </Text>
          </TouchableOpacity>
        </View>
      )}

      {error && (
        <View style={errorContainerStyle}>
          <Text variant="body" color={theme.colors.white}>
            {error}
          </Text>
        </View>
      )}

      <PhotoGrid
        photos={photoCardProps}
        onPhotoPress={handlePhotoPress}
        loading={loading}
        onLoadMore={handleLoadMore}
        hasMore={hasMore}
        emptyMessage="No photos match your criteria"
        testID="gallery-photo-grid"
      />

      <Lightbox
        visible={lightboxVisible}
        photos={photosWithUrls}
        initialIndex={lightboxIndex}
        onClose={handleCloseLightbox}
        onPhotoDeleted={handlePhotoDeleted}
        testID="gallery-lightbox"
      />
    </View>
  );
};

export default GalleryScreen;
