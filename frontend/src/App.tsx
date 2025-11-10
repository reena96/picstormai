/**
 * App Component
 * Main app entry point with AuthProvider and Navigation
 * Story 1.3: Login/Logout UI implementation
 */

import React from 'react';
import { ThemeProvider } from './contexts/ThemeContext';
import { AuthProvider } from './contexts/AuthContext';
import { RootNavigator } from './navigation/RootNavigator';
import { SafeAreaProvider } from 'react-native-safe-area-context';

const App: React.FC = () => {
  return (
    <SafeAreaProvider>
      <ThemeProvider>
        <AuthProvider>
          <RootNavigator />
        </AuthProvider>
      </ThemeProvider>
    </SafeAreaProvider>
  );
};

// Legacy demo component (commented out for reference)
/*
const DemoApp: React.FC = () => {
  // Legacy demo code - preserved for reference
  // Uncomment and use DemoApp instead of App to view the design system showcase
};
*/

export default App;
