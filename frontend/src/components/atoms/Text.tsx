/**
 * Text Component (Atom)
 * Variants: h1, h2, h3, h4, body, bodySmall, caption, label
 * Theme-aware with semantic accessibility support
 */

import React from 'react';
import { Text as RNText, TextStyle, TextProps as RNTextProps } from 'react-native';
import { useTheme } from '../../hooks/useTheme';

export interface TextProps extends RNTextProps {
  variant?: 'h1' | 'h2' | 'h3' | 'h4' | 'body' | 'bodySmall' | 'caption' | 'label';
  weight?: 'thin' | 'light' | 'normal' | 'medium' | 'semibold' | 'bold' | 'extrabold';
  color?: string;
  align?: 'left' | 'center' | 'right' | 'justify';
  children: React.ReactNode;
  style?: TextStyle;
  testID?: string;
}

export const Text: React.FC<TextProps> = ({
  variant = 'body',
  weight,
  color,
  align = 'left',
  children,
  style,
  testID,
  ...rest
}) => {
  const { theme } = useTheme();

  const getVariantStyle = (): TextStyle => {
    const variantStyles: Record<string, TextStyle> = {
      h1: {
        fontSize: theme.typography.fontSize['4xl'],
        fontWeight: theme.typography.fontWeight.bold as TextStyle['fontWeight'],
        lineHeight: theme.typography.fontSize['4xl'] * theme.typography.lineHeight.tight,
        color: theme.colors.text.primary,
      },
      h2: {
        fontSize: theme.typography.fontSize['3xl'],
        fontWeight: theme.typography.fontWeight.bold as TextStyle['fontWeight'],
        lineHeight: theme.typography.fontSize['3xl'] * theme.typography.lineHeight.tight,
        color: theme.colors.text.primary,
      },
      h3: {
        fontSize: theme.typography.fontSize['2xl'],
        fontWeight: theme.typography.fontWeight.semibold as TextStyle['fontWeight'],
        lineHeight: theme.typography.fontSize['2xl'] * theme.typography.lineHeight.tight,
        color: theme.colors.text.primary,
      },
      h4: {
        fontSize: theme.typography.fontSize.xl,
        fontWeight: theme.typography.fontWeight.semibold as TextStyle['fontWeight'],
        lineHeight: theme.typography.fontSize.xl * theme.typography.lineHeight.normal,
        color: theme.colors.text.primary,
      },
      body: {
        fontSize: theme.typography.fontSize.base,
        fontWeight: theme.typography.fontWeight.normal as TextStyle['fontWeight'],
        lineHeight: theme.typography.fontSize.base * theme.typography.lineHeight.normal,
        color: theme.colors.text.primary,
      },
      bodySmall: {
        fontSize: theme.typography.fontSize.sm,
        fontWeight: theme.typography.fontWeight.normal as TextStyle['fontWeight'],
        lineHeight: theme.typography.fontSize.sm * theme.typography.lineHeight.normal,
        color: theme.colors.text.secondary,
      },
      caption: {
        fontSize: theme.typography.fontSize.xs,
        fontWeight: theme.typography.fontWeight.normal as TextStyle['fontWeight'],
        lineHeight: theme.typography.fontSize.xs * theme.typography.lineHeight.normal,
        color: theme.colors.text.secondary,
      },
      label: {
        fontSize: theme.typography.fontSize.sm,
        fontWeight: theme.typography.fontWeight.medium as TextStyle['fontWeight'],
        lineHeight: theme.typography.fontSize.sm * theme.typography.lineHeight.normal,
        color: theme.colors.text.primary,
      },
    };

    return variantStyles[variant];
  };

  const getAccessibilityRole = () => {
    if (variant.startsWith('h')) {
      return 'header';
    }
    return 'text';
  };

  const getAccessibilityLevel = () => {
    if (variant === 'h1') return 1;
    if (variant === 'h2') return 2;
    if (variant === 'h3') return 3;
    if (variant === 'h4') return 4;
    return undefined;
  };

  const textStyle: TextStyle = {
    ...getVariantStyle(),
    fontFamily: theme.typography.fontFamily.primary,
    textAlign: align,
    ...(weight && { fontWeight: theme.typography.fontWeight[weight] as TextStyle['fontWeight'] }),
    ...(color && { color }),
  };

  return (
    <RNText
      style={[textStyle, style]}
      accessibilityRole={getAccessibilityRole()}
      aria-level={getAccessibilityLevel()}
      testID={testID}
      {...rest}
    >
      {children}
    </RNText>
  );
};
