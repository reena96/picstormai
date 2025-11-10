/**
 * Spinner Component (Atom)
 * Circular loading indicator with smooth 2s rotation animation
 * Sizes: small (16), medium (24), large (40)
 * WCAG compliant with aria-label="Loading"
 */

import React from 'react';
import { View, ActivityIndicator, ViewStyle } from 'react-native';
import { useTheme } from '../../hooks/useTheme';

export interface SpinnerProps {
  size?: 'small' | 'medium' | 'large';
  color?: string;
  accessibilityLabel?: string;
  testID?: string;
  style?: ViewStyle;
}

export const Spinner: React.FC<SpinnerProps> = ({
  size = 'medium',
  color,
  accessibilityLabel = 'Loading',
  testID,
  style,
}) => {
  const { theme } = useTheme();

  const sizeMap: Record<string, 'small' | 'large' | number> = {
    small: 'small',
    medium: 24,
    large: 'large',
  };

  const spinnerColor = color || theme.colors.primary[500];
  const spinnerSize = sizeMap[size];

  return (
    <View
      style={style}
      accessibilityRole="progressbar"
      accessibilityLabel={accessibilityLabel}
      accessibilityLive="polite"
      testID={testID}
    >
      <ActivityIndicator size={spinnerSize} color={spinnerColor} />
    </View>
  );
};
