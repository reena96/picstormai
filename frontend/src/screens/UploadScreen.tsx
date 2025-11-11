/**
 * Upload Screen - Photo Selection & Upload Management
 * Story 2.1: Photo Selection & Validation UI
 */

import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Image,
  Platform,
  ViewStyle,
  TextStyle,
  ImageStyle,
} from 'react-native';
import { useTheme } from '../hooks/useTheme';
import { Button } from '../components/atoms/Button';
import { Upload, X, AlertCircle } from 'lucide-react-native';
import {
  SelectedPhoto,
  validatePhoto,
  validatePhotoCount,
  MAX_PHOTOS_PER_UPLOAD,
  MAX_FILE_SIZE_MB,
  SUPPORTED_IMAGE_TYPES,
} from '../types/upload';

export const UploadScreen: React.FC = () => {
  const { theme } = useTheme();
  const [selectedPhotos, setSelectedPhotos] = useState<SelectedPhoto[]>([]);
  const [errors, setErrors] = useState<string[]>([]);

  const handleSelectPhotos = useCallback(async () => {
    try {
      // Web-specific file input
      if (Platform.OS === 'web') {
        const input = document.createElement('input');
        input.type = 'file';
        input.multiple = true;
        input.accept = SUPPORTED_IMAGE_TYPES.join(',');

        input.onchange = async (e: Event) => {
          const target = e.target as HTMLInputElement;
          const files = target.files;
          if (!files) return;

          const newErrors: string[] = [];
          const newPhotos: SelectedPhoto[] = [];

          // Validate count
          const totalCount = selectedPhotos.length + files.length;
          const countError = validatePhotoCount(totalCount);
          if (countError) {
            newErrors.push(countError);
            setErrors(newErrors);
            return;
          }

          // Process files
          for (let i = 0; i < files.length; i++) {
            const file = files[i];
            const photo: SelectedPhoto = {
              id: `${Date.now()}-${i}`,
              uri: URL.createObjectURL(file),
              name: file.name,
              type: file.type,
              size: file.size,
              thumbnail: URL.createObjectURL(file),
            };

            const validationError = validatePhoto(photo);
            if (validationError) {
              newErrors.push(`${file.name}: ${validationError}`);
            } else {
              newPhotos.push(photo);
            }
          }

          setSelectedPhotos(prev => [...prev, ...newPhotos]);
          setErrors(newErrors);
        };

        input.click();
      } else {
        // Mobile: Would use react-native-document-picker
        // Placeholder for now
        setErrors(['Mobile photo selection coming soon']);
      }
    } catch (error) {
      setErrors(['Error selecting photos']);
    }
  }, [selectedPhotos]);

  const handleRemovePhoto = useCallback((photoId: string) => {
    setSelectedPhotos(prev => prev.filter(p => p.id !== photoId));
  }, []);

  const handleDrop = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();

    if (Platform.OS !== 'web') return;

    const files = Array.from(e.dataTransfer.files);
    const newErrors: string[] = [];
    const newPhotos: SelectedPhoto[] = [];

    // Validate count
    const totalCount = selectedPhotos.length + files.length;
    const countError = validatePhotoCount(totalCount);
    if (countError) {
      newErrors.push(countError);
      setErrors(newErrors);
      return;
    }

    // Process files
    files.forEach((file, i) => {
      const photo: SelectedPhoto = {
        id: `${Date.now()}-${i}`,
        uri: URL.createObjectURL(file),
        name: file.name,
        type: file.type,
        size: file.size,
        thumbnail: URL.createObjectURL(file),
      };

      const validationError = validatePhoto(photo);
      if (validationError) {
        newErrors.push(`${file.name}: ${validationError}`);
      } else {
        newPhotos.push(photo);
      }
    });

    setSelectedPhotos(prev => [...prev, ...newPhotos]);
    setErrors(newErrors);
  }, [selectedPhotos]);

  const handleDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const styles = StyleSheet.create<{
    container: ViewStyle;
    header: ViewStyle;
    title: TextStyle;
    subtitle: TextStyle;
    dropzone: ViewStyle;
    dropzoneText: TextStyle;
    dropzoneHint: TextStyle;
    errorContainer: ViewStyle;
    errorText: TextStyle;
    photosContainer: ViewStyle;
    photoGrid: ViewStyle;
    photoCard: ViewStyle;
    photoImage: ImageStyle;
    photoInfo: ViewStyle;
    photoName: TextStyle;
    photoSize: TextStyle;
    removeButton: ViewStyle;
    statsContainer: ViewStyle;
    statsText: TextStyle;
    buttonContainer: ViewStyle;
  }>({
    container: {
      flex: 1,
      backgroundColor: theme.colors.background,
    },
    header: {
      padding: theme.spacing[4],
      backgroundColor: theme.colors.surface,
      borderBottomWidth: 1,
      borderBottomColor: theme.colors.border,
    },
    title: {
      fontSize: theme.typography.fontSize['2xl'],
      fontWeight: theme.typography.fontWeight.bold as TextStyle['fontWeight'],
      color: theme.colors.text.primary,
      fontFamily: theme.typography.fontFamily.primary,
      marginBottom: theme.spacing[1],
    },
    subtitle: {
      fontSize: theme.typography.fontSize.sm,
      color: theme.colors.text.secondary,
      fontFamily: theme.typography.fontFamily.primary,
    },
    dropzone: {
      margin: theme.spacing[4],
      padding: theme.spacing[8],
      borderWidth: 2,
      borderStyle: 'dashed',
      borderColor: theme.colors.primary,
      borderRadius: 12,
      backgroundColor: theme.colors.surface,
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: 200,
    },
    dropzoneText: {
      fontSize: theme.typography.fontSize.lg,
      fontWeight: theme.typography.fontWeight.semibold as TextStyle['fontWeight'],
      color: theme.colors.text.primary,
      fontFamily: theme.typography.fontFamily.primary,
      marginTop: theme.spacing[3],
      marginBottom: theme.spacing[2],
    },
    dropzoneHint: {
      fontSize: theme.typography.fontSize.sm,
      color: theme.colors.text.secondary,
      fontFamily: theme.typography.fontFamily.primary,
      textAlign: 'center',
    },
    errorContainer: {
      marginHorizontal: theme.spacing[4],
      marginBottom: theme.spacing[3],
      padding: theme.spacing[3],
      backgroundColor: '#FEE2E2',
      borderRadius: 8,
      flexDirection: 'row',
      alignItems: 'flex-start',
    },
    errorText: {
      flex: 1,
      fontSize: theme.typography.fontSize.sm,
      color: '#DC2626',
      fontFamily: theme.typography.fontFamily.primary,
      marginLeft: theme.spacing[2],
    },
    photosContainer: {
      flex: 1,
    },
    photoGrid: {
      padding: theme.spacing[4],
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: theme.spacing[3],
    },
    photoCard: {
      width: Platform.OS === 'web' ? 150 : '45%',
      backgroundColor: theme.colors.surface,
      borderRadius: 8,
      overflow: 'hidden',
      borderWidth: 1,
      borderColor: theme.colors.border,
    },
    photoImage: {
      width: '100%',
      height: 150,
      backgroundColor: theme.colors.muted,
    },
    photoInfo: {
      padding: theme.spacing[2],
    },
    photoName: {
      fontSize: theme.typography.fontSize.xs,
      color: theme.colors.text.primary,
      fontFamily: theme.typography.fontFamily.primary,
      marginBottom: theme.spacing[1],
      numberOfLines: 1,
      ellipsizeMode: 'middle',
    },
    photoSize: {
      fontSize: theme.typography.fontSize.xs,
      color: theme.colors.text.secondary,
      fontFamily: theme.typography.fontFamily.primary,
    },
    removeButton: {
      position: 'absolute',
      top: theme.spacing[2],
      right: theme.spacing[2],
      backgroundColor: 'rgba(0, 0, 0, 0.6)',
      borderRadius: 12,
      padding: theme.spacing[1],
    },
    statsContainer: {
      padding: theme.spacing[4],
      backgroundColor: theme.colors.surface,
      borderTopWidth: 1,
      borderTopColor: theme.colors.border,
    },
    statsText: {
      fontSize: theme.typography.fontSize.sm,
      color: theme.colors.text.secondary,
      fontFamily: theme.typography.fontFamily.primary,
      textAlign: 'center',
      marginBottom: theme.spacing[2],
    },
    buttonContainer: {
      paddingHorizontal: theme.spacing[4],
      paddingBottom: theme.spacing[4],
    },
  });

  const totalSize = selectedPhotos.reduce((sum, p) => sum + p.size, 0);

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Upload Photos</Text>
        <Text style={styles.subtitle}>
          Select up to {MAX_PHOTOS_PER_UPLOAD} photos (max {MAX_FILE_SIZE_MB}MB each)
        </Text>
      </View>

      {selectedPhotos.length === 0 && (
        <View
          style={styles.dropzone}
          // @ts-ignore - Web-only props
          onDrop={handleDrop}
          onDragOver={handleDragOver}
        >
          <Upload size={48} color={theme.colors.primary} />
          <Text style={styles.dropzoneText}>Drop photos here</Text>
          <Text style={styles.dropzoneHint}>or</Text>
          <Button
            variant="primary"
            onPress={handleSelectPhotos}
            style={{ marginTop: theme.spacing[3] }}
          >
            Select Photos
          </Button>
          <Text style={styles.dropzoneHint}>
            Supports JPG, PNG, GIF, WebP
          </Text>
        </View>
      )}

      {errors.length > 0 && (
        <View style={styles.errorContainer}>
          <AlertCircle size={20} color="#DC2626" />
          <View style={{ flex: 1 }}>
            {errors.map((error, index) => (
              <Text key={index} style={styles.errorText}>{error}</Text>
            ))}
          </View>
        </View>
      )}

      {selectedPhotos.length > 0 && (
        <>
          <ScrollView style={styles.photosContainer}>
            <View style={styles.photoGrid}>
              {selectedPhotos.map((photo) => (
                <View key={photo.id} style={styles.photoCard}>
                  <Image
                    source={{ uri: photo.thumbnail || photo.uri }}
                    style={styles.photoImage}
                    resizeMode="cover"
                  />
                  <TouchableOpacity
                    style={styles.removeButton}
                    onPress={() => handleRemovePhoto(photo.id)}
                  >
                    <X size={16} color="#FFFFFF" />
                  </TouchableOpacity>
                  <View style={styles.photoInfo}>
                    <Text style={styles.photoName} numberOfLines={1}>{photo.name}</Text>
                    <Text style={styles.photoSize}>{formatFileSize(photo.size)}</Text>
                  </View>
                </View>
              ))}
            </View>
          </ScrollView>

          <View style={styles.statsContainer}>
            <Text style={styles.statsText}>
              {selectedPhotos.length} {selectedPhotos.length === 1 ? 'photo' : 'photos'} selected • {formatFileSize(totalSize)} total
            </Text>
            <View style={styles.buttonContainer}>
              <Button
                variant="primary"
                onPress={() => {/* Upload will be implemented in Story 2.4 */}}
                disabled={selectedPhotos.length === 0}
              >
                Start Upload
              </Button>
              <Button
                variant="secondary"
                onPress={handleSelectPhotos}
                style={{ marginTop: theme.spacing[2] }}
              >
                Add More Photos
              </Button>
            </View>
          </View>
        </>
      )}
    </View>
  );
};

export default UploadScreen;
