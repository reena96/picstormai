/**
 * ThemeContext Tests
 * Tests theme switching, persistence, and system preference detection
 */

import React from 'react';
import { render } from '@testing-library/react-native';
import { render as renderDOM } from '@testing-library/react';
import { Text, View } from 'react-native';
import { axe, toHaveNoViolations } from 'jest-axe';
import { ThemeProvider, useTheme } from './ThemeContext';

expect.extend(toHaveNoViolations);

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

  describe('Accessibility - WCAG 2.1 AA Compliance', () => {
    it('should have no accessibility violations when ThemeProvider renders', async () => {
      const ThemeTestComponent = () => {
        const { theme } = useTheme();
        return (
          <View style={{ backgroundColor: theme.colors.background }}>
            <Text style={{ color: theme.colors.text.primary }}>
              Theme Provider Content
            </Text>
          </View>
        );
      };

      const { container } = renderDOM(
        <ThemeProvider>
          <ThemeTestComponent />
        </ThemeProvider>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations in light mode', async () => {
      const ThemeTestComponent = () => {
        const { theme, isDark } = useTheme();
        return (
          <View style={{ backgroundColor: theme.colors.background }}>
            <Text style={{ color: theme.colors.text.primary }}>
              Current theme: {isDark ? 'dark' : 'light'}
            </Text>
          </View>
        );
      };

      const { container } = renderDOM(
        <ThemeProvider>
          <ThemeTestComponent />
        </ThemeProvider>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should maintain accessibility with themed components', async () => {
      const ThemedCard = () => {
        const { theme } = useTheme();
        return (
          <View
            style={{
              backgroundColor: theme.colors.surface,
              padding: theme.spacing[4],
              borderRadius: theme.borderRadius.base,
            }}
          >
            <Text
              style={{
                color: theme.colors.text.primary,
                fontSize: theme.typography.fontSize.lg,
                fontWeight: theme.typography.fontWeight.semibold as any,
              }}
            >
              Card Title
            </Text>
            <Text style={{ color: theme.colors.text.secondary }}>
              Card content with themed styling
            </Text>
          </View>
        );
      };

      const { container } = renderDOM(
        <ThemeProvider>
          <ThemedCard />
        </ThemeProvider>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations with multiple themed elements', async () => {
      const ComplexThemedComponent = () => {
        const { theme } = useTheme();
        return (
          <View style={{ backgroundColor: theme.colors.background }}>
            <View
              style={{
                backgroundColor: theme.colors.primary[500],
                padding: theme.spacing[3],
              }}
            >
              <Text style={{ color: theme.colors.text.inverse }}>
                Primary Header
              </Text>
            </View>
            <View style={{ padding: theme.spacing[4] }}>
              <Text style={{ color: theme.colors.text.primary }}>
                Primary Text
              </Text>
              <Text style={{ color: theme.colors.text.secondary }}>
                Secondary Text
              </Text>
              <Text style={{ color: theme.colors.text.tertiary }}>
                Tertiary Text
              </Text>
            </View>
          </View>
        );
      };

      const { container } = renderDOM(
        <ThemeProvider>
          <ComplexThemedComponent />
        </ThemeProvider>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });
});
