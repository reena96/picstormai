/**
 * Upload Screen - Photo Selection & Upload Management
 * Story 2.1: Photo Selection & Validation UI
 * Story 2.8: SSE Client Integration
 */

import React, { useState, useCallback, useEffect } from 'react';
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
import { useNavigation } from '@react-navigation/native';
import { useTheme } from '../hooks/useTheme';
import { useAuth } from '../hooks/useAuth';
import { useSSE } from '../hooks/useSSE';
import { Button } from '../components/atoms/Button';
import { UploadCompletionModal } from '../components/molecules/UploadCompletionModal';
import { Upload, X, AlertCircle, Wifi, WifiOff, ArrowLeft } from 'lucide-react-native';
import {
  SelectedPhoto,
  validatePhoto,
  validatePhotoCount,
  MAX_PHOTOS_PER_UPLOAD,
  MAX_FILE_SIZE_MB,
  SUPPORTED_IMAGE_TYPES,
} from '../types/upload';
import { uploadService, UploadProgress } from '../services/uploadService';
import { apiService } from '../services/api';
import {
  UploadProgressMessage,
  PhotoUploadedMessage,
  PhotoFailedMessage,
  SessionCompletedMessage,
} from '../types/sse';

export const UploadScreen: React.FC = () => {
  const navigation = useNavigation();
  const { theme } = useTheme();
  const { user } = useAuth();
  const [selectedPhotos, setSelectedPhotos] = useState<SelectedPhoto[]>([]);
  const [errors, setErrors] = useState<string[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<Map<string, UploadProgress>>(new Map());
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const [authToken, setAuthToken] = useState<string | null>(null);
  const [completionModalVisible, setCompletionModalVisible] = useState(false);
  const [completionStats, setCompletionStats] = useState<SessionCompletedMessage | null>(null);

  // Get API base URL
  const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

  // Build SSE URL for current session
  const sseUrl = currentSessionId
    ? `${API_BASE_URL}/api/upload/sessions/${currentSessionId}/stream`
    : '';

  // SSE connection for real-time progress updates
  const { isConnected, connectionStatus, subscribe, unsubscribe } = useSSE({
    url: sseUrl,
    authToken,
    enabled: !!currentSessionId && !!authToken,
    onStateResync: useCallback(async () => {
      // AC5: Sync state on reconnect
      if (!currentSessionId || !authToken) return;

      try {
        console.log('[UploadScreen] Syncing state after reconnect');
        const response = await fetch(
          `${API_BASE_URL}/api/upload/sessions/${currentSessionId}`,
          {
            headers: {
              'Authorization': `Bearer ${authToken}`,
            },
          }
        );

        if (!response.ok) {
          throw new Error('Failed to fetch session state');
        }

        const session = await response.json();

        // Sync local progress state
        const updatedProgress = new Map<string, UploadProgress>();
        session.photos?.forEach((photo: any) => {
          const status = photo.uploadStatus?.toLowerCase() || 'queued';
          updatedProgress.set(photo.id, {
            photoId: photo.id,
            status: status as UploadProgress['status'],
            progress: status === 'completed' ? 100 : 0,
            bytesUploaded: 0,
            totalBytes: 0,
          });
        });

        setUploadProgress(updatedProgress);
        console.log('[UploadScreen] State synced successfully');
      } catch (error) {
        console.error('[UploadScreen] Failed to sync state:', error);
      }
    }, [currentSessionId, authToken, API_BASE_URL]),
  });

  // Load auth token on mount
  useEffect(() => {
    const loadToken = async () => {
      const token = await apiService.getAccessToken();
      setAuthToken(token);
    };
    loadToken();
  }, []);

  // Subscribe to SSE messages when session starts
  useEffect(() => {
    if (!currentSessionId || !authToken) return;

    const handleSSEMessage = (message: UploadProgressMessage) => {
      console.log('[UploadScreen] SSE message received:', message);

      switch (message.type) {
        case 'PHOTO_UPLOADED':
          handlePhotoUploaded(message as PhotoUploadedMessage);
          break;
        case 'PHOTO_FAILED':
          handlePhotoFailed(message as PhotoFailedMessage);
          break;
        case 'SESSION_COMPLETED':
          handleSessionCompleted(message as SessionCompletedMessage);
          break;
      }
    };

    subscribe(handleSSEMessage);

    return () => {
      unsubscribe();
    };
  }, [currentSessionId, authToken, subscribe, unsubscribe]);

  // Handle SSE message: Photo uploaded successfully
  const handlePhotoUploaded = useCallback((message: PhotoUploadedMessage) => {
    console.log('[UploadScreen] Photo uploaded:', message.photoId);
    setUploadProgress((prev) => {
      const updated = new Map(prev);
      updated.set(message.photoId, {
        photoId: message.photoId,
        status: 'completed',
        progress: 100,
        bytesUploaded: 0,
        totalBytes: 0,
      });
      return updated;
    });
  }, []);

  // Handle SSE message: Photo upload failed
  const handlePhotoFailed = useCallback((message: PhotoFailedMessage) => {
    console.log('[UploadScreen] Photo failed:', message.photoId, message.reason);
    setUploadProgress((prev) => {
      const updated = new Map(prev);
      updated.set(message.photoId, {
        photoId: message.photoId,
        status: 'failed',
        progress: 0,
        bytesUploaded: 0,
        totalBytes: 0,
        error: message.reason,
      });
      return updated;
    });
    setErrors(prev => [...prev, `Photo ${message.photoId}: ${message.reason}`]);
  }, []);

  // Handle SSE message: Session completed
  const handleSessionCompleted = useCallback((message: SessionCompletedMessage) => {
    console.log('[UploadScreen] Session completed:', message);

    // Clear fallback timer if it exists
    const fallbackTimer = (window as any).__uploadCompletionTimer;
    if (fallbackTimer) {
      clearTimeout(fallbackTimer);
      (window as any).__uploadCompletionTimer = null;
    }

    setCompletionStats(message);
    setCompletionModalVisible(true);
    setIsUploading(false);
    // Clear session ID to stop SSE reconnection attempts
    // The backend closes the SSE stream when session completes
    setCurrentSessionId(null);
  }, []);

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

  // Handle modal actions
  const handleViewPhotos = useCallback(() => {
    setCompletionModalVisible(false);
    // @ts-ignore - Navigation type inference
    navigation.navigate('Gallery', { highlightSessionId: currentSessionId });
  }, [navigation, currentSessionId]);

  const handleRetryFailed = useCallback(() => {
    setCompletionModalVisible(false);
    // TODO: Implement retry logic in Story 2.12
    console.log('[UploadScreen] Retry failed uploads - to be implemented in Story 2.12');
  }, []);

  const handleCloseModal = useCallback(() => {
    setCompletionModalVisible(false);
  }, []);

  const handleStartUpload = useCallback(async () => {
    if (selectedPhotos.length === 0 || !user) {
      console.log('UploadScreen: Cannot upload - selectedPhotos:', selectedPhotos.length, 'user:', !!user);
      return;
    }

    console.log('UploadScreen: Starting upload for', selectedPhotos.length, 'photos');
    console.log('UploadScreen: User object:', user);
    setIsUploading(true);
    setErrors([]);

    try {
      // Try to get token directly from storage to debug
      const { storage } = await import('../utils/storage');
      const { STORAGE_KEYS } = await import('../services/api');
      const directToken = await storage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      console.log('UploadScreen: Direct token from storage:', directToken ? directToken.substring(0, 20) + '...' : 'null');

      const token = await apiService.getAccessToken();

      console.log('UploadScreen: Auth token via apiService:', token ? token.substring(0, 20) + '...' : 'null');
      if (!token) {
        console.error('UploadScreen: No auth token found!');
        throw new Error('Not authenticated. Please log in first.');
      }
      console.log('UploadScreen: API_BASE_URL:', API_BASE_URL);

      console.log('UploadScreen: Calling uploadService.startUploadSession');
      const sessionId = await uploadService.startUploadSession(
        selectedPhotos,
        API_BASE_URL,
        token,
        (progress) => {
          // Progress updates are handled via SSE messages - no need to log here
          setUploadProgress(prev => new Map(prev).set(progress.photoId, progress));
        },
        (sessionId) => {
          console.log('UploadScreen: Upload complete via local callback! SessionId:', sessionId);
          // Fallback: If SSE doesn't send SESSION_COMPLETED within 2 seconds, handle completion locally
          const fallbackTimer = setTimeout(() => {
            console.warn('[UploadScreen] SSE SESSION_COMPLETED not received, using local fallback');

            // Count completion stats from local upload progress state
            let uploadedCount = 0;
            let failedCount = 0;

            // Check if we have progress data from SSE
            if (uploadProgress.size > 0) {
              uploadProgress.forEach((progress) => {
                if (progress.status === 'completed') {
                  uploadedCount++;
                } else if (progress.status === 'failed') {
                  failedCount++;
                }
              });
            } else {
              // No SSE progress updates received - assume all succeeded
              // since the local callback only fires when uploads complete
              console.log('[UploadScreen] No SSE progress data, assuming all uploads succeeded');
              uploadedCount = selectedPhotos.length;
              failedCount = 0;
            }

            // Show completion modal with stats
            const stats: SessionCompletedMessage = {
              type: 'SESSION_COMPLETED',
              sessionId: sessionId,
              totalCount: selectedPhotos.length,
              uploadedCount,
              failedCount,
              timestamp: new Date().toISOString(),
            };

            console.log('[UploadScreen] Fallback stats:', stats);
            setCompletionStats(stats);
            setCompletionModalVisible(true);
            setIsUploading(false);
            setCurrentSessionId(null);
          }, 2000);

          // Store timer so we can clear it if SSE message arrives
          (window as any).__uploadCompletionTimer = fallbackTimer;
        },
        (photoId, error) => {
          console.error('UploadScreen: Photo upload failed:', photoId, error);
          // Don't add to errors here - let SSE PHOTO_FAILED handle it
        }
      );

      console.log('UploadScreen: Upload session started:', sessionId);
      // Set session ID to enable SSE connection
      setCurrentSessionId(sessionId);
    } catch (error: any) {
      console.error('UploadScreen: Upload failed:', error);
      setErrors([error.message || 'Upload failed']);
      setIsUploading(false);
    }
  }, [selectedPhotos, user, API_BASE_URL]);

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
    headerTop: ViewStyle;
    backButton: ViewStyle;
    title: TextStyle;
    subtitle: TextStyle;
    connectionBanner: ViewStyle;
    connectionBannerConnected: ViewStyle;
    connectionBannerConnecting: ViewStyle;
    connectionBannerDisconnected: ViewStyle;
    connectionBannerText: TextStyle;
    connectionBannerTextConnected: TextStyle;
    connectionBannerTextConnecting: TextStyle;
    connectionBannerTextDisconnected: TextStyle;
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
    headerTop: {
      flexDirection: 'row',
      alignItems: 'center',
      marginBottom: theme.spacing[2],
    },
    backButton: {
      padding: theme.spacing[2],
      marginRight: theme.spacing[2],
      marginLeft: -theme.spacing[2],
    },
    title: {
      fontSize: theme.typography.fontSize['2xl'],
      fontWeight: theme.typography.fontWeight.bold as TextStyle['fontWeight'],
      color: theme.colors.text.primary,
      fontFamily: theme.typography.fontFamily.primary,
    },
    subtitle: {
      fontSize: theme.typography.fontSize.sm,
      color: theme.colors.text.secondary,
      fontFamily: theme.typography.fontFamily.primary,
    },
    connectionBanner: {
      flexDirection: 'row',
      alignItems: 'center',
      padding: theme.spacing[3],
      marginHorizontal: theme.spacing[4],
      marginTop: theme.spacing[3],
      borderRadius: 8,
      gap: theme.spacing[2],
    },
    connectionBannerConnected: {
      backgroundColor: '#D1FAE5',
      borderWidth: 1,
      borderColor: '#10B981',
    },
    connectionBannerConnecting: {
      backgroundColor: '#FEF3C7',
      borderWidth: 1,
      borderColor: '#F59E0B',
    },
    connectionBannerDisconnected: {
      backgroundColor: '#FEE2E2',
      borderWidth: 1,
      borderColor: '#EF4444',
    },
    connectionBannerText: {
      fontSize: theme.typography.fontSize.sm,
      fontFamily: theme.typography.fontFamily.primary,
      fontWeight: theme.typography.fontWeight.medium as TextStyle['fontWeight'],
    },
    connectionBannerTextConnected: {
      color: '#065F46',
    },
    connectionBannerTextConnecting: {
      color: '#92400E',
    },
    connectionBannerTextDisconnected: {
      color: '#991B1B',
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
    },
    photoCard: {
      width: Platform.OS === 'web' ? 150 : '45%',
      backgroundColor: theme.colors.surface,
      borderRadius: 8,
      overflow: 'hidden',
      borderWidth: 1,
      borderColor: theme.colors.border,
      margin: theme.spacing[1.5],
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
        <View style={styles.headerTop}>
          <TouchableOpacity
            onPress={() => navigation.navigate('Home' as never)}
            style={styles.backButton}
            accessibilityLabel="Back to Home"
          >
            <ArrowLeft size={24} color={theme.text} />
          </TouchableOpacity>
          <Text style={styles.title}>Upload Photos</Text>
        </View>
        <Text style={styles.subtitle}>
          Select up to {MAX_PHOTOS_PER_UPLOAD} photos (max {MAX_FILE_SIZE_MB}MB each)
        </Text>
      </View>

      {/* SSE Connection Status Banner */}
      {isUploading && currentSessionId && (
        <View style={[
          styles.connectionBanner,
          connectionStatus === 'connected' ? styles.connectionBannerConnected :
          connectionStatus === 'connecting' ? styles.connectionBannerConnecting :
          styles.connectionBannerDisconnected
        ]}>
          {connectionStatus === 'connected' && <Wifi size={16} color="#10B981" />}
          {connectionStatus === 'connecting' && <Wifi size={16} color="#F59E0B" />}
          {connectionStatus === 'disconnected' && <WifiOff size={16} color="#EF4444" />}
          <Text style={[
            styles.connectionBannerText,
            connectionStatus === 'connected' ? styles.connectionBannerTextConnected :
            connectionStatus === 'connecting' ? styles.connectionBannerTextConnecting :
            styles.connectionBannerTextDisconnected
          ]}>
            {connectionStatus === 'connected' && 'Connected - Real-time updates active'}
            {connectionStatus === 'connecting' && 'Connecting to real-time updates...'}
            {connectionStatus === 'disconnected' && 'Reconnecting...'}
          </Text>
        </View>
      )}

      {selectedPhotos.length === 0 && (
        <View
          style={styles.dropzone}
          // @ts-ignore - Web-only props
          onDrop={handleDrop}
          onDragOver={handleDragOver}
        >
          <Upload size={48} color={theme.colors.primary[500]} />
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
                onPress={handleStartUpload}
                disabled={selectedPhotos.length === 0 || isUploading}
                loading={isUploading}
              >
                {isUploading ? 'Uploading...' : 'Start Upload'}
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

      {/* Upload Completion Modal */}
      {completionStats && (
        <UploadCompletionModal
          visible={completionModalVisible}
          totalPhotos={completionStats.totalCount}
          successfulPhotos={completionStats.uploadedCount}
          failedPhotos={completionStats.failedCount}
          onViewPhotos={handleViewPhotos}
          onRetryFailed={completionStats.failedCount > 0 ? handleRetryFailed : undefined}
          onClose={handleCloseModal}
          soundEnabled={true}
        />
      )}
    </View>
  );
};

export default UploadScreen;
