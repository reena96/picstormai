/**
 * TagFilterBar - Horizontal scrollable tag filter component
 * Story 3.4: Tag Filter & Search
 */

import React from 'react';
import { View, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { Text } from '../atoms/Text';
import { Button } from '../atoms/Button';
import { useTheme } from '../../hooks/useTheme';

export interface Tag {
  id: string;
  name: string;
  color: string;
}

export interface TagFilterBarProps {
  availableTags: Tag[];
  selectedTagIds: string[];
  onToggleTag: (tagId: string) => void;
  onClearAll: () => void;
  filteredPhotoCount?: number;
  testID?: string;
}

export const TagFilterBar: React.FC<TagFilterBarProps> = ({
  availableTags,
  selectedTagIds,
  onToggleTag,
  onClearAll,
  filteredPhotoCount,
  testID = 'tag-filter-bar',
}) => {
  const { theme } = useTheme();

  if (availableTags.length === 0) {
    return null; // No tags to filter by
  }

  const selectedCount = selectedTagIds.length;

  // Sort tags alphabetically by name
  const sortedTags = [...availableTags].sort((a, b) =>
    a.name.localeCompare(b.name)
  );

  return (
    <View
      style={[styles.container, { backgroundColor: theme.colors.surface }]}
      testID={testID}
    >
      {/* Header */}
      <View style={styles.header}>
        <Text variant="caption" color={theme.colors.text.secondary}>
          Filter by tags
        </Text>
        {selectedCount > 0 && (
          <TouchableOpacity
            onPress={onClearAll}
            testID={`${testID}-clear-button`}
            accessible
            accessibilityRole="button"
            accessibilityLabel="Clear all filters"
          >
            <Text variant="caption" color={theme.colors.primary} weight="600">
              Clear all ({selectedCount})
            </Text>
          </TouchableOpacity>
        )}
      </View>

      {/* Tag Chips */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        style={styles.scrollView}
        contentContainerStyle={styles.tagsContainer}
        testID={`${testID}-scroll`}
      >
        {sortedTags.map((tag) => {
          const isSelected = selectedTagIds.includes(tag.id);
          return (
            <TouchableOpacity
              key={tag.id}
              onPress={() => onToggleTag(tag.id)}
              testID={`${testID}-tag-${tag.id}`}
              accessible
              accessibilityRole="button"
              accessibilityLabel={`Filter by ${tag.name}`}
              accessibilityState={{ selected: isSelected }}
            >
              <View
                style={[
                  styles.chip,
                  {
                    backgroundColor: isSelected ? tag.color : theme.colors.background,
                    borderColor: tag.color,
                    borderWidth: isSelected ? 0 : 2,
                  },
                  isSelected && styles.chipSelected,
                ]}
              >
                <Text
                  variant="caption"
                  weight="600"
                  style={[
                    styles.chipText,
                    { color: isSelected ? '#FFFFFF' : theme.colors.text.primary },
                  ]}
                >
                  {tag.name}
                </Text>
              </View>
            </TouchableOpacity>
          );
        })}
      </ScrollView>

      {/* Filtered Count */}
      {selectedCount > 0 && filteredPhotoCount !== undefined && (
        <Text
          variant="caption"
          style={[styles.countText, { color: theme.colors.text.secondary }]}
          testID={`${testID}-count`}
        >
          Showing {filteredPhotoCount} {filteredPhotoCount === 1 ? 'photo' : 'photos'}
        </Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#E5E7EB',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  scrollView: {
    paddingHorizontal: 16,
  },
  tagsContainer: {
    flexDirection: 'row',
    gap: 8,
    paddingRight: 16,
  },
  chip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    marginRight: 8,
  },
  chipSelected: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.15,
    shadowRadius: 4,
    elevation: 3,
  },
  chipText: {
    fontSize: 14,
  },
  countText: {
    paddingHorizontal: 16,
    marginTop: 8,
  },
});
