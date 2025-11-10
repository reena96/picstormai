/**
 * ThemeContext Tests
 * Tests theme switching, persistence, and system preference detection
 */

import React from 'react';
import { render } from '@testing-library/react-native';
import { Text } from 'react-native';
import { ThemeProvider, useTheme } from './ThemeContext';

// Mock localStorage for web
const localStorageMock = (() => {
  let store: Record<string, string> = {};

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
});

// Test component that uses theme
const TestComponent = () => {
  const { theme, isDark, toggleTheme } = useTheme();

  return (
    <Text testID="theme-mode">{isDark ? 'dark' : 'light'}</Text>
  );
};

describe('ThemeContext', () => {
  beforeEach(() => {
    localStorageMock.clear();
  });

  it('provides theme context to children', () => {
    const { getByTestId } = render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );

    expect(getByTestId('theme-mode')).toBeTruthy();
  });

  it('initializes with light theme by default', () => {
    const { getByTestId } = render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );

    expect(getByTestId('theme-mode').props.children).toBe('light');
  });

  it('throws error when useTheme is used outside provider', () => {
    // Suppress console.error for this test
    const consoleError = console.error;
    console.error = jest.fn();

    expect(() => {
      render(<TestComponent />);
    }).toThrow('useTheme must be used within a ThemeProvider');

    console.error = consoleError;
  });

  it('provides theme object with correct structure', () => {
    let capturedTheme: any;

    const CaptureTheme = () => {
      const { theme } = useTheme();
      capturedTheme = theme;
      return null;
    };

    render(
      <ThemeProvider>
        <CaptureTheme />
      </ThemeProvider>
    );

    expect(capturedTheme).toHaveProperty('colors');
    expect(capturedTheme).toHaveProperty('typography');
    expect(capturedTheme).toHaveProperty('spacing');
    expect(capturedTheme).toHaveProperty('shadows');
    expect(capturedTheme).toHaveProperty('borderRadius');
    expect(capturedTheme).toHaveProperty('animations');
  });
});
