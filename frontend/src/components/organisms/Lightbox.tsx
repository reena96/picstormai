/**
 * Lightbox Component - Fullscreen Photo Viewer
 * Story 3.2: Photo Viewing - Lightbox
 * Story 3.5: Individual Photo Download (added download button)
 *
 * Features:
 * - Fullscreen photo display with dark overlay
 * - Navigation (Previous/Next arrows)
 * - Keyboard navigation (web: Arrow keys, Escape)
 * - Swipe gestures (mobile: left/right)
 * - Zoom functionality (web: scroll wheel, mobile: pinch, buttons, double-tap)
 * - Photo metadata display (filename, date, file size)
 * - Loading states with blur-up technique
 * - Body scroll lock (web)
 * - Download button with presigned URL download
 */

import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  View,
  Image,
  TouchableOpacity,
  StyleSheet,
  Modal,
  Platform,
  PanResponder,
  Animated,
  Dimensions,
  GestureResponderEvent,
  PanResponderGestureState,
  ViewStyle,
  ImageStyle,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { X, ChevronLeft, ChevronRight, ZoomIn, ZoomOut, Download, Trash2 } from 'lucide-react-native';
import { useTheme } from '../../hooks/useTheme';
import { Text } from '../atoms/Text';
import { Spinner } from '../atoms/Spinner';
import { Photo, Tag as PhotoTag, galleryService } from '../../services/galleryService';
import { TagInput, Tag } from '../molecules/TagInput';
import { tagService } from '../../services/tagService';
import { downloadService } from '../../services/downloadService';

interface LightboxProps {
  visible: boolean;
  photos: Photo[];
  initialIndex: number;
  onClose: () => void;
  onPhotoChange?: (index: number) => void;
  onPhotoDeleted?: (photoId: string) => void;
  testID?: string;
}

