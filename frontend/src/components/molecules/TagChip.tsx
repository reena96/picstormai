/**
 * TagChip Component (Molecule)
 * Small pill-shaped tag with optional remove button
 * Supports click for filtering and remove for editing
 */

import React from 'react';
import { View, Pressable, StyleSheet, ViewStyle } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { Text } from '../atoms/Text';
import { X } from 'lucide-react-native';

export interface TagChipProps {
  label: string;
  color?: string;
  editable?: boolean;
  onPress?: () => void;
  onRemove?: () => void;
  testID?: string;
  style?: ViewStyle;
}

export const TagChip: React.FC<TagChipProps> = ({
  label,
  color,
  editable = false,
  onPress,
  onRemove,
  testID,
  style,
}) => {
  const { theme } = useTheme();

  const containerStyle: ViewStyle = {
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: theme.spacing[2],
    paddingRight: editable ? theme.spacing[1] : theme.spacing[2],
    paddingVertical: theme.spacing[1],
    borderRadius: theme.borderRadius.full,
    backgroundColor: color || theme.colors.primary[100],
    borderWidth: 1,
    borderColor: color ? 'transparent' : theme.colors.primary[300],
    alignSelf: 'flex-start',
  };

  const textColor = color ? theme.colors.white : theme.colors.primary[700];

  const handlePress = () => {
    if (onPress && !editable) {
      onPress();
    }
  };

  const handleRemove = () => {
    if (onRemove && editable) {
      onRemove();
    }
  };

  const chipContent = (
    <View style={containerStyle}>
      <Text
        variant="caption"
        weight="medium"
        color={textColor}
        numberOfLines={1}
        style={{ maxWidth: 120 }}
      >
        {label}
      </Text>
      {editable && onRemove && (
        <Pressable
          onPress={handleRemove}
          accessibilityRole="button"
          accessibilityLabel={`Remove ${label} tag`}
          hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
          style={{ marginLeft: theme.spacing[1] }}
        >
          <X size={12} color={textColor} />
        </Pressable>
      )}
    </View>
  );

  if (onPress && !editable) {
    return (
      <Pressable
        onPress={handlePress}
        accessibilityRole="button"
        accessibilityLabel={`Filter by ${label}`}
        testID={testID}
        style={({ pressed }) => [
          { opacity: pressed ? 0.7 : 1 },
          style,
        ]}
      >
        {chipContent}
      </Pressable>
    );
  }

  return <View style={style} testID={testID}>{chipContent}</View>;
};
