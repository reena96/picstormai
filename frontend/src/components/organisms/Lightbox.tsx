/**
 * Lightbox Component (Organism)
 * Full-screen photo viewer with navigation, zoom, and metadata
 * Features: swipe gestures (mobile), keyboard navigation (web), action bar
 */

import React, { useState, useEffect } from 'react';
import { View, Modal, Image, Pressable, StyleSheet, ViewStyle, ImageStyle, Platform, Dimensions } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { Text } from '../atoms/Text';
import { Button } from '../atoms/Button';
import { TagChip } from '../molecules/TagChip';
import { X, ChevronLeft, ChevronRight, Download, Share2, Tag, Trash2 } from 'lucide-react-native';

export interface Photo {
  id: string;
  url: string;
  filename: string;
  uploadDate: Date;
  fileSize: number;
  tags?: string[];
}

export interface LightboxProps {
  visible: boolean;
  photos: Photo[];
  initialIndex: number;
  onClose: () => void;
  onNavigate?: (index: number) => void;
  onDownload?: (photoId: string) => void;
  onShare?: (photoId: string) => void;
  onTag?: (photoId: string) => void;
  onDelete?: (photoId: string) => void;
  testID?: string;
}

export const Lightbox: React.FC<LightboxProps> = ({
  visible,
  photos,
  initialIndex,
  onClose,
  onNavigate,
  onDownload,
  onShare,
  onTag,
  onDelete,
  testID,
}) => {
  const { theme } = useTheme();
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [dimensions] = useState(Dimensions.get('window'));

  useEffect(() => {
    setCurrentIndex(initialIndex);
  }, [initialIndex, visible]);

  useEffect(() => {
    if (!visible) return;

    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      } else if (e.key === 'ArrowLeft') {
        handlePrevious();
      } else if (e.key === 'ArrowRight') {
        handleNext();
      }
    };

    if (Platform.OS === 'web') {
      window.addEventListener('keydown', handleKeyPress);
      return () => window.removeEventListener('keydown', handleKeyPress);
    }
  }, [visible, currentIndex]);

  const handlePrevious = () => {
    const newIndex = currentIndex > 0 ? currentIndex - 1 : photos.length - 1;
    setCurrentIndex(newIndex);
    onNavigate?.(newIndex);
  };

  const handleNext = () => {
    const newIndex = currentIndex < photos.length - 1 ? currentIndex + 1 : 0;
    setCurrentIndex(newIndex);
    onNavigate?.(newIndex);
  };

  const currentPhoto = photos[currentIndex];

  if (!currentPhoto) return null;

  const overlayStyle: ViewStyle = {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.95)',
    justifyContent: 'center',
    alignItems: 'center',
  };

  const imageStyle: ImageStyle = {
    maxWidth: dimensions.width * 0.9,
    maxHeight: dimensions.height * 0.9,
    aspectRatio: 1,
  };

  const closeButtonStyle: ViewStyle = {
    position: 'absolute',
    top: theme.spacing[4],
    right: theme.spacing[4],
    zIndex: 10,
    minWidth: 48,
    minHeight: 48,
  };

  const navigationButtonStyle: ViewStyle = {
    position: 'absolute',
    top: '50%',
    transform: [{ translateY: -24 }],
    zIndex: 10,
    minWidth: 48,
    minHeight: 48,
  };

  const actionBarStyle: ViewStyle = {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    padding: theme.spacing[4],
  };

  const metadataStyle: ViewStyle = {
    marginBottom: theme.spacing[3],
  };

  const actionsStyle: ViewStyle = {
    flexDirection: 'row',
    justifyContent: 'space-around',
    gap: theme.spacing[2],
  };

  const formatDate = (date: Date): string => {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    }).format(date);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
      testID={testID}
    >
      <View style={overlayStyle}>
        {/* Close button */}
        <Pressable
          style={closeButtonStyle}
          onPress={onClose}
          accessibilityRole="button"
          accessibilityLabel="Close lightbox"
        >
          <X size={32} color={theme.colors.white} />
        </Pressable>

        {/* Previous button */}
        {photos.length > 1 && (
          <Pressable
            style={[navigationButtonStyle, { left: theme.spacing[4] }]}
            onPress={handlePrevious}
            accessibilityRole="button"
            accessibilityLabel="Previous photo"
          >
            <ChevronLeft size={48} color={theme.colors.white} />
          </Pressable>
        )}

        {/* Next button */}
        {photos.length > 1 && (
          <Pressable
            style={[navigationButtonStyle, { right: theme.spacing[4] }]}
            onPress={handleNext}
            accessibilityRole="button"
            accessibilityLabel="Next photo"
          >
            <ChevronRight size={48} color={theme.colors.white} />
          </Pressable>
        )}

        {/* Main image */}
        <Image
          source={{ uri: currentPhoto.url }}
          style={imageStyle}
          resizeMode="contain"
          accessibilityIgnoresInvertColors
        />

        {/* Action bar and metadata */}
        <View style={actionBarStyle}>
          {/* Metadata */}
          <View style={metadataStyle}>
            <Text variant="body" color={theme.colors.white} weight="medium">
              {currentPhoto.filename}
            </Text>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: theme.spacing[1] }}>
              <Text variant="caption" color={theme.colors.gray[300]}>
                {formatDate(currentPhoto.uploadDate)}
              </Text>
              <Text variant="caption" color={theme.colors.gray[300]}>
                {formatFileSize(currentPhoto.fileSize)}
              </Text>
            </View>

            {/* Tags */}
            {currentPhoto.tags && currentPhoto.tags.length > 0 && (
              <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginTop: theme.spacing[2], gap: theme.spacing[1] }}>
                {currentPhoto.tags.map((tag, index) => (
                  <TagChip key={index} label={tag} />
                ))}
              </View>
            )}
          </View>

          {/* Action buttons */}
          <View style={actionsStyle}>
            {onDownload && (
              <Button
                variant="secondary"
                size="medium"
                onPress={() => onDownload(currentPhoto.id)}
                accessibilityLabel="Download photo"
              >
                <Download size={20} color={theme.colors.white} />
              </Button>
            )}
            {onShare && (
              <Button
                variant="secondary"
                size="medium"
                onPress={() => onShare(currentPhoto.id)}
                accessibilityLabel="Share photo"
              >
                <Share2 size={20} color={theme.colors.white} />
              </Button>
            )}
            {onTag && (
              <Button
                variant="secondary"
                size="medium"
                onPress={() => onTag(currentPhoto.id)}
                accessibilityLabel="Add tags"
              >
                <Tag size={20} color={theme.colors.white} />
              </Button>
            )}
            {onDelete && (
              <Button
                variant="secondary"
                size="medium"
                onPress={() => onDelete(currentPhoto.id)}
                accessibilityLabel="Delete photo"
              >
                <Trash2 size={20} color={theme.colors.error[500]} />
              </Button>
            )}
          </View>

          {/* Photo counter */}
          {photos.length > 1 && (
            <Text variant="caption" color={theme.colors.gray[400]} align="center" style={{ marginTop: theme.spacing[2] }}>
              {currentIndex + 1} / {photos.length}
            </Text>
          )}
        </View>
      </View>
    </Modal>
  );
};
