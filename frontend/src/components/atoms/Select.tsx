/**
 * Select Component (Atom)
 * Simple dropdown using native select element for web
 * For mobile, could be enhanced with a modal picker later
 */

import React from 'react';
import { View, ViewStyle, Platform, TouchableOpacity } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { Text } from './Text';
import { ChevronDown } from 'lucide-react-native';
import { Icon } from './Icon';

export interface SelectOption {
  label: string;
  value: string;
}

export interface SelectProps {
  label?: string;
  value: string;
  options: SelectOption[];
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  testID?: string;
  style?: ViewStyle;
}

export const Select: React.FC<SelectProps> = ({
  label,
  value,
  options,
  onChange,
  placeholder = 'Select...',
  disabled = false,
  testID,
  style,
}) => {
  const { theme } = useTheme();

  const containerStyle: ViewStyle = {
    marginBottom: theme.spacing[4],
  };

  const labelStyle: ViewStyle = {
    marginBottom: theme.spacing[2],
  };

  const selectContainerStyle: ViewStyle = {
    borderWidth: 1,
    borderColor: theme.colors.gray[300],
    borderRadius: theme.borderRadius.base,
    backgroundColor: theme.colors.surface,
    height: 44,
    paddingHorizontal: theme.spacing[3],
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  };

  const selectedOption = options.find((opt) => opt.value === value);
  const displayText = selectedOption?.label || placeholder;

  if (Platform.OS === 'web') {
    return (
      <View style={[containerStyle, style]} testID={testID}>
        {label && (
          <View style={labelStyle}>
            <Text variant="body" weight="medium" color={theme.colors.text.primary}>
              {label}
            </Text>
          </View>
        )}
        <View style={{ position: 'relative' }}>
          <select
            value={value}
            onChange={(e: any) => onChange(e.target.value)}
            disabled={disabled}
            style={{
              width: '100%',
              height: 44,
              paddingLeft: theme.spacing[3],
              paddingRight: theme.spacing[3],
              fontSize: 16,
              color: theme.colors.text.primary,
              backgroundColor: theme.colors.surface,
              border: `1px solid ${theme.colors.gray[300]}`,
              borderRadius: theme.borderRadius.base,
              outline: 'none',
              cursor: disabled ? 'not-allowed' : 'pointer',
              appearance: 'none',
              WebkitAppearance: 'none',
            }}
          >
            {placeholder && !value && (
              <option value="" disabled>
                {placeholder}
              </option>
            )}
            {options.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <View
            style={{
              position: 'absolute',
              right: theme.spacing[3],
              top: '50%',
              transform: [{ translateY: -12 }],
              pointerEvents: 'none',
            }}
          >
            <Icon icon={ChevronDown} size="sm" color={theme.colors.text.secondary} />
          </View>
        </View>
      </View>
    );
  }

  // For mobile platforms, show a simple TouchableOpacity
  // In a production app, this would open a modal with options
  return (
    <View style={[containerStyle, style]} testID={testID}>
      {label && (
        <View style={labelStyle}>
          <Text variant="body" weight="medium" color={theme.colors.text.primary}>
            {label}
          </Text>
        </View>
      )}
      <TouchableOpacity
        style={selectContainerStyle}
        onPress={() => {
          // For now, cycle through options on mobile
          const currentIndex = options.findIndex((opt) => opt.value === value);
          const nextIndex = (currentIndex + 1) % options.length;
          onChange(options[nextIndex].value);
        }}
        disabled={disabled}
        testID={`${testID}-button`}
      >
        <Text
          variant="body"
          color={value ? theme.colors.text.primary : theme.colors.text.secondary}
        >
          {displayText}
        </Text>
        <Icon icon={ChevronDown} size="sm" color={theme.colors.text.secondary} />
      </TouchableOpacity>
    </View>
  );
};