export const Lightbox: React.FC<LightboxProps> = ({
  visible,
  photos,
  initialIndex,
  onClose,
  onPhotoChange,
  onPhotoDeleted,
  testID,
}) => {
  const { theme } = useTheme();
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [imageLoading, setImageLoading] = useState(true);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [panOffset, setPanOffset] = useState({ x: 0, y: 0 });
  const [lastTap, setLastTap] = useState<number>(0);
  const [availableTags, setAvailableTags] = useState<Tag[]>([]);
  const [photoTags, setPhotoTags] = useState<Record<string, Tag[]>>({});
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [imageError, setImageError] = useState(false);
  const zoomTransform = useRef(new Animated.Value(1)).current;
  const panX = useRef(new Animated.Value(0)).current;
  const panY = useRef(new Animated.Value(0)).current;

  // Sync currentIndex with initialIndex when lightbox opens
  useEffect(() => {
    if (visible) {
      setCurrentIndex(initialIndex);
      setZoomLevel(1);
      setPanOffset({ x: 0, y: 0 });
      setImageLoading(true);
      setImageError(false);
      setDownloadError(null);
      loadAvailableTags();
      loadPhotoTags();
    }
  }, [visible, initialIndex]);

  // Load available tags when lightbox opens
  const loadAvailableTags = async () => {
    try {
      const tags = await tagService.getTags();
      setAvailableTags(tags);
    } catch (error) {
      console.error('Failed to load available tags:', error);
    }
  };

  // Load tags for current photo from photo data
  const loadPhotoTags = () => {
    const tagsMap: Record<string, Tag[]> = {};
    photos.forEach((photo) => {
      tagsMap[photo.id] = photo.tags || [];
    });
    setPhotoTags(tagsMap);
  };

  // Handle adding tag to photo
  const handleAddTag = async (tagName: string): Promise<Tag> => {
    const photoId = currentPhoto.id;
    const newTag = await tagService.addTagToPhoto(photoId, tagName);

    // Update local state
    setPhotoTags((prev) => ({
      ...prev,
      [photoId]: [...(prev[photoId] || []), newTag],
    }));

    // Add to available tags if it's a new tag
    if (!availableTags.find((t) => t.name === newTag.name)) {
      setAvailableTags((prev) => [...prev, newTag]);
    }

    return newTag;
  };

  // Handle removing tag from photo
  const handleRemoveTag = async (tagId: string): Promise<void> => {
    const photoId = currentPhoto.id;
    await tagService.removeTagFromPhoto(photoId, tagId);

    // Update local state
    setPhotoTags((prev) => ({
      ...prev,
      [photoId]: (prev[photoId] || []).filter((t) => t.id !== tagId),
    }));
  };

  // Handle download button click
  const handleDownload = async () => {
    const currentPhoto = photos[currentIndex];

    setIsDownloading(true);
    setDownloadError(null);

    try {
      await downloadService.downloadPhoto(currentPhoto.id);
      // Success - no need to show toast on web as browser shows download notification
      // On mobile, we could show a toast here if needed
    } catch (error) {
      console.error('Download failed:', error);
      const errorMessage = error instanceof Error
        ? error.message
        : 'Download failed. Please try again.';
      setDownloadError(errorMessage);

      // Auto-clear error after 5 seconds
      setTimeout(() => setDownloadError(null), 5000);
    } finally {
      setIsDownloading(false);
    }
  };

  // Handle delete button click
  const handleDelete = async () => {
    const currentPhoto = photos[currentIndex];

    if (!confirm(`Are you sure you want to delete "${currentPhoto.filename}"?`)) {
      return;
    }

    setIsDeleting(true);
    setDeleteError(null);

    try {
      await galleryService.deletePhoto(currentPhoto.id);

      // Notify parent component that photo was deleted
      if (onPhotoDeleted) {
        onPhotoDeleted(currentPhoto.id);
      }

      // Close lightbox after successful delete
      onClose();
    } catch (error) {
      console.error('Delete failed:', error);
      const errorMessage = error instanceof Error
        ? error.message
        : 'Delete failed. Please try again.';
      setDeleteError(errorMessage);

      // Auto-clear error after 5 seconds
      setTimeout(() => setDeleteError(null), 5000);
    } finally {
      setIsDeleting(false);
    }
  };

  // Notify parent of photo change
  useEffect(() => {
    if (visible && onPhotoChange) {
      onPhotoChange(currentIndex);
    }
  }, [currentIndex, visible, onPhotoChange]);

  // Reset zoom and pan when photo changes
  useEffect(() => {
    setZoomLevel(1);
    setPanOffset({ x: 0, y: 0 });
    setImageLoading(true);
    setDownloadError(null); // Clear download error when navigating
    zoomTransform.setValue(1);
    panX.setValue(0);
    panY.setValue(0);
  }, [currentIndex]);

  const currentPhoto = photos[currentIndex];
  const isFirstPhoto = currentIndex === 0;
  const isLastPhoto = currentIndex === photos.length - 1;

  // Navigation handlers
  const handlePrevious = useCallback(() => {
    if (!isFirstPhoto) {
      setCurrentIndex((prev) => prev - 1);
    }
  }, [isFirstPhoto]);

  const handleNext = useCallback(() => {
    if (!isLastPhoto) {
      setCurrentIndex((prev) => prev + 1);
    }
  }, [isLastPhoto]);

  // Keyboard navigation (Web)
  useEffect(() => {
    if (!visible || Platform.OS !== 'web') return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      } else if (e.key === 'ArrowLeft') {
        e.preventDefault();
        handlePrevious();
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        handleNext();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [visible, handlePrevious, handleNext, onClose]);

  // Body scroll lock (Web)
  useEffect(() => {
    if (!visible || Platform.OS !== 'web') return;

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, [visible]);

  // Swipe gesture handler (Mobile)
  const panResponder = useRef(
    PanResponder.create({
      onMoveShouldSetPanResponder: (evt, gestureState) => {
        // Only handle swipes when not zoomed
        if (zoomLevel > 1) return false;
        return Math.abs(gestureState.dx) > 10;
      },
      onPanResponderRelease: (evt, gestureState) => {
        if (zoomLevel > 1) return;

        // Swipe right -> previous photo
        if (gestureState.dx > 50 && !isFirstPhoto) {
          handlePrevious();
        }
        // Swipe left -> next photo
        else if (gestureState.dx < -50 && !isLastPhoto) {
          handleNext();
        }
      },
    })
  ).current;

  // Pan gesture handler for zoomed images
  const zoomPanResponder = useRef(
    PanResponder.create({
      onMoveShouldSetPanResponder: () => zoomLevel > 1,
      onPanResponderGrant: () => {
        panX.setOffset(panOffset.x);
        panY.setOffset(panOffset.y);
      },
      onPanResponderMove: Animated.event(
        [null, { dx: panX, dy: panY }],
        { useNativeDriver: false }
      ),
      onPanResponderRelease: () => {
        panX.flattenOffset();
        panY.flattenOffset();
        setPanOffset({
          x: (panX as any)._value,
          y: (panY as any)._value,
        });
      },
    })
  ).current;

  // Zoom controls
  const handleZoomIn = useCallback(() => {
    setZoomLevel((prev) => Math.min(4, prev + 0.5));
  }, []);

  const handleZoomOut = useCallback(() => {
    setZoomLevel((prev) => {
      const newZoom = Math.max(0.5, prev - 0.5);
      if (newZoom === 1) {
        setPanOffset({ x: 0, y: 0 });
        panX.setValue(0);
        panY.setValue(0);
      }
      return newZoom;
    });
  }, []);

  // Double-tap zoom toggle
  const handleImagePress = useCallback(() => {
    const now = Date.now();
    const DOUBLE_TAP_DELAY = 300;

    if (now - lastTap < DOUBLE_TAP_DELAY) {
      // Double tap detected
      if (zoomLevel === 1) {
        setZoomLevel(2);
      } else {
        setZoomLevel(1);
        setPanOffset({ x: 0, y: 0 });
        panX.setValue(0);
        panY.setValue(0);
      }
    }
    setLastTap(now);
  }, [lastTap, zoomLevel]);

  // Wheel event for zoom (Web)
  useEffect(() => {
    if (!visible || Platform.OS !== 'web') return;

    const handleWheel = (e: WheelEvent) => {
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault();
        const delta = e.deltaY > 0 ? 0.9 : 1.1;
        setZoomLevel((prev) => {
          const newZoom = Math.min(4, Math.max(0.5, prev * delta));
          if (newZoom === 1) {
            setPanOffset({ x: 0, y: 0 });
            panX.setValue(0);
            panY.setValue(0);
          }
          return newZoom;
        });
      }
    };

    const element = document.getElementById('lightbox-image-container');
    if (element) {
      element.addEventListener('wheel', handleWheel, { passive: false });
      return () => element.removeEventListener('wheel', handleWheel);
    }
  }, [visible, zoomLevel]);

  // Animate zoom transform
  useEffect(() => {
    Animated.spring(zoomTransform, {
      toValue: zoomLevel,
      useNativeDriver: true,
      friction: 7,
    }).start();
  }, [zoomLevel]);

  // Format helpers (reused from PhotoCard)
  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    }).format(date);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  if (!currentPhoto) return null;

  const containerStyle: ViewStyle = {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.95)',
    justifyContent: 'center',
    alignItems: 'center',
  };

  const closeButtonStyle: ViewStyle = {
    position: 'absolute',
    top: Platform.OS === 'web' ? 20 : 50,
    right: 20,
    zIndex: 20,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    borderRadius: 20,
    padding: 8,
  };

  const downloadButtonStyle: ViewStyle = {
    position: 'absolute',
    top: Platform.OS === 'web' ? 20 : 50,
    right: 120, // Position to the left of delete button
    zIndex: 20,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    borderRadius: 20,
    padding: 8,
  };

  const deleteButtonStyle: ViewStyle = {
    position: 'absolute',
    top: Platform.OS === 'web' ? 20 : 50,
    right: 70, // Position between download and close button
    zIndex: 20,
    backgroundColor: 'rgba(220, 38, 38, 0.8)', // Red background for delete
    borderRadius: 20,
    padding: 8,
  };

  const navButtonStyle: ViewStyle = {
    position: 'absolute',
    top: '50%',
    transform: [{ translateY: -20 }],
    zIndex: 20,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    borderRadius: 20,
    padding: 8,
  };

  const prevButtonStyle: ViewStyle = {
    ...navButtonStyle,
    left: 20,
  };

  const nextButtonStyle: ViewStyle = {
    ...navButtonStyle,
    right: 20,
  };

  const imageContainerStyle: ViewStyle = {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
  };

  const windowDimensions = Dimensions.get('window');
  const imageStyle: ImageStyle = Platform.OS === 'web'
    ? {
        maxWidth: windowDimensions.width * 0.9,
        maxHeight: windowDimensions.height * 0.8,
        width: windowDimensions.width * 0.9,
        height: windowDimensions.height * 0.8,
      }
    : {
        width: windowDimensions.width,
        height: windowDimensions.height * 0.8,
      };

  const metadataBarStyle: ViewStyle = {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    maxHeight: '40%',
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    padding: theme.spacing[4],
    paddingBottom: Platform.OS === 'web' ? theme.spacing[4] : theme.spacing[6],
  };

  const zoomControlsStyle: ViewStyle = {
    position: 'absolute',
    top: Platform.OS === 'web' ? 20 : 50,
    left: 20,
    zIndex: 20,
    flexDirection: 'row',
    gap: 8,
  };

  const zoomButtonStyle: ViewStyle = {
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    borderRadius: 20,
    padding: 8,
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
      testID={testID}
    >
      <View style={containerStyle} {...panResponder.panHandlers}>
        {/* Download button */}
        <TouchableOpacity
          style={downloadButtonStyle}
          onPress={handleDownload}
          disabled={isDownloading}
          accessibilityLabel="Download photo"
          accessibilityRole="button"
          testID={`${testID}-download-button`}
        >
          {isDownloading ? (
            <ActivityIndicator size="small" color="#FFFFFF" />
          ) : (
            <Download size={24} color="#FFFFFF" />
          )}
        </TouchableOpacity>

        {/* Delete button */}
        <TouchableOpacity
          style={deleteButtonStyle}
          onPress={handleDelete}
          disabled={isDeleting}
          accessibilityLabel="Delete photo"
          accessibilityRole="button"
          testID={`${testID}-delete-button`}
        >
          {isDeleting ? (
            <ActivityIndicator size="small" color="#FFFFFF" />
          ) : (
            <Trash2 size={24} color="#FFFFFF" />
          )}
        </TouchableOpacity>

        {/* Close button */}
        <TouchableOpacity
          style={closeButtonStyle}
          onPress={onClose}
          accessibilityLabel="Close lightbox"
          testID={`${testID}-close-button`}
        >
          <X size={24} color="#FFFFFF" />
        </TouchableOpacity>

        {/* Zoom controls */}
        <View style={zoomControlsStyle}>
          <TouchableOpacity
            style={zoomButtonStyle}
            onPress={handleZoomOut}
            disabled={zoomLevel <= 0.5}
            accessibilityLabel="Zoom out"
            testID={`${testID}-zoom-out-button`}
          >
            <ZoomOut size={20} color={zoomLevel <= 0.5 ? '#666666' : '#FFFFFF'} />
          </TouchableOpacity>
          <TouchableOpacity
            style={zoomButtonStyle}
            onPress={handleZoomIn}
            disabled={zoomLevel >= 4}
            accessibilityLabel="Zoom in"
            testID={`${testID}-zoom-in-button`}
          >
            <ZoomIn size={20} color={zoomLevel >= 4 ? '#666666' : '#FFFFFF'} />
          </TouchableOpacity>
        </View>

        {/* Previous button */}
        {!isFirstPhoto && (
          <TouchableOpacity
            style={prevButtonStyle}
            onPress={handlePrevious}
            accessibilityLabel="Previous photo"
            testID={`${testID}-previous-button`}
          >
            <ChevronLeft size={32} color="#FFFFFF" />
          </TouchableOpacity>
        )}

        {/* Image container */}
        <View
          style={imageContainerStyle}
          {...(zoomLevel > 1 ? zoomPanResponder.panHandlers : {})}
          // @ts-ignore - web-specific id
          id="lightbox-image-container"
        >
          {/* Full resolution image */}
          <Animated.View
            style={{
              transform: [
                { scale: zoomTransform },
                { translateX: panX },
                { translateY: panY },
              ],
            }}
          >
            <TouchableOpacity
              onPress={handleImagePress}
              activeOpacity={1}
            >
              <Image
                source={{ uri: currentPhoto.storageUrl || '' }}
                style={imageStyle}
                resizeMode="contain"
                onLoadStart={() => {
                  setImageLoading(true);
                  setImageError(false);
                }}
                onLoadEnd={() => {
                  setImageLoading(false);
                }}
                onError={() => {
                  setImageLoading(false);
                  setImageError(true);
                }}
                accessibilityLabel={`Photo: ${currentPhoto.filename}`}
                testID={`${testID}-image`}
              />
            </TouchableOpacity>
          </Animated.View>

          {/* Loading spinner */}
          {imageLoading && !imageError && (
            <View style={StyleSheet.absoluteFill}>
              <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
                <Spinner size="large" color="#FFFFFF" />
              </View>
            </View>
          )}

          {/* Image error message */}
          {imageError && (
            <View style={StyleSheet.absoluteFill}>
              <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 }}>
                <Text variant="body" color="#FFFFFF" style={{ textAlign: 'center', marginBottom: 10 }}>
                  Failed to load image
                </Text>
                <Text variant="caption" color="#CCCCCC" style={{ textAlign: 'center' }}>
                  The image URL may have expired or the file may be unavailable.
                </Text>
                <Text variant="caption" color="#CCCCCC" style={{ textAlign: 'center', marginTop: 10 }}>
                  URL: {currentPhoto.storageUrl ? 'Present' : 'Missing'}
                </Text>
              </View>
            </View>
          )}
        </View>

        {/* Next button */}
        {!isLastPhoto && (
          <TouchableOpacity
            style={nextButtonStyle}
            onPress={handleNext}
            accessibilityLabel="Next photo"
            testID={`${testID}-next-button`}
          >
            <ChevronRight size={32} color="#FFFFFF" />
          </TouchableOpacity>
        )}

        {/* Metadata bar with tags */}
        <ScrollView
          style={metadataBarStyle}
          contentContainerStyle={{ paddingBottom: theme.spacing[2] }}
          pointerEvents="box-none"
        >
          <View pointerEvents="auto">
            {/* Download error message */}
            {downloadError && (
              <View style={{
                backgroundColor: 'rgba(220, 38, 38, 0.9)',
                padding: theme.spacing[2],
                borderRadius: 4,
                marginBottom: theme.spacing[2],
              }}>
                <Text
                  variant="caption"
                  color={theme.colors.white}
                  testID={`${testID}-download-error`}
                >
                  {downloadError}
                </Text>
              </View>
            )}

            {/* Delete error message */}
            {deleteError && (
              <View style={{
                backgroundColor: 'rgba(220, 38, 38, 0.9)',
                padding: theme.spacing[2],
                borderRadius: 4,
                marginBottom: theme.spacing[2],
              }}>
                <Text
                  variant="caption"
                  color={theme.colors.white}
                  testID={`${testID}-delete-error`}
                >
                  {deleteError}
                </Text>
              </View>
            )}

            <Text
              variant="body"
              weight="bold"
              color={theme.colors.white}
              numberOfLines={1}
              testID={`${testID}-filename`}
            >
              {currentPhoto.filename}
            </Text>
            <View style={{ flexDirection: 'row', marginTop: theme.spacing[1], gap: 16 }}>
              <Text
                variant="caption"
                color={theme.colors.gray[300]}
                testID={`${testID}-date`}
              >
                {formatDate(currentPhoto.createdAt)}
              </Text>
              <Text
                variant="caption"
                color={theme.colors.gray[300]}
                testID={`${testID}-size`}
              >
                {formatFileSize(currentPhoto.fileSize)}
              </Text>
              <Text
                variant="caption"
                color={theme.colors.gray[300]}
                testID={`${testID}-index`}
              >
                {currentIndex + 1} / {photos.length}
              </Text>
            </View>

            {/* Tag input */}
            <TagInput
              photoId={currentPhoto.id}
              tags={photoTags[currentPhoto.id] || []}
              availableTags={availableTags}
              onAddTag={handleAddTag}
              onRemoveTag={handleRemoveTag}
              testID={`${testID}-tag-input`}
            />
          </View>
        </ScrollView>
      </View>
    </Modal>
  );
};
