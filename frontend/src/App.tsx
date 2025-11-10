/**
 * Demo App Component
 * Showcases the design system components
 * This is a placeholder until full app implementation in later stories
 */

import React, { useState } from 'react';
import { View, SafeAreaView, StyleSheet } from 'react-native';
import { useTheme } from './hooks/useTheme';
import { Text } from './components/atoms/Text';
import { Button } from './components/atoms/Button';
import { Badge } from './components/atoms/Badge';
import { PhotoGrid } from './components/organisms/PhotoGrid';
import { SettingsPanel, UserSettings } from './components/organisms/SettingsPanel';
import { EmptyState } from './components/molecules/EmptyState';
import { Settings, Image } from 'lucide-react-native';

const App: React.FC = () => {
  const { theme, toggleTheme, isDark } = useTheme();
  const [settingsVisible, setSettingsVisible] = useState(false);
  const [settings, setSettings] = useState<UserSettings>({
    notifications: {
      uploadComplete: true,
      desktopNotifications: false,
      pushNotifications: true,
    },
    appearance: {
      successAnimations: true,
      successSounds: true,
      theme: 'auto',
    },
    upload: {
      concurrentLimit: 5,
      autoRetryFailed: true,
    },
  });

  const containerStyle = {
    flex: 1,
    backgroundColor: theme.colors.background,
  };

  const headerStyle = {
    padding: theme.spacing[4],
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
    flexDirection: 'row' as const,
    justifyContent: 'space-between' as const,
    alignItems: 'center' as const,
  };

  const contentStyle = {
    flex: 1,
    padding: theme.spacing[4],
  };

  return (
    <SafeAreaView style={containerStyle}>
      {/* Header */}
      <View style={headerStyle}>
        <View>
          <Text variant="h3">PicStormAI Design System</Text>
          <View style={{ flexDirection: 'row', marginTop: theme.spacing[1], gap: theme.spacing[2] }}>
            <Badge variant={isDark ? 'info' : 'neutral'}>
              {isDark ? 'Dark Mode' : 'Light Mode'}
            </Badge>
            <Badge variant="success">Story 0.5</Badge>
          </View>
        </View>
        <View style={{ flexDirection: 'row', gap: theme.spacing[2] }}>
          <Button variant="secondary" size="medium" onPress={toggleTheme}>
            Toggle Theme
          </Button>
          <Button
            variant="primary"
            size="medium"
            onPress={() => setSettingsVisible(true)}
            accessibilityLabel="Open settings"
          >
            <Settings size={20} color={theme.colors.white} />
          </Button>
        </View>
      </View>

      {/* Content */}
      <View style={contentStyle}>
        <EmptyState
          icon={Image}
          headline="Design System Ready"
          subtext="All components have been implemented successfully. This is a demo app showing the design system in action. Full photo upload features will be implemented in later stories."
          ctaLabel="View Documentation"
          onCtaPress={() => console.log('Documentation coming in Storybook')}
        />

        {/* Photo Grid with empty state */}
        <PhotoGrid
          photos={[]}
          onPhotoPress={() => {}}
          emptyMessage="Upload your first photos to get started"
        />
      </View>

      {/* Settings Panel */}
      <SettingsPanel
        visible={settingsVisible}
        onClose={() => setSettingsVisible(false)}
        settings={settings}
        onSaveSettings={(newSettings) => {
          setSettings(newSettings);
          console.log('Settings saved:', newSettings);
        }}
      />
    </SafeAreaView>
  );
};

export default App;
