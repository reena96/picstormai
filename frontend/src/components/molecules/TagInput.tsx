/**
 * Tag Input Component - Complete Tag Management UI
 * Integrates AutocompleteInput for tag suggestions
 * Displays existing tags as chips with remove functionality
 * Enforces max 10 tags per photo limit
 * Story 3.3: Photo Tagging UI
 */

import React, { useState, useEffect } from 'react';
import { View, StyleSheet } from 'react-native';
import { AutocompleteInput, AutocompleteSuggestion } from './AutocompleteInput';
import { TagChip } from './TagChip';
import { Text } from '../atoms/Text';
import { useTheme } from '../../hooks/useTheme';

export interface Tag {
  id: string;
  name: string;
  color: string;
}

export interface TagInputProps {
  photoId: string;
  tags: Tag[];
  availableTags: Tag[];
  onAddTag: (tagName: string) => Promise<Tag>;
  onRemoveTag: (tagId: string) => Promise<void>;
  maxTags?: number;
  testID?: string;
}

export const TagInput: React.FC<TagInputProps> = ({
  photoId,
  tags,
  availableTags,
  onAddTag,
  onRemoveTag,
  maxTags = 10,
  testID,
}) => {
  const { theme } = useTheme();
  const [inputValue, setInputValue] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isMaxTagsReached = tags.length >= maxTags;

  const handleAddTag = async (tagName: string) => {
    if (isMaxTagsReached) {
      setError(`Maximum ${maxTags} tags per photo`);
      return;
    }

    setIsAdding(true);
    setError(null);

    try {
      await onAddTag(tagName);
      setInputValue('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add tag');
    } finally {
      setIsAdding(false);
    }
  };

  const handleSelectSuggestion = async (suggestion: AutocompleteSuggestion) => {
    await handleAddTag(suggestion.name);
  };

  const handleRemoveTag = async (tagId: string) => {
    setError(null);
    try {
      await onRemoveTag(tagId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove tag');
    }
  };

  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => setError(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  return (
    <View style={styles.container} testID={testID}>
      {/* Display existing tags */}
      {tags.length > 0 && (
        <View style={styles.tagsContainer} testID={`${testID}-tags`}>
          {tags.map((tag) => (
            <TagChip
              key={tag.id}
              label={tag.name}
              color={tag.color}
              editable={true}
              onRemove={() => handleRemoveTag(tag.id)}
              testID={`${testID}-chip-${tag.id}`}
              style={styles.tagChip}
            />
          ))}
        </View>
      )}

      {/* Tag input field with autocomplete */}
      <AutocompleteInput
        value={inputValue}
        onChangeText={setInputValue}
        onSubmit={handleAddTag}
        suggestions={availableTags}
        onSelectSuggestion={handleSelectSuggestion}
        placeholder={
          isMaxTagsReached ? `Max ${maxTags} tags reached` : 'Add tag...'
        }
        maxLength={30}
        disabled={isMaxTagsReached || isAdding}
        testID={`${testID}-autocomplete`}
      />

      {/* Error message */}
      {error && (
        <Text
          variant="caption"
          style={styles.errorText}
          color={theme.colors.error[500] || '#EF4444'}
          testID={`${testID}-error`}
        >
          {error}
        </Text>
      )}

      {/* Tag count */}
      <Text
        variant="caption"
        style={styles.countText}
        color={theme.colors.text.secondary}
        testID={`${testID}-count`}
      >
        {tags.length} / {maxTags} tags
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginTop: 16,
  },
  tagsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 12,
  },
  tagChip: {
    marginRight: 8,
    marginBottom: 8,
  },
  errorText: {
    marginTop: 4,
  },
  countText: {
    marginTop: 4,
  },
});
