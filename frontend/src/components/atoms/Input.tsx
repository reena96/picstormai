/**
 * Input Component (Atom)
 * Types: text, email, password
 * States: default, focus, error, disabled
 * WCAG 2.1 AA compliant with proper labels and error messaging
 */

import React, { useState } from 'react';
import { View, TextInput, Text, StyleSheet, ViewStyle, TextStyle, Pressable } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { Eye, EyeOff } from 'lucide-react-native';

export interface InputProps {
  type?: 'text' | 'email' | 'password';
  label?: string;
  placeholder?: string;
  value: string;
  onChangeText: (text: string) => void;
  error?: string;
  disabled?: boolean;
  multiline?: boolean;
  numberOfLines?: number;
  accessibilityLabel?: string;
  testID?: string;
  style?: ViewStyle;
}

export const Input: React.FC<InputProps> = ({
  type = 'text',
  label,
  placeholder,
  value,
  onChangeText,
  error,
  disabled = false,
  multiline = false,
  numberOfLines = 1,
  accessibilityLabel,
  testID,
  style,
}) => {
  const { theme } = useTheme();
  const [isFocused, setIsFocused] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const containerStyle: ViewStyle = {
    marginBottom: theme.spacing[2],
  };

  const labelStyle: TextStyle = {
    fontSize: theme.typography.fontSize.sm,
    fontWeight: theme.typography.fontWeight.medium as TextStyle['fontWeight'],
    color: theme.colors.text.primary,
    marginBottom: theme.spacing[1],
    fontFamily: theme.typography.fontFamily.primary,
  };

  const inputContainerStyle: ViewStyle = {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 2,
    borderRadius: theme.borderRadius.base,
    borderColor: error
      ? theme.colors.error[500]
      : isFocused
      ? theme.colors.primary[500]
      : theme.colors.border,
    backgroundColor: disabled ? theme.colors.gray[100] : theme.colors.surface,
    minHeight: 44, // WCAG minimum touch target
    paddingHorizontal: theme.spacing[3],
  };

  const inputStyle: TextStyle = {
    flex: 1,
    fontSize: theme.typography.fontSize.base,
    color: disabled ? theme.colors.text.disabled : theme.colors.text.primary,
    fontFamily: theme.typography.fontFamily.primary,
    paddingVertical: theme.spacing[2],
  };

  const errorStyle: TextStyle = {
    fontSize: theme.typography.fontSize.sm,
    color: theme.colors.error[500],
    marginTop: theme.spacing[1],
    fontFamily: theme.typography.fontFamily.primary,
  };

  const getKeyboardType = () => {
    if (type === 'email') return 'email-address';
    return 'default';
  };

  const getSecureTextEntry = () => {
    return type === 'password' && !showPassword;
  };

  return (
    <View style={[containerStyle, style]}>
      {label && (
        <Text style={labelStyle} accessibilityRole="text">
          {label}
        </Text>
      )}
      <View style={inputContainerStyle}>
        <TextInput
          value={value}
          onChangeText={onChangeText}
          placeholder={placeholder}
          placeholderTextColor={theme.colors.text.disabled}
          editable={!disabled}
          secureTextEntry={getSecureTextEntry()}
          keyboardType={getKeyboardType()}
          autoCapitalize={type === 'email' ? 'none' : 'sentences'}
          autoCorrect={type === 'email' ? false : true}
          multiline={multiline}
          numberOfLines={numberOfLines}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          accessibilityLabel={accessibilityLabel || label}
          accessibilityState={{ disabled }}
          aria-invalid={!!error}
          aria-errormessage={error}
          testID={testID}
          style={inputStyle}
        />
        {type === 'password' && (
          <Pressable
            onPress={() => setShowPassword(!showPassword)}
            accessibilityRole="button"
            accessibilityLabel={showPassword ? 'Hide password' : 'Show password'}
            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
          >
            {showPassword ? (
              <EyeOff size={20} color={theme.colors.text.secondary} />
            ) : (
              <Eye size={20} color={theme.colors.text.secondary} />
            )}
          </Pressable>
        )}
      </View>
      {error && (
        <Text style={errorStyle} accessibilityRole="alert">
          {error}
        </Text>
      )}
    </View>
  );
};
