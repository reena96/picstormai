import React from 'react';
import type { Meta } from '@storybook/react';
import { View } from 'react-native';
import { Text } from '../../src/components/atoms/Text';
import { useTheme } from '../../src/hooks/useTheme';
import { colors, typography, spacing, borderRadius } from '../../src/styles/tokens';

const meta: Meta = {
  title: 'Design System/Tokens',
  tags: ['autodocs'],
};

export default meta;

export const Colors = () => {
  const { theme } = useTheme();

  const ColorSwatch = ({ color, name }: { color: string; name: string }) => (
    <View style={{ marginBottom: 16 }}>
      <View
        style={{
          width: 100,
          height: 100,
          backgroundColor: color,
          borderRadius: 8,
          marginBottom: 8,
          borderWidth: 1,
          borderColor: theme.colors.border,
        }}
      />
      <Text variant="caption">{name}</Text>
      <Text variant="caption" color={theme.colors.text.secondary}>
        {color}
      </Text>
    </View>
  );

  return (
    <View style={{ padding: 20 }}>
      <Text variant="h2" style={{ marginBottom: 24 }}>
        Color Palette
      </Text>

      <Text variant="h4" style={{ marginTop: 32, marginBottom: 16 }}>
        Primary
      </Text>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 16 }}>
        {Object.entries(colors.primary).map(([key, value]) => (
          <ColorSwatch key={key} color={value} name={`primary.${key}`} />
        ))}
      </View>

      <Text variant="h4" style={{ marginTop: 32, marginBottom: 16 }}>
        Semantic Colors
      </Text>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 16 }}>
        <ColorSwatch color={colors.success[500]} name="success.500" />
        <ColorSwatch color={colors.error[500]} name="error.500" />
        <ColorSwatch color={colors.warning[500]} name="warning.500" />
        <ColorSwatch color={colors.info[500]} name="info.500" />
      </View>

      <Text variant="h4" style={{ marginTop: 32, marginBottom: 16 }}>
        Neutral Grays
      </Text>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 16 }}>
        {Object.entries(colors.gray).map(([key, value]) => (
          <ColorSwatch key={key} color={value} name={`gray.${key}`} />
        ))}
      </View>
    </View>
  );
};

export const Typography = () => {
  const { theme } = useTheme();

  return (
    <View style={{ padding: 20 }}>
      <Text variant="h2" style={{ marginBottom: 24 }}>
        Typography Scale
      </Text>

      {(['h1', 'h2', 'h3', 'h4', 'body', 'bodySmall', 'caption', 'label'] as const).map((variant) => (
        <View key={variant} style={{ marginBottom: 24 }}>
          <Text variant={variant}>
            {variant.toUpperCase()} - The quick brown fox jumps over the lazy dog
          </Text>
          <Text variant="caption" color={theme.colors.text.secondary} style={{ marginTop: 4 }}>
            Variant: {variant}
          </Text>
        </View>
      ))}
    </View>
  );
};

export const Spacing = () => {
  const { theme } = useTheme();

  return (
    <View style={{ padding: 20 }}>
      <Text variant="h2" style={{ marginBottom: 24 }}>
        Spacing Scale (8px Grid)
      </Text>

      {Object.entries(spacing).map(([key, value]) => (
        <View key={key} style={{ marginBottom: 16 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <View
              style={{
                width: value,
                height: 40,
                backgroundColor: theme.colors.primary[500],
                marginRight: 16,
              }}
            />
            <View>
              <Text variant="body">spacing.{key}</Text>
              <Text variant="caption" color={theme.colors.text.secondary}>
                {value}px
              </Text>
            </View>
          </View>
        </View>
      ))}
    </View>
  );
};

export const BorderRadius = () => {
  const { theme } = useTheme();

  return (
    <View style={{ padding: 20 }}>
      <Text variant="h2" style={{ marginBottom: 24 }}>
        Border Radius Scale
      </Text>

      {Object.entries(borderRadius).map(([key, value]) => (
        <View key={key} style={{ marginBottom: 16 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <View
              style={{
                width: 80,
                height: 80,
                backgroundColor: theme.colors.primary[100],
                borderRadius: value,
                marginRight: 16,
                borderWidth: 2,
                borderColor: theme.colors.primary[500],
              }}
            />
            <View>
              <Text variant="body">borderRadius.{key}</Text>
              <Text variant="caption" color={theme.colors.text.secondary}>
                {value === 9999 ? 'Full (pill)' : `${value}px`}
              </Text>
            </View>
          </View>
        </View>
      ))}
    </View>
  );
};
