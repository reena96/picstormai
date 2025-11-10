/**
 * Settings Screen
 * Wraps SettingsPanel organism and handles API integration
 */

import React, { useState, useEffect } from 'react';
import { View, ActivityIndicator, StyleSheet, Alert } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { SettingsPanel, UserSettings } from '../components/organisms/SettingsPanel';
import { useTheme } from '../hooks/useTheme';
import { userService } from '../services/userService';
import { UserPreferences } from '../types/user';
import { Text } from '../components/atoms/Text';

export const SettingsScreen: React.FC = () => {
  const { theme } = useTheme();
  const navigation = useNavigation();
  const [settings, setSettings] = useState<UserSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Fetch preferences on mount
  useEffect(() => {
    loadPreferences();
  }, []);

  const loadPreferences = async () => {
    try {
      setLoading(true);
      setError(null);
      const preferences = await userService.getPreferences();

      // Map backend preferences to SettingsPanel format
      const mappedSettings: UserSettings = {
        notifications: {
          uploadComplete: preferences.uploadCompleteNotifications,
          desktopNotifications: false, // Platform-specific, not stored in backend
          pushNotifications: false, // Platform-specific, not stored in backend
        },
        appearance: {
          successAnimations: preferences.animationsEnabled,
          successSounds: preferences.soundEnabled,
          theme: mapThemeFromBackend(preferences.theme),
        },
        upload: {
          concurrentLimit: preferences.concurrentUploads,
          autoRetryFailed: preferences.autoRetryFailed,
        },
      };

      setSettings(mappedSettings);
    } catch (err) {
      console.error('Failed to load preferences:', err);
      setError('Failed to load settings. Using defaults.');

      // Set default settings on error
      setSettings({
        notifications: {
          uploadComplete: true,
          desktopNotifications: false,
          pushNotifications: false,
        },
        appearance: {
          successAnimations: true,
          successSounds: true,
          theme: 'light',
        },
        upload: {
          concurrentLimit: 3,
          autoRetryFailed: true,
        },
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSettings = async (updatedSettings: UserSettings) => {
    try {
      setSaving(true);
      setError(null);

      // Map SettingsPanel format to backend request
      const updateRequest = {
        animationsEnabled: updatedSettings.appearance.successAnimations,
        soundEnabled: updatedSettings.appearance.successSounds,
        theme: mapThemeToBackend(updatedSettings.appearance.theme),
        concurrentUploads: updatedSettings.upload.concurrentLimit,
        uploadCompleteNotifications: updatedSettings.notifications.uploadComplete,
        autoRetryFailed: updatedSettings.upload.autoRetryFailed,
      };

      await userService.updatePreferences(updateRequest);

      // Update local state
      setSettings(updatedSettings);

      // Show success message
      Alert.alert('Success', 'Settings saved successfully!');
    } catch (err) {
      console.error('Failed to save preferences:', err);
      Alert.alert('Error', 'Failed to save settings. Please try again.');
      setError('Failed to save settings.');
    } finally {
      setSaving(false);
    }
  };

  const mapThemeFromBackend = (theme: string): 'light' | 'dark' | 'auto' => {
    switch (theme.toLowerCase()) {
      case 'light':
        return 'light';
      case 'dark':
        return 'dark';
      case 'system':
        return 'auto';
      default:
        return 'light';
    }
  };

  const mapThemeToBackend = (theme: 'light' | 'dark' | 'auto'): string => {
    switch (theme) {
      case 'light':
        return 'LIGHT';
      case 'dark':
        return 'DARK';
      case 'auto':
        return 'SYSTEM';
      default:
        return 'LIGHT';
    }
  };

  if (loading) {
    return (
      <View style={[styles.loadingContainer, { backgroundColor: theme.colors.background }]}>
        <ActivityIndicator size="large" color={theme.colors.primary[500]} />
        <Text style={{ marginTop: theme.spacing[4] }}>Loading settings...</Text>
      </View>
    );
  }

  if (!settings) {
    return (
      <View style={[styles.loadingContainer, { backgroundColor: theme.colors.background }]}>
        <Text variant="body" color={theme.colors.error[500]}>
          Failed to load settings
        </Text>
      </View>
    );
  }

  return (
    <SettingsPanel
      visible={true}
      onClose={() => navigation.goBack()}
      settings={settings}
      onSaveSettings={handleSaveSettings}
      testID="settings-screen"
    />
  );
};

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});

export default SettingsScreen;
