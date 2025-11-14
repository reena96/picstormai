/**
 * AutocompleteInput Component
 * Text input with dropdown autocomplete suggestions
 * Supports keyboard navigation (Arrow Up/Down, Enter, Escape)
 * Story 3.3: Photo Tagging UI
 */

import React, { useState, useCallback, useRef, useEffect } from 'react';
import {
  View,
  TextInput,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  Platform,
  Keyboard,
  ViewStyle,
  TextStyle,
} from 'react-native';
import { Text } from '../atoms/Text';
import { useTheme } from '../../hooks/useTheme';

export interface AutocompleteSuggestion {
  id: string;
  name: string;
  color?: string;
}

export interface AutocompleteInputProps {
  value: string;
  onChangeText: (text: string) => void;
  onSubmit: (text: string) => void;
  suggestions: AutocompleteSuggestion[];
  onSelectSuggestion: (suggestion: AutocompleteSuggestion) => void;
  placeholder?: string;
  maxLength?: number;
  disabled?: boolean;
  testID?: string;
}

export const AutocompleteInput: React.FC<AutocompleteInputProps> = ({
  value,
  onChangeText,
  onSubmit,
  suggestions,
  onSelectSuggestion,
  placeholder = 'Type tag name...',
  maxLength = 30,
  disabled = false,
  testID,
}) => {
  const { theme } = useTheme();
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const inputRef = useRef<TextInput>(null);

  // Filter suggestions by partial match (case-insensitive)
  const filteredSuggestions = suggestions.filter((s) =>
    s.name.toLowerCase().includes(value.toLowerCase())
  );

  const handleSubmit = useCallback(() => {
    if (value.trim()) {
      if (selectedIndex >= 0 && filteredSuggestions[selectedIndex]) {
        onSelectSuggestion(filteredSuggestions[selectedIndex]);
      } else {
        onSubmit(value.trim());
      }
      onChangeText('');
      setShowSuggestions(false);
      setSelectedIndex(-1);
      if (Platform.OS !== 'web') {
        Keyboard.dismiss();
      }
    }
  }, [value, selectedIndex, filteredSuggestions, onSelectSuggestion, onSubmit, onChangeText]);

  const handleKeyPress = useCallback(
    (e: any) => {
      if (Platform.OS === 'web') {
        if (e.nativeEvent.key === 'ArrowDown') {
          e.preventDefault();
          setSelectedIndex((prev) =>
            prev < filteredSuggestions.length - 1 ? prev + 1 : prev
          );
        } else if (e.nativeEvent.key === 'ArrowUp') {
          e.preventDefault();
          setSelectedIndex((prev) => (prev > 0 ? prev - 1 : -1));
        } else if (e.nativeEvent.key === 'Escape') {
          setShowSuggestions(false);
          setSelectedIndex(-1);
        }
      }
    },
    [filteredSuggestions.length]
  );

  const handleFocus = () => {
    if (value.length > 0 && filteredSuggestions.length > 0) {
      setShowSuggestions(true);
    }
  };

  const handleChangeText = (text: string) => {
    onChangeText(text);
    setShowSuggestions(text.length > 0 && filteredSuggestions.length > 0);
    setSelectedIndex(-1);
  };

  const handleSelectSuggestion = (suggestion: AutocompleteSuggestion) => {
    onSelectSuggestion(suggestion);
    onChangeText('');
    setShowSuggestions(false);
    setSelectedIndex(-1);
  };

  useEffect(() => {
    // Update suggestions visibility when filtered suggestions change
    if (value.length > 0 && filteredSuggestions.length > 0) {
      setShowSuggestions(true);
    } else {
      setShowSuggestions(false);
    }
  }, [filteredSuggestions.length, value.length]);

  const inputStyle: TextStyle = {
    height: 40,
    borderWidth: 1,
    borderRadius: theme.borderRadius.md,
    paddingHorizontal: theme.spacing[3],
    fontSize: 14,
    backgroundColor: theme.colors.background,
    color: theme.colors.text.primary,
    borderColor: theme.colors.border,
  };

  const inputDisabledStyle: TextStyle = {
    opacity: 0.5,
    backgroundColor: theme.colors.surface,
  };

  const suggestionContainerStyle: ViewStyle = {
    position: 'absolute',
    top: 42,
    left: 0,
    right: 0,
    maxHeight: 200,
    borderRadius: theme.borderRadius.md,
    backgroundColor: theme.colors.background,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 5,
    zIndex: 1001,
  };

  return (
    <View style={styles.container} testID={testID}>
      <TextInput
        ref={inputRef}
        value={value}
        onChangeText={handleChangeText}
        onSubmitEditing={handleSubmit}
        onFocus={handleFocus}
        onKeyPress={handleKeyPress}
        placeholder={placeholder}
        placeholderTextColor={theme.colors.text.secondary}
        maxLength={maxLength}
        editable={!disabled}
        style={[inputStyle, disabled && inputDisabledStyle]}
        testID={`${testID}-input`}
        returnKeyType="done"
      />

      {showSuggestions && filteredSuggestions.length > 0 && (
        <View style={suggestionContainerStyle} testID={`${testID}-suggestions`}>
          <FlatList
            data={filteredSuggestions}
            keyExtractor={(item) => item.id}
            renderItem={({ item, index }) => (
              <TouchableOpacity
                onPress={() => handleSelectSuggestion(item)}
                style={[
                  styles.suggestionItem,
                  {
                    backgroundColor:
                      index === selectedIndex
                        ? theme.colors.primary[100]
                        : 'transparent',
                    borderBottomColor: theme.colors.border,
                  },
                ]}
                testID={`${testID}-suggestion-${item.id}`}
              >
                {item.color && (
                  <View
                    style={[
                      styles.colorIndicator,
                      { backgroundColor: item.color },
                    ]}
                  />
                )}
                <Text variant="body" style={styles.suggestionText}>
                  {item.name}
                </Text>
              </TouchableOpacity>
            )}
            style={styles.suggestionsList}
            keyboardShouldPersistTaps="handled"
          />
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'relative',
    zIndex: 1000,
  },
  suggestionsList: {
    maxHeight: 200,
  },
  suggestionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 12,
    borderBottomWidth: 1,
  },
  colorIndicator: {
    width: 16,
    height: 16,
    borderRadius: 8,
    marginRight: 10,
  },
  suggestionText: {
    flex: 1,
  },
});
