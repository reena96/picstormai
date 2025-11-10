/**
 * Badge Component (Atom)
 * Variants: success, error, warning, info, neutral
 * Small pill-shaped status indicators with optional icons
 */

import React from 'react';
import { View, Text, StyleSheet, ViewStyle, TextStyle } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { LucideIcon } from 'lucide-react-native';
import { Icon } from './Icon';

export interface BadgeProps {
  variant?: 'success' | 'error' | 'warning' | 'info' | 'neutral';
  icon?: LucideIcon;
  children: React.ReactNode;
  testID?: string;
  style?: ViewStyle;
}

export const Badge: React.FC<BadgeProps> = ({
  variant = 'neutral',
  icon,
  children,
  testID,
  style,
}) => {
  const { theme } = useTheme();

  const getVariantStyle = (): { container: ViewStyle; text: TextStyle } => {
    const variants = {
      success: {
        container: {
          backgroundColor: theme.colors.success[100],
          borderColor: theme.colors.success[500],
        },
        text: {
          color: theme.colors.success[700],
        },
      },
      error: {
        container: {
          backgroundColor: theme.colors.error[100],
          borderColor: theme.colors.error[500],
        },
        text: {
          color: theme.colors.error[700],
        },
      },
      warning: {
        container: {
          backgroundColor: theme.colors.warning[100],
          borderColor: theme.colors.warning[500],
        },
        text: {
          color: theme.colors.warning[700],
        },
      },
      info: {
        container: {
          backgroundColor: theme.colors.info[100],
          borderColor: theme.colors.info[500],
        },
        text: {
          color: theme.colors.info[600],
        },
      },
      neutral: {
        container: {
          backgroundColor: theme.colors.gray[100],
          borderColor: theme.colors.gray[400],
        },
        text: {
          color: theme.colors.gray[700],
        },
      },
    };

    return variants[variant];
  };

  const variantStyles = getVariantStyle();

  const containerStyle: ViewStyle = {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: theme.spacing[2],
    paddingVertical: theme.spacing[1],
    borderRadius: theme.borderRadius.full,
    borderWidth: 1,
    alignSelf: 'flex-start',
    ...variantStyles.container,
  };

  const textStyle: TextStyle = {
    fontSize: theme.typography.fontSize.xs,
    fontWeight: theme.typography.fontWeight.semibold as TextStyle['fontWeight'],
    fontFamily: theme.typography.fontFamily.primary,
    ...variantStyles.text,
  };

  return (
    <View style={[containerStyle, style]} testID={testID} accessibilityRole="text">
      {icon && (
        <View style={{ marginRight: theme.spacing[1] }}>
          <Icon icon={icon} size="xs" color={variantStyles.text.color as string} decorative />
        </View>
      )}
      <Text style={textStyle}>{children}</Text>
    </View>
  );
};
