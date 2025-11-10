/**
 * SettingsPanel Component (Organism)
 * Slide-in panel (web) / full-screen (mobile) for user preferences
 * Sections: Notifications, Appearance, Upload Settings
 */

import React, { useState } from 'react';
import { View, ScrollView, Modal, Switch, StyleSheet, ViewStyle, Platform } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { Text } from '../atoms/Text';
import { Button } from '../atoms/Button';
import { X } from 'lucide-react-native';

export interface UserSettings {
  notifications: {
    uploadComplete: boolean;
    desktopNotifications: boolean;
    pushNotifications: boolean;
  };
  appearance: {
    successAnimations: boolean;
    successSounds: boolean;
    theme: 'light' | 'dark' | 'auto';
  };
  upload: {
    concurrentLimit: number;
    autoRetryFailed: boolean;
  };
}

export interface SettingsPanelProps {
  visible: boolean;
  onClose: () => void;
  settings: UserSettings;
  onSaveSettings: (settings: UserSettings) => void;
  testID?: string;
  style?: ViewStyle;
}

export const SettingsPanel: React.FC<SettingsPanelProps> = ({
  visible,
  onClose,
  settings: initialSettings,
  onSaveSettings,
  testID,
  style,
}) => {
  const { theme, setTheme } = useTheme();
  const [settings, setSettings] = useState<UserSettings>(initialSettings);

  const handleToggle = (section: keyof UserSettings, key: string) => {
    setSettings(prev => ({
      ...prev,
      [section]: {
        ...prev[section],
        [key]: !prev[section][key as keyof typeof prev[typeof section]],
      },
    }));
  };

  const handleThemeChange = (themeMode: 'light' | 'dark' | 'auto') => {
    setSettings(prev => ({
      ...prev,
      appearance: { ...prev.appearance, theme: themeMode },
    }));
    setTheme(themeMode);
  };

  const handleSave = () => {
    onSaveSettings(settings);
    onClose();
  };

  const containerStyle: ViewStyle = Platform.OS === 'web' ? {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    width: 400,
    backgroundColor: theme.colors.background,
    ...theme.shadows.xl,
    borderLeftWidth: 1,
    borderLeftColor: theme.colors.border,
  } : {
    flex: 1,
    backgroundColor: theme.colors.background,
  };

  const headerStyle: ViewStyle = {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: theme.spacing[4],
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
  };

  const sectionStyle: ViewStyle = {
    padding: theme.spacing[4],
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
  };

  const settingRowStyle: ViewStyle = {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: theme.spacing[3],
  };

  const themeButtonStyle = (isSelected: boolean): ViewStyle => ({
    flex: 1,
    marginHorizontal: theme.spacing[1],
    backgroundColor: isSelected ? theme.colors.primary[500] : theme.colors.surface,
    borderWidth: 1,
    borderColor: isSelected ? theme.colors.primary[500] : theme.colors.border,
    borderRadius: theme.borderRadius.base,
    padding: theme.spacing[2],
    alignItems: 'center',
  });

  const panelContent = (
    <View style={containerStyle} testID={testID}>
      {/* Header */}
      <View style={headerStyle}>
        <Text variant="h3">Settings</Text>
        <Button variant="text" size="small" onPress={onClose} accessibilityLabel="Close settings">
          <X size={24} color={theme.colors.text.primary} />
        </Button>
      </View>

      {/* Settings sections */}
      <ScrollView>
        {/* Notifications */}
        <View style={sectionStyle}>
          <Text variant="h4" style={{ marginBottom: theme.spacing[3] }}>Notifications</Text>

          <View style={settingRowStyle}>
            <View style={{ flex: 1 }}>
              <Text variant="body">Upload Complete</Text>
              <Text variant="caption" color={theme.colors.text.secondary}>
                Show notification when uploads finish
              </Text>
            </View>
            <Switch
              value={settings.notifications.uploadComplete}
              onValueChange={() => handleToggle('notifications', 'uploadComplete')}
              trackColor={{ false: theme.colors.gray[300], true: theme.colors.primary[300] }}
              thumbColor={settings.notifications.uploadComplete ? theme.colors.primary[500] : theme.colors.gray[100]}
            />
          </View>

          {Platform.OS === 'web' && (
            <View style={settingRowStyle}>
              <View style={{ flex: 1 }}>
                <Text variant="body">Desktop Notifications</Text>
                <Text variant="caption" color={theme.colors.text.secondary}>
                  Enable browser notifications
                </Text>
              </View>
              <Switch
                value={settings.notifications.desktopNotifications}
                onValueChange={() => handleToggle('notifications', 'desktopNotifications')}
                trackColor={{ false: theme.colors.gray[300], true: theme.colors.primary[300] }}
                thumbColor={settings.notifications.desktopNotifications ? theme.colors.primary[500] : theme.colors.gray[100]}
              />
            </View>
          )}

          {Platform.OS !== 'web' && (
            <View style={settingRowStyle}>
              <View style={{ flex: 1 }}>
                <Text variant="body">Push Notifications</Text>
                <Text variant="caption" color={theme.colors.text.secondary}>
                  Receive push notifications
                </Text>
              </View>
              <Switch
                value={settings.notifications.pushNotifications}
                onValueChange={() => handleToggle('notifications', 'pushNotifications')}
                trackColor={{ false: theme.colors.gray[300], true: theme.colors.primary[300] }}
                thumbColor={settings.notifications.pushNotifications ? theme.colors.primary[500] : theme.colors.gray[100]}
              />
            </View>
          )}
        </View>

        {/* Appearance */}
        <View style={sectionStyle}>
          <Text variant="h4" style={{ marginBottom: theme.spacing[3] }}>Appearance</Text>

          <View style={settingRowStyle}>
            <View style={{ flex: 1 }}>
              <Text variant="body">Success Animations</Text>
              <Text variant="caption" color={theme.colors.text.secondary}>
                Show confetti when uploads complete
              </Text>
            </View>
            <Switch
              value={settings.appearance.successAnimations}
              onValueChange={() => handleToggle('appearance', 'successAnimations')}
              trackColor={{ false: theme.colors.gray[300], true: theme.colors.primary[300] }}
              thumbColor={settings.appearance.successAnimations ? theme.colors.primary[500] : theme.colors.gray[100]}
            />
          </View>

          <View style={settingRowStyle}>
            <View style={{ flex: 1 }}>
              <Text variant="body">Success Sounds</Text>
              <Text variant="caption" color={theme.colors.text.secondary}>
                Play sound effects for actions
              </Text>
            </View>
            <Switch
              value={settings.appearance.successSounds}
              onValueChange={() => handleToggle('appearance', 'successSounds')}
              trackColor={{ false: theme.colors.gray[300], true: theme.colors.primary[300] }}
              thumbColor={settings.appearance.successSounds ? theme.colors.primary[500] : theme.colors.gray[100]}
            />
          </View>

          <View style={{ marginTop: theme.spacing[3] }}>
            <Text variant="label" style={{ marginBottom: theme.spacing[2] }}>Theme</Text>
            <View style={{ flexDirection: 'row' }}>
              <Button
                variant="text"
                size="small"
                onPress={() => handleThemeChange('light')}
                style={themeButtonStyle(settings.appearance.theme === 'light')}
              >
                <Text color={settings.appearance.theme === 'light' ? theme.colors.white : theme.colors.text.primary}>
                  Light
                </Text>
              </Button>
              <Button
                variant="text"
                size="small"
                onPress={() => handleThemeChange('dark')}
                style={themeButtonStyle(settings.appearance.theme === 'dark')}
              >
                <Text color={settings.appearance.theme === 'dark' ? theme.colors.white : theme.colors.text.primary}>
                  Dark
                </Text>
              </Button>
              <Button
                variant="text"
                size="small"
                onPress={() => handleThemeChange('auto')}
                style={themeButtonStyle(settings.appearance.theme === 'auto')}
              >
                <Text color={settings.appearance.theme === 'auto' ? theme.colors.white : theme.colors.text.primary}>
                  Auto
                </Text>
              </Button>
            </View>
          </View>
        </View>

        {/* Upload Settings */}
        <View style={sectionStyle}>
          <Text variant="h4" style={{ marginBottom: theme.spacing[3] }}>Upload</Text>

          <View style={settingRowStyle}>
            <View style={{ flex: 1 }}>
              <Text variant="body">Auto-Retry Failed Uploads</Text>
              <Text variant="caption" color={theme.colors.text.secondary}>
                Automatically retry uploads that fail
              </Text>
            </View>
            <Switch
              value={settings.upload.autoRetryFailed}
              onValueChange={() => handleToggle('upload', 'autoRetryFailed')}
              trackColor={{ false: theme.colors.gray[300], true: theme.colors.primary[300] }}
              thumbColor={settings.upload.autoRetryFailed ? theme.colors.primary[500] : theme.colors.gray[100]}
            />
          </View>

          <View style={{ marginTop: theme.spacing[3] }}>
            <Text variant="label">Concurrent Upload Limit: {settings.upload.concurrentLimit}</Text>
            <Text variant="caption" color={theme.colors.text.secondary}>
              Currently simplified - full slider implementation deferred
            </Text>
          </View>
        </View>
      </ScrollView>

      {/* Save button */}
      <View style={{ padding: theme.spacing[4], borderTopWidth: 1, borderTopColor: theme.colors.border }}>
        <Button variant="primary" size="large" onPress={handleSave}>
          Save Changes
        </Button>
      </View>
    </View>
  );

  if (Platform.OS === 'web') {
    return visible ? panelContent : null;
  }

  return (
    <Modal
      visible={visible}
      animationType="slide"
      onRequestClose={onClose}
      presentationStyle="fullScreen"
    >
      {panelContent}
    </Modal>
  );
};
