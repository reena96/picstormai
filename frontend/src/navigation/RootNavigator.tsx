/**
 * Root Navigator
 * Handles conditional rendering of Auth Stack vs Main Stack based on authentication state
 */

import React from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuth } from '../hooks/useAuth';
import { useTheme } from '../hooks/useTheme';
import LoginScreen from '../screens/LoginScreen';
import HomeScreen from '../screens/HomeScreen';
import SettingsScreen from '../screens/SettingsScreen';
import OnboardingScreen from '../screens/OnboardingScreen';
import UploadScreen from '../screens/UploadScreen';

const Stack = createNativeStackNavigator();

export const RootNavigator: React.FC = () => {
  const { isAuthenticated, isLoading, user, markOnboardingComplete } = useAuth();
  const { theme } = useTheme();

  // Show loading screen while checking auth status
  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={theme.colors.primary[500]} />
      </View>
    );
  }

  // Determine if we should show onboarding
  const shouldShowOnboarding = isAuthenticated && user && !user.hasSeenOnboarding;

  const handleOnboardingComplete = async () => {
    await markOnboardingComplete();
    // Navigation will automatically update when user state changes
  };

  const handleOnboardingSkip = async () => {
    await markOnboardingComplete();
    // Navigation will automatically update when user state changes
  };

  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
          animation: 'slide_from_right',
        }}
      >
        {!isAuthenticated ? (
          <Stack.Group>
            <Stack.Screen name="Login" component={LoginScreen} />
          </Stack.Group>
        ) : shouldShowOnboarding ? (
          <Stack.Group>
            <Stack.Screen name="Onboarding">
              {() => (
                <OnboardingScreen
                  onComplete={handleOnboardingComplete}
                  onSkip={handleOnboardingSkip}
                />
              )}
            </Stack.Screen>
          </Stack.Group>
        ) : (
          <Stack.Group>
            <Stack.Screen name="Home" component={HomeScreen} />
            <Stack.Screen name="Upload" component={UploadScreen} />
            <Stack.Screen name="Settings" component={SettingsScreen} />
          </Stack.Group>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
};

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});

export default RootNavigator;
