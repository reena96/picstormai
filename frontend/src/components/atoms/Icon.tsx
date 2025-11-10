/**
 * Icon Component (Atom)
 * Wrapper for Lucide React Native icons with theme support
 * Sizes: xs (12), sm (16), base (20), md (24), lg (32), xl (40)
 * WCAG compliant with proper accessibility attributes
 */

import React from 'react';
import { View, ViewStyle } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { LucideIcon } from 'lucide-react-native';

export interface IconProps {
  icon: LucideIcon;
  size?: 'xs' | 'sm' | 'base' | 'md' | 'lg' | 'xl';
  color?: string;
  decorative?: boolean; // If true, uses aria-hidden; if false, requires accessibilityLabel
  accessibilityLabel?: string;
  testID?: string;
  style?: ViewStyle;
}

export const Icon: React.FC<IconProps> = ({
  icon: IconComponent,
  size = 'base',
  color,
  decorative = true,
  accessibilityLabel,
  testID,
  style,
}) => {
  const { theme } = useTheme();

  const sizeMap: Record<string, number> = {
    xs: 12,
    sm: 16,
    base: 20,
    md: 24,
    lg: 32,
    xl: 40,
  };

  const iconSize = sizeMap[size];
  const iconColor = color || theme.colors.text.primary;

  return (
    <View
      style={style}
      accessibilityRole={decorative ? undefined : 'image'}
      accessibilityLabel={decorative ? undefined : accessibilityLabel}
      aria-hidden={decorative}
      testID={testID}
    >
      <IconComponent size={iconSize} color={iconColor} />
    </View>
  );
};
