/**
 * Upload Dashboard - Progress Tracking UI
 * Story 2.5: Upload Progress UI
 */

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { UploadProgress } from '../../services/uploadService';

interface UploadDashboardProps {
  photos: UploadProgress[];
}

export const UploadDashboard: React.FC<UploadDashboardProps> = ({ photos }) => {
  const completedCount = photos.filter(p => p.status === 'completed').length;
  const overallProgress = Math.round((completedCount / photos.length) * 100);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>
        Upload Progress: {completedCount}/{photos.length} ({overallProgress}%)
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { padding: 16 },
  title: { fontSize: 18, fontWeight: 'bold' },
});
