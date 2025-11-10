/**
 * UploadDashboard Component (Organism)
 * Aggregate progress display with categorized upload lists
 * Features: sticky header, collapsible sections, minimize toggle, batch actions
 */

import React, { useState } from 'react';
import { View, ScrollView, StyleSheet, ViewStyle } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { Text } from '../atoms/Text';
import { Button } from '../atoms/Button';
import { ProgressBar } from '../molecules/ProgressBar';
import { UploadStatusIndicator, UploadStatusIndicatorProps, UploadStatus } from '../molecules/UploadStatusIndicator';
import { ChevronDown, ChevronUp, Minimize2, X } from 'lucide-react-native';

export interface UploadItem extends UploadStatusIndicatorProps {
  id: string;
  status: UploadStatus;
}

export interface UploadDashboardProps {
  uploads: UploadItem[];
  onCancelUpload: (uploadId: string) => void;
  onRetryUpload: (uploadId: string) => void;
  onCancelAll: () => void;
  minimized?: boolean;
  onToggleMinimize: () => void;
  testID?: string;
  style?: ViewStyle;
}

export const UploadDashboard: React.FC<UploadDashboardProps> = ({
  uploads,
  onCancelUpload,
  onRetryUpload,
  onCancelAll,
  minimized = false,
  onToggleMinimize,
  testID,
  style,
}) => {
  const { theme } = useTheme();
  const [completedCollapsed, setCompletedCollapsed] = useState(false);
  const [failedCollapsed, setFailedCollapsed] = useState(false);

  // Categorize uploads
  const activeUploads = uploads.filter(u => u.status === 'uploading' || u.status === 'queued');
  const completedUploads = uploads.filter(u => u.status === 'complete');
  const failedUploads = uploads.filter(u => u.status === 'failed');

  // Calculate aggregate progress
  const totalFiles = uploads.length;
  const completedFiles = completedUploads.length;
  const uploadingFiles = activeUploads.filter(u => u.status === 'uploading');
  const avgProgress = uploadingFiles.length > 0
    ? uploadingFiles.reduce((sum, u) => sum + (u.progress || 0), 0) / uploadingFiles.length
    : 0;
  const overallProgress = totalFiles > 0
    ? ((completedFiles + avgProgress / 100 * uploadingFiles.length) / totalFiles) * 100
    : 0;

  // Estimate remaining time
  const avgSpeed = uploadingFiles.reduce((sum, u) => sum + (u.uploadSpeed || 0), 0) / Math.max(uploadingFiles.length, 1);
  const remainingBytes = uploadingFiles.reduce((sum, u) => sum + u.fileSize * (100 - (u.progress || 0)) / 100, 0);
  const etaMinutes = avgSpeed > 0 ? Math.ceil(remainingBytes / avgSpeed / 60) : 0;

  const containerStyle: ViewStyle = {
    backgroundColor: theme.colors.surface,
    borderTopWidth: 1,
    borderTopColor: theme.colors.border,
    ...theme.shadows.lg,
  };

  const headerStyle: ViewStyle = {
    padding: theme.spacing[4],
    backgroundColor: theme.colors.background,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
  };

  const sectionHeaderStyle: ViewStyle = {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: theme.spacing[3],
    backgroundColor: theme.colors.gray[100],
  };

  if (minimized) {
    return (
      <View style={[containerStyle, { padding: theme.spacing[3] }, style]} testID={testID}>
        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
          <View style={{ flex: 1, marginRight: theme.spacing[4] }}>
            <Text variant="bodySmall" weight="medium">
              {completedFiles} of {totalFiles} photos • {etaMinutes}m remaining
            </Text>
            <ProgressBar progress={overallProgress} height={4} style={{ marginTop: theme.spacing[1] }} />
          </View>
          <Button variant="text" size="small" onPress={onToggleMinimize} accessibilityLabel="Maximize upload dashboard">
            <ChevronUp size={20} color={theme.colors.text.primary} />
          </Button>
        </View>
      </View>
    );
  }

  return (
    <View style={[containerStyle, { maxHeight: 600 }, style]} testID={testID}>
      {/* Header with aggregate progress */}
      <View style={headerStyle}>
        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: theme.spacing[2] }}>
          <Text variant="h4">Upload Progress</Text>
          <View style={{ flexDirection: 'row', gap: theme.spacing[2] }}>
            <Button variant="text" size="small" onPress={onCancelAll} accessibilityLabel="Cancel all uploads">
              <X size={20} color={theme.colors.error[500]} />
            </Button>
            <Button variant="text" size="small" onPress={onToggleMinimize} accessibilityLabel="Minimize upload dashboard">
              <Minimize2 size={20} color={theme.colors.text.primary} />
            </Button>
          </View>
        </View>

        <Text variant="body" color={theme.colors.text.secondary} style={{ marginBottom: theme.spacing[2] }}>
          {completedFiles} of {totalFiles} photos • {etaMinutes > 0 ? `${etaMinutes} min remaining` : 'Complete'}
        </Text>

        <ProgressBar progress={overallProgress} height={8} />
      </View>

      {/* Scrollable upload list */}
      <ScrollView style={{ flex: 1 }}>
        {/* Active uploads section */}
        {activeUploads.length > 0 && (
          <View>
            <View style={sectionHeaderStyle}>
              <Text variant="label" weight="semibold">
                Active ({activeUploads.length})
              </Text>
            </View>
            {activeUploads.map((upload) => (
              <UploadStatusIndicator
                key={upload.id}
                {...upload}
                onCancel={() => onCancelUpload(upload.id)}
              />
            ))}
          </View>
        )}

        {/* Completed section */}
        {completedUploads.length > 0 && (
          <View>
            <View style={sectionHeaderStyle}>
              <Text variant="label" weight="semibold">
                Completed ({completedUploads.length})
              </Text>
              <Button
                variant="text"
                size="small"
                onPress={() => setCompletedCollapsed(!completedCollapsed)}
                accessibilityLabel={completedCollapsed ? 'Expand completed' : 'Collapse completed'}
              >
                {completedCollapsed ? <ChevronDown size={16} /> : <ChevronUp size={16} />}
              </Button>
            </View>
            {!completedCollapsed && completedUploads.map((upload) => (
              <UploadStatusIndicator key={upload.id} {...upload} />
            ))}
          </View>
        )}

        {/* Failed section */}
        {failedUploads.length > 0 && (
          <View>
            <View style={sectionHeaderStyle}>
              <Text variant="label" weight="semibold" color={theme.colors.error[600]}>
                Failed ({failedUploads.length})
              </Text>
              <Button
                variant="text"
                size="small"
                onPress={() => setFailedCollapsed(!failedCollapsed)}
                accessibilityLabel={failedCollapsed ? 'Expand failed' : 'Collapse failed'}
              >
                {failedCollapsed ? <ChevronDown size={16} /> : <ChevronUp size={16} />}
              </Button>
            </View>
            {!failedCollapsed && failedUploads.map((upload) => (
              <UploadStatusIndicator
                key={upload.id}
                {...upload}
                onRetry={() => onRetryUpload(upload.id)}
              />
            ))}
          </View>
        )}
      </ScrollView>
    </View>
  );
};
