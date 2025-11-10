/**
 * EmptyState Component (Molecule)
 * Centered layout with illustration, headline, subtext, and CTA
 * Responsive: scales down on mobile
 */

import React from 'react';
import { View, ViewStyle, TextStyle } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { Text } from '../atoms/Text';
import { Button } from '../atoms/Button';
import { Icon } from '../atoms/Icon';
import { LucideIcon } from 'lucide-react-native';

export interface EmptyStateProps {
  icon: LucideIcon;
  headline: string;
  subtext: string;
  ctaLabel?: string;
  onCtaPress?: () => void;
  testID?: string;
  style?: ViewStyle;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  headline,
  subtext,
  ctaLabel,
  onCtaPress,
  testID,
  style,
}) => {
  const { theme } = useTheme();

  const containerStyle: ViewStyle = {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: theme.spacing[8],
    minHeight: 400,
  };

  const iconContainerStyle: ViewStyle = {
    marginBottom: theme.spacing[6],
    opacity: 0.6,
  };

  const headlineStyle: TextStyle = {
    marginBottom: theme.spacing[2],
    textAlign: 'center',
  };

  const subtextStyle: TextStyle = {
    marginBottom: theme.spacing[6],
    textAlign: 'center',
    maxWidth: 400,
  };

  return (
    <View style={[containerStyle, style]} testID={testID}>
      <View style={iconContainerStyle}>
        <Icon
          icon={icon}
          size="xl"
          color={theme.colors.text.secondary}
          decorative
        />
      </View>

      <Text variant="h3" weight="semibold" style={headlineStyle}>
        {headline}
      </Text>

      <Text variant="body" color={theme.colors.text.secondary} style={subtextStyle}>
        {subtext}
      </Text>

      {ctaLabel && onCtaPress && (
        <Button variant="primary" size="large" onPress={onCtaPress}>
          {ctaLabel}
        </Button>
      )}
    </View>
  );
};
