/**
 * Home Screen (Placeholder)
 * Main screen shown after successful login
 */

import React from 'react';
import { View, Text, StyleSheet, ViewStyle, TextStyle } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useTheme } from '../hooks/useTheme';
import { useAuth } from '../hooks/useAuth';
import { Button } from '../components/atoms/Button';
import { Settings } from 'lucide-react-native';

export const HomeScreen: React.FC = () => {
  const { theme } = useTheme();
  const { logout } = useAuth();
  const navigation = useNavigation();

  const styles = StyleSheet.create<{
    container: ViewStyle;
    content: ViewStyle;
    title: TextStyle;
    subtitle: TextStyle;
    buttonContainer: ViewStyle;
  }>({
    container: {
      flex: 1,
      backgroundColor: theme.colors.background,
    },
    content: {
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
      paddingHorizontal: theme.spacing[4],
    },
    title: {
      fontSize: theme.typography.fontSize['2xl'],
      fontWeight: theme.typography.fontWeight.bold as TextStyle['fontWeight'],
      color: theme.colors.text.primary,
      fontFamily: theme.typography.fontFamily.primary,
      marginBottom: theme.spacing[2],
    },
    subtitle: {
      fontSize: theme.typography.fontSize.base,
      color: theme.colors.text.secondary,
      fontFamily: theme.typography.fontFamily.primary,
      marginBottom: theme.spacing[8],
      textAlign: 'center',
    },
    buttonContainer: {
      width: '100%',
      maxWidth: 300,
      gap: theme.spacing[3],
    },
  });

  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>Welcome to RapidPhotoUpload!</Text>
        <Text style={styles.subtitle}>
          You're logged in. This is the home screen placeholder.
        </Text>

        <View style={styles.buttonContainer}>
          <Button
            variant="primary"
            onPress={() => navigation.navigate('Settings' as never)}
            testID="settings-button"
          >
            <Settings size={20} color={theme.colors.white} style={{ marginRight: theme.spacing[2] }} />
            Settings
          </Button>

          <Button
            variant="secondary"
            onPress={logout}
            testID="logout-button"
          >
            Log Out
          </Button>
        </View>
      </View>
    </View>
  );
};

export default HomeScreen;
