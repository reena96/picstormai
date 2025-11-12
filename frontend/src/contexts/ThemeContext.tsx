/**
 * Theme Context Provider
 * Provides light/dark theme support with system preference detection and persistence
 */

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { Platform } from 'react-native';
import { Theme, ThemeMode, ThemeContextValue } from '../types/theme';
import { colors, typography, spacing, shadows, borderRadius, animations } from '../styles/tokens';

// Light Theme
const lightTheme: Theme = {
  colors: {
    ...colors,
    background: colors.white,
    surface: colors.gray[50],
    text: {
      primary: colors.gray[900],
      secondary: colors.gray[600],
      disabled: colors.gray[400],
      inverse: colors.white,
    },
    border: colors.gray[300],
  },
  typography,
  spacing,
  shadows,
  borderRadius,
  animations,
};

// Dark Theme
const darkTheme: Theme = {
  colors: {
    ...colors,
    background: colors.gray[900],
    surface: colors.gray[800],
    text: {
      primary: colors.gray[50],
      secondary: colors.gray[400],
      disabled: colors.gray[600],
      inverse: colors.gray[900],
    },
    border: colors.gray[700],
  },
  typography,
  spacing,
  shadows,
  borderRadius,
  animations,
};

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

interface ThemeProviderProps {
  children: ReactNode;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children }) => {
  // FORCE LIGHT MODE FOR NOW
  const [mode, setMode] = useState<ThemeMode>('light');
  const [isDark, setIsDark] = useState(false);

  console.log('ThemeProvider: Rendering, mode:', mode, 'isDark:', isDark);

  // Initialize theme from storage or system preference
  useEffect(() => {
    const initializeTheme = async () => {
      console.log('ThemeProvider: Initializing theme');
      try {
        // For web: use localStorage
        if (Platform.OS === 'web') {
          console.log('ThemeProvider: Platform is web');
          // FORCE LIGHT MODE - ignore saved preferences for now
          console.log('ThemeProvider: FORCING LIGHT MODE');
          setMode('light');
          setIsDark(false);
          localStorage.setItem('themeMode', 'light');

          // Listen for system preference changes
          const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
          const handleChange = (e: MediaQueryListEvent) => {
            const currentMode = localStorage.getItem('themeMode') as ThemeMode;
            if (currentMode === 'auto' || !currentMode) {
              setIsDark(e.matches);
            }
          };
          mediaQuery.addEventListener('change', handleChange);
          return () => mediaQuery.removeEventListener('change', handleChange);
        } else {
          // For mobile: use AsyncStorage (implementation deferred to integration phase)
          // const savedMode = await AsyncStorage.getItem('themeMode');
          // For now, default to auto/light
          setMode('auto');
          setIsDark(false);
        }
      } catch (error) {
        console.error('ThemeProvider: Failed to load theme preference:', error);
      }
      console.log('ThemeProvider: Theme initialization complete');
    };

    initializeTheme();
  }, []); // FIXED: Empty dependency array - only run once on mount

  const toggleTheme = () => {
    const newMode = isDark ? 'light' : 'dark';
    setMode(newMode);
    setIsDark(!isDark);

    if (Platform.OS === 'web') {
      localStorage.setItem('themeMode', newMode);
    }
    // For mobile: AsyncStorage.setItem('themeMode', newMode);
  };

  const setThemeMode = (newMode: ThemeMode) => {
    setMode(newMode);

    if (newMode === 'auto') {
      if (Platform.OS === 'web') {
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        setIsDark(prefersDark);
      }
    } else {
      setIsDark(newMode === 'dark');
    }

    if (Platform.OS === 'web') {
      localStorage.setItem('themeMode', newMode);
    }
    // For mobile: AsyncStorage.setItem('themeMode', newMode);
  };

  const value: ThemeContextValue = {
    theme: isDark ? darkTheme : lightTheme,
    mode,
    isDark,
    toggleTheme,
    setTheme: setThemeMode,
  };

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useTheme = (): ThemeContextValue => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
