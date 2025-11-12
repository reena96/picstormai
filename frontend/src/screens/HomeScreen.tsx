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

export const HomeScreen: React.FC = () => {
  console.log('HomeScreen: Rendering');
  const navigation = useNavigation();
  const { theme } = useTheme();
  const { logout, user } = useAuth();
  console.log('HomeScreen: User:', user?.email);

  const styles = StyleSheet.create<{
    container: ViewStyle;
    content: ViewStyle;
    title: TextStyle;
    subtitle: TextStyle;
    buttonContainer: ViewStyle;
    button: ViewStyle;
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
    },
    button: {
      marginBottom: theme.spacing[3],
    },
  });

  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>Welcome to PicStormAI!</Text>
        <Text style={styles.subtitle}>
          Successfully logged in as {user?.displayName || 'User'}
        </Text>
        <Text style={styles.subtitle}>
          Epic 1 (Authentication & Onboarding) Complete! 🎉
        </Text>

        <View style={styles.buttonContainer}>
          <Button
            variant="primary"
            onPress={() => navigation.navigate('Upload' as never)}
            testID="upload-button"
            style={styles.button}
          >
            Upload Photos
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
