/**
 * App Component
 * Main app entry point with AuthProvider and Navigation
 * Story 1.3: Login/Logout UI implementation
 */

import React, { useEffect } from 'react';
import { ThemeProvider } from './contexts/ThemeContext';
import { AuthProvider } from './contexts/AuthContext';
import { RootNavigator } from './navigation/RootNavigator';

const App: React.FC = () => {
  useEffect(() => {
    console.log('=== App Component Mounted ===');
    return () => {
      console.log('=== App Component Unmounted ===');
    };
  }, []);

  console.log('App: Rendering');

  return (
    <ThemeProvider>
      <AuthProvider>
        <RootNavigator />
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;
