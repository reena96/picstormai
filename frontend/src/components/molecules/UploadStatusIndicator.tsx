/**
 * UploadStatusIndicator Component (Molecule)
 * Complete upload item display with thumbnail, progress, status, and controls
 * Shows: thumbnail (48x48), filename, file size, status badge, progress bar, upload speed, ETA
 */

import React from 'react';
import { View, Image, StyleSheet, ViewStyle, ImageStyle } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { Text } from '../atoms/Text';
import { Badge } from '../atoms/Badge';
import { Button } from '../atoms/Button';
import { ProgressBar } from './ProgressBar';
import { CheckCircle, XCircle, Clock, Upload, RotateCw, X } from 'lucide-react-native';

export type UploadStatus = 'queued' | 'uploading' | 'complete' | 'failed';

export interface UploadStatusIndicatorProps {
  thumbnailUrl: string;
  filename: string;
  fileSize: number;
  status: UploadStatus;
  progress?: number; // 0-100, for 'uploading' status
  uploadSpeed?: number; // bytes/second
  eta?: number; // seconds remaining
  onRetry?: () => void;
  onCancel?: () => void;
  testID?: string;
  style?: ViewStyle;
}

export const UploadStatusIndicator: React.FC<UploadStatusIndicatorProps> = ({
  thumbnailUrl,
  filename,
  fileSize,
  status,
  progress = 0,
  uploadSpeed,
  eta,
  onRetry,
  onCancel,
  testID,
  style,
}) => {
  const { theme } = useTheme();

  const containerStyle: ViewStyle = {
    flexDirection: 'row',
    alignItems: 'center',
    padding: theme.spacing[3],
    backgroundColor: theme.colors.surface,
    borderRadius: theme.borderRadius.base,
    borderWidth: 1,
    borderColor: theme.colors.border,
    marginBottom: theme.spacing[2],
  };

  const thumbnailStyle: ImageStyle = {
    width: 48,
    height: 48,
    borderRadius: theme.borderRadius.sm,
    backgroundColor: theme.colors.gray[200],
    marginRight: theme.spacing[3],
  };

  const contentStyle: ViewStyle = {
    flex: 1,
  };

  const headerStyle: ViewStyle = {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: theme.spacing[1],
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const formatSpeed = (bytesPerSecond: number): string => {
    if (bytesPerSecond < 1024) return `${bytesPerSecond} B/s`;
    if (bytesPerSecond < 1024 * 1024) return `${(bytesPerSecond / 1024).toFixed(1)} KB/s`;
    return `${(bytesPerSecond / (1024 * 1024)).toFixed(1)} MB/s`;
  };

  const formatETA = (seconds: number): string => {
    if (seconds < 60) return `${Math.ceil(seconds)}s`;
    if (seconds < 3600) return `${Math.ceil(seconds / 60)}m`;
    return `${Math.ceil(seconds / 3600)}h`;
  };

  const getStatusBadge = () => {
    switch (status) {
      case 'queued':
        return <Badge variant="neutral" icon={Clock}>Queued</Badge>;
      case 'uploading':
        return <Badge variant="info" icon={Upload}>Uploading</Badge>;
      case 'complete':
        return <Badge variant="success" icon={CheckCircle}>Complete</Badge>;
      case 'failed':
        return <Badge variant="error" icon={XCircle}>Failed</Badge>;
      default:
        return null;
    }
  };

  const getActionButtons = () => {
    if (status === 'failed' && onRetry) {
      return (
        <Button variant="text" size="small" onPress={onRetry} accessibilityLabel="Retry upload">
          <RotateCw size={16} color={theme.colors.primary[500]} />
        </Button>
      );
    }
    if (status === 'uploading' && onCancel) {
      return (
        <Button variant="text" size="small" onPress={onCancel} accessibilityLabel="Cancel upload">
          <X size={16} color={theme.colors.error[500]} />
        </Button>
      );
    }
    return null;
  };

  return (
    <View style={[containerStyle, style]} testID={testID}>
      <Image source={{ uri: thumbnailUrl }} style={thumbnailStyle} resizeMode="cover" />

      <View style={contentStyle}>
        <View style={headerStyle}>
          <View style={{ flex: 1, marginRight: theme.spacing[2] }}>
            <Text variant="body" numberOfLines={1}>
              {filename}
            </Text>
            <Text variant="caption" color={theme.colors.text.secondary}>
              {formatFileSize(fileSize)}
            </Text>
          </View>
          {getStatusBadge()}
        </View>

        {status === 'uploading' && (
          <>
            <ProgressBar progress={progress} height={6} style={{ marginTop: theme.spacing[1] }} />
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: theme.spacing[1] }}>
              <Text variant="caption" color={theme.colors.text.secondary}>
                {progress.toFixed(0)}% complete
              </Text>
              {uploadSpeed && eta && (
                <Text variant="caption" color={theme.colors.text.secondary}>
                  {formatSpeed(uploadSpeed)} • {formatETA(eta)} remaining
                </Text>
              )}
            </View>
          </>
        )}
      </View>

      {getActionButtons()}
    </View>
  );
};
