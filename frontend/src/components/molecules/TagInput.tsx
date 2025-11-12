/**
 * Tag Input Component - Add/Remove Tags
 * Story 3.3: Photo Tagging UI
 */

import React, { useState } from 'react';
import { View, TextInput, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { X } from 'lucide-react-native';

interface Tag {
  id: string;
  name: string;
  color: string;
}

interface TagInputProps {
  tags: Tag[];
  onAddTag: (tagName: string) => void;
  onRemoveTag: (tagId: string) => void;
}

export const TagInput: React.FC<TagInputProps> = ({ tags, onAddTag, onRemoveTag }) => {
  const [inputValue, setInputValue] = useState('');

  const handleSubmit = () => {
    if (inputValue.trim()) {
      onAddTag(inputValue.trim());
      setInputValue('');
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.tagList}>
        {tags.map((tag) => (
          <View key={tag.id} style={[styles.tagChip, { backgroundColor: tag.color }]}>
            <Text style={styles.tagText}>{tag.name}</Text>
            <TouchableOpacity onPress={() => onRemoveTag(tag.id)}>
              <X size={16} color="#FFFFFF" />
            </TouchableOpacity>
          </View>
        ))}
      </View>
      <TextInput
        style={styles.input}
        value={inputValue}
        onChangeText={setInputValue}
        onSubmitEditing={handleSubmit}
        placeholder="Add tag..."
        maxLength={20}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { padding: 16 },
  tagList: { flexDirection: 'row', flexWrap: 'wrap', marginBottom: 12 },
  tagChip: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    marginRight: 8,
    marginBottom: 8,
  },
  tagText: { color: '#FFFFFF', marginRight: 6 },
  input: {
    borderWidth: 1,
    borderColor: '#DDD',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
  },
});
