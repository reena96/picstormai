/**
 * Button Component Tests
 * Tests all variants, states, and accessibility features
 */

import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { render as renderDOM } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { Button } from './Button';
import { ThemeProvider } from '../../contexts/ThemeContext';

expect.extend(toHaveNoViolations);

const renderWithTheme = (component: React.ReactElement) => {
  return render(<ThemeProvider>{component}</ThemeProvider>);
};

const renderWithThemeDOM = (component: React.ReactElement) => {
  return renderDOM(<ThemeProvider>{component}</ThemeProvider>);
};

describe('Button', () => {
  describe('Rendering', () => {
    it('renders with default props', () => {
      const { getByText } = renderWithTheme(
        <Button onPress={() => {}}>Click Me</Button>
      );
      expect(getByText('Click Me')).toBeTruthy();
    });

    it('renders primary variant', () => {
      const { getByRole } = renderWithTheme(
        <Button variant="primary" onPress={() => {}}>Primary</Button>
      );
      expect(getByRole('button')).toBeTruthy();
    });

    it('renders secondary variant', () => {
      const { getByRole } = renderWithTheme(
        <Button variant="secondary" onPress={() => {}}>Secondary</Button>
      );
      expect(getByRole('button')).toBeTruthy();
    });

    it('renders text variant', () => {
      const { getByRole } = renderWithTheme(
        <Button variant="text" onPress={() => {}}>Text</Button>
      );
      expect(getByRole('button')).toBeTruthy();
    });

    it('renders FAB variant', () => {
      const { getByRole } = renderWithTheme(
        <Button variant="fab" onPress={() => {}}>+</Button>
      );
      expect(getByRole('button')).toBeTruthy();
    });
  });

  describe('Sizes', () => {
    it('renders small size', () => {
      const { getByRole } = renderWithTheme(
        <Button size="small" onPress={() => {}}>Small</Button>
      );
      expect(getByRole('button')).toBeTruthy();
    });

    it('renders medium size (default)', () => {
      const { getByRole } = renderWithTheme(
        <Button size="medium" onPress={() => {}}>Medium</Button>
      );
      expect(getByRole('button')).toBeTruthy();
    });

    it('renders large size', () => {
      const { getByRole } = renderWithTheme(
        <Button size="large" onPress={() => {}}>Large</Button>
      );
      expect(getByRole('button')).toBeTruthy();
    });
  });

  describe('States', () => {
    it('handles press events', () => {
      const onPress = jest.fn();
      const { getByRole } = renderWithTheme(
        <Button onPress={onPress}>Press Me</Button>
      );

      fireEvent.press(getByRole('button'));
      expect(onPress).toHaveBeenCalledTimes(1);
    });

    it('does not call onPress when disabled', () => {
      const onPress = jest.fn();
      const { getByRole } = renderWithTheme(
        <Button disabled onPress={onPress}>Disabled</Button>
      );

      fireEvent.press(getByRole('button'));
      expect(onPress).not.toHaveBeenCalled();
    });

    it('shows loading indicator when loading', () => {
      const { queryByText } = renderWithTheme(
        <Button loading onPress={() => {}}>Loading</Button>
      );

      // Text should not be visible when loading
      expect(queryByText('Loading')).toBeNull();
    });

    it('does not call onPress when loading', () => {
      const onPress = jest.fn();
      const { getByRole } = renderWithTheme(
        <Button loading onPress={onPress}>Loading</Button>
      );

      fireEvent.press(getByRole('button'));
      expect(onPress).not.toHaveBeenCalled();
    });
  });

  describe('Accessibility', () => {
    it('has correct accessibility role', () => {
      const { getByRole } = renderWithTheme(
        <Button onPress={() => {}}>Accessible</Button>
      );
      expect(getByRole('button')).toBeTruthy();
    });

    it('accepts custom accessibility label', () => {
      const { getByLabelText } = renderWithTheme(
        <Button onPress={() => {}} accessibilityLabel="Custom Label">Button</Button>
      );
      expect(getByLabelText('Custom Label')).toBeTruthy();
    });

    it('has disabled state in accessibility', () => {
      const { getByRole } = renderWithTheme(
        <Button disabled onPress={() => {}}>Disabled</Button>
      );
      const button = getByRole('button');
      expect(button.props.accessibilityState.disabled).toBe(true);
    });

    it('accepts testID prop', () => {
      const { getByTestId } = renderWithTheme(
        <Button testID="test-button" onPress={() => {}}>Test</Button>
      );
      expect(getByTestId('test-button')).toBeTruthy();
    });
  });

  describe('Accessibility - WCAG 2.1 AA Compliance', () => {
    it('should have no accessibility violations for primary button', async () => {
      const { container } = renderWithThemeDOM(
        <Button variant="primary" onPress={() => {}}>Primary Button</Button>
      );
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for secondary button', async () => {
      const { container } = renderWithThemeDOM(
        <Button variant="secondary" onPress={() => {}}>Secondary Button</Button>
      );
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for text button', async () => {
      const { container } = renderWithThemeDOM(
        <Button variant="text" onPress={() => {}}>Text Button</Button>
      );
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for FAB button', async () => {
      const { container } = renderWithThemeDOM(
        <Button variant="fab" onPress={() => {}}>+</Button>
      );
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for disabled button', async () => {
      const { container } = renderWithThemeDOM(
        <Button disabled onPress={() => {}}>Disabled Button</Button>
      );
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for loading button', async () => {
      const { container } = renderWithThemeDOM(
        <Button loading onPress={() => {}}>Loading Button</Button>
      );
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for button with custom accessibility label', async () => {
      const { container } = renderWithThemeDOM(
        <Button
          onPress={() => {}}
          accessibilityLabel="Submit form"
        >
          Submit
        </Button>
      );
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for small button', async () => {
      const { container } = renderWithThemeDOM(
        <Button size="small" onPress={() => {}}>Small Button</Button>
      );
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for large button', async () => {
      const { container } = renderWithThemeDOM(
        <Button size="large" onPress={() => {}}>Large Button</Button>
      );
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });
});
