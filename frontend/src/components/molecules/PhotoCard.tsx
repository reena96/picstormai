/**
 * PhotoCard Component (Molecule)
 * 1:1 aspect ratio photo card with hover overlay, metadata, and loading state
 * Accessible with keyboard navigation and screen reader support
 */

import React, { useState } from 'react';
import { View, Image, Pressable, StyleSheet, ViewStyle, Platform } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { Text } from '../atoms/Text';
import { Spinner } from '../atoms/Spinner';

export interface PhotoCardProps {
  photoUrl: string;
  thumbnailUrl: string;
  filename: string;
  uploadDate: Date;
  fileSize?: number;
  tags?: string[];
  onPress: () => void;
  loading?: boolean;
  testID?: string;
  style?: ViewStyle;
}

export const PhotoCard: React.FC<PhotoCardProps> = ({
  photoUrl,
  thumbnailUrl,
  filename,
  uploadDate,
  fileSize,
  tags = [],
  onPress,
  loading = false,
  testID,
  style,
}) => {
  const { theme } = useTheme();
  const [imageLoaded, setImageLoaded] = useState(false);
  const [isHovered, setIsHovered] = useState(false);

  const containerStyle: ViewStyle = {
    aspectRatio: 1,
    borderRadius: theme.borderRadius.base,
    overflow: 'hidden',
    backgroundColor: theme.colors.surface,
    ...theme.shadows.sm,
  };

  const imageStyle: ViewStyle = {
    width: '100%',
    height: '100%',
  };

  const overlayStyle: ViewStyle = {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    opacity: isHovered && Platform.OS === 'web' ? 1 : 0,
    justifyContent: 'center',
    alignItems: 'center',
  };

  const metadataStyle: ViewStyle = {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    padding: theme.spacing[2],
  };

  const skeletonStyle: ViewStyle = {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: theme.colors.gray[200],
    justifyContent: 'center',
    alignItems: 'center',
  };

  const formatDate = (date: Date): string => {
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

  return (
    <Pressable
      onPress={onPress}
      onHoverIn={() => setIsHovered(true)}
      onHoverOut={() => setIsHovered(false)}
      accessibilityRole="button"
      accessibilityLabel={`Photo: ${filename}, uploaded ${formatDate(uploadDate)}`}
      testID={testID}
      style={({ pressed }) => [
        containerStyle,
        pressed && { transform: [{ scale: 0.98 }], ...theme.shadows.md },
        style,
      ]}
    >
      <Image
        source={{ uri: thumbnailUrl }}
        style={imageStyle}
        resizeMode="cover"
        onLoad={() => setImageLoaded(true)}
        accessibilityIgnoresInvertColors
      />

      {/* Skeleton loading state */}
      {(!imageLoaded || loading) && (
        <View style={skeletonStyle}>
          <Spinner size="medium" />
        </View>
      )}

      {/* Hover overlay (web only) */}
      <View style={overlayStyle} pointerEvents="none">
        <Text variant="body" color={theme.colors.white}>
          View Photo
        </Text>
      </View>

      {/* Metadata footer */}
      {imageLoaded && !loading && (
        <View style={metadataStyle} pointerEvents="none">
          <Text variant="caption" color={theme.colors.white} numberOfLines={1}>
            {filename}
          </Text>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 2 }}>
            <Text variant="caption" color={theme.colors.gray[300]}>
              {formatDate(uploadDate)}
            </Text>
            {fileSize && (
              <Text variant="caption" color={theme.colors.gray[300]}>
                {formatFileSize(fileSize)}
              </Text>
            )}
          </View>
        </View>
      )}
    </Pressable>
  );
};
