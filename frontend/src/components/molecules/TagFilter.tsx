/**
 * Tag Filter Component
 * Story 3.4: Tag Filter & Search
 */

import React from 'react';
import { View, ScrollView, TouchableOpacity, Text, StyleSheet } from 'react-native';

interface Tag {
  id: string;
  name: string;
  count: number;
}

interface TagFilterProps {
  tags: Tag[];
  selectedTags: string[];
  onToggleTag: (tagId: string) => void;
}

export const TagFilter: React.FC<TagFilterProps> = ({ tags, selectedTags, onToggleTag }) => {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Filter by Tags</Text>
      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        {tags.map((tag) => {
          const isSelected = selectedTags.includes(tag.id);
          return (
            <TouchableOpacity
              key={tag.id}
              style={[styles.tagButton, isSelected && styles.tagButtonActive]}
              onPress={() => onToggleTag(tag.id)}
            >
              <Text style={[styles.tagText, isSelected && styles.tagTextActive]}>
                {tag.name} ({tag.count})
              </Text>
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { padding: 16 },
  title: { fontSize: 16, fontWeight: 'bold', marginBottom: 12 },
  tagButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#F0F0F0',
    marginRight: 8,
  },
  tagButtonActive: { backgroundColor: '#007AFF' },
  tagText: { color: '#333' },
  tagTextActive: { color: '#FFFFFF' },
});
