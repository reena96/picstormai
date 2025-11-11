/**
 * App Component
 * Main app entry point with AuthProvider and Navigation
 * Story 1.3: Login/Logout UI implementation
 */

import React from 'react';
import { ThemeProvider } from './contexts/ThemeContext';
import { AuthProvider } from './contexts/AuthContext';
import { useAuth } from './hooks/useAuth';
import LoginScreen from './screens/LoginScreen';
import HomeScreen from './screens/HomeScreen';
import { View, ActivityIndicator } from 'react-native';
import { useTheme } from './hooks/useTheme';

const AppContent: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();
  const { theme } = useTheme();

  if (isLoading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: theme.colors.background }}>
        <ActivityIndicator size="large" color={theme.colors.primary[500]} />
      </View>
    );
  }

  // Simple conditional rendering without navigation for now
  if (!isAuthenticated) {
    return <LoginScreen />;
  }

  return <HomeScreen />;
};

const App: React.FC = () => {
  return (
    <ThemeProvider>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;
