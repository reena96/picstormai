/**
 * Button Component (Atom)
 * Variants: primary, secondary, text, fab
 * States: default, disabled, loading, pressed
 * WCAG 2.1 AA compliant with 44px minimum touch target
 */

import React from 'react';
import { Pressable, Text, ViewStyle, TextStyle, ActivityIndicator } from 'react-native';
import { useTheme } from '../../hooks/useTheme';

export interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'text' | 'fab';
  size?: 'small' | 'medium' | 'large';
  disabled?: boolean;
  loading?: boolean;
  onPress: () => void;
  children: React.ReactNode;
  accessibilityLabel?: string;
  testID?: string;
  style?: ViewStyle;
}

export const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'medium',
  disabled = false,
  loading = false,
  onPress,
  children,
  accessibilityLabel,
  testID,
  style,
}) => {
  const { theme } = useTheme();

  const getButtonStyle = (pressed: boolean): ViewStyle => {
    const baseStyle: ViewStyle = {
      borderRadius: theme.borderRadius.base,
      alignItems: 'center',
      justifyContent: 'center',
      flexDirection: 'row',
      minHeight: 44, // WCAG minimum touch target
      minWidth: 44,
      opacity: pressed ? 0.8 : 1,
      transform: [{ scale: pressed ? 0.98 : 1 }],
    };

    // Size variations
    const sizeStyles: Record<string, ViewStyle> = {
      small: { paddingHorizontal: theme.spacing[3], paddingVertical: theme.spacing[2], minHeight: 36 },
      medium: { paddingHorizontal: theme.spacing[4], paddingVertical: theme.spacing[3], minHeight: 44 },
      large: { paddingHorizontal: theme.spacing[6], paddingVertical: theme.spacing[4], minHeight: 52 },
    };

    // Variant styles
    let variantStyle: ViewStyle = {};
    if (variant === 'primary') {
      variantStyle = {
        backgroundColor: disabled ? theme.colors.gray[400] : theme.colors.primary[500],
      };
    } else if (variant === 'secondary') {
      variantStyle = {
        backgroundColor: disabled ? theme.colors.gray[100] : theme.colors.surface,
        borderWidth: 1,
        borderColor: disabled ? theme.colors.gray[300] : theme.colors.border,
      };
    } else if (variant === 'text') {
      variantStyle = {
        backgroundColor: 'transparent',
        paddingHorizontal: theme.spacing[2],
      };
    } else if (variant === 'fab') {
      variantStyle = {
        width: 56,
        height: 56,
        borderRadius: theme.borderRadius.full,
        backgroundColor: disabled ? theme.colors.gray[400] : theme.colors.primary[500],
        ...theme.shadows.md,
      };
    }

    return { ...baseStyle, ...sizeStyles[size], ...variantStyle };
  };

  const getTextStyle = (): TextStyle => {
    const baseTextStyle: TextStyle = {
      fontFamily: theme.typography.fontFamily.primary,
      fontWeight: theme.typography.fontWeight.semibold as TextStyle['fontWeight'],
    };

    const sizeTextStyles: Record<string, TextStyle> = {
      small: { fontSize: theme.typography.fontSize.sm },
      medium: { fontSize: theme.typography.fontSize.base },
      large: { fontSize: theme.typography.fontSize.lg },
    };

    let colorStyle: TextStyle = {};
    if (variant === 'primary' || variant === 'fab') {
      colorStyle = { color: theme.colors.text.inverse };
    } else if (variant === 'secondary') {
      colorStyle = { color: disabled ? theme.colors.text.disabled : theme.colors.text.primary };
    } else if (variant === 'text') {
      colorStyle = { color: disabled ? theme.colors.text.disabled : theme.colors.primary[500] };
    }

    return { ...baseTextStyle, ...sizeTextStyles[size], ...colorStyle };
  };

  const childArray = React.Children.toArray(children);
  const isTextChild = (child: React.ReactNode): child is string | number =>
    typeof child === 'string' || typeof child === 'number';

  const normalizedChildren = childArray.map((child, index) => {
    if (isTextChild(child)) {
      return (
        <Text key={`button-text-${index}`} style={getTextStyle()}>
          {child}
        </Text>
      );
    }
    return child;
  });

  const derivedAccessibilityLabel =
    accessibilityLabel ||
    (() => {
      const labelText = childArray
        .filter(isTextChild)
        .map((child) => String(child).trim())
        .filter(Boolean)
        .join(' ');
      return labelText.length > 0 ? labelText : undefined;
    })();

  const renderChildren = () => {
    if (loading) {
      return (
        <ActivityIndicator
          size="small"
          color={variant === 'primary' || variant === 'fab' ? theme.colors.white : theme.colors.primary[500]}
          accessibilityLabel="Loading"
        />
      );
    }

    return normalizedChildren;
  };

  return (
    <Pressable
      onPress={disabled || loading ? undefined : onPress}
      disabled={disabled || loading}
      accessibilityRole="button"
      accessibilityLabel={derivedAccessibilityLabel}
      accessibilityState={{ disabled: disabled || loading, busy: loading }}
      testID={testID}
      style={({ pressed }) => [getButtonStyle(pressed), style]}
      aria-busy={loading}
    >
      {renderChildren()}
    </Pressable>
  );
};
