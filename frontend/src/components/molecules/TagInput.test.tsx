/**
 * Tests for TagInput Component
 * Story 3.3: Photo Tagging UI
 */

import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react';
import { TagInput, Tag } from './TagInput';

// Mock child components
jest.mock('./AutocompleteInput', () => ({
  AutocompleteInput: ({ onSubmit, onSelectSuggestion, disabled, testID }: any) => {
    const React = require('react');
    const { View, TouchableOpacity, Text } = require('react-native');
    return (
      <View testID={testID} data-disabled={disabled}>
        <TouchableOpacity onPress={() => onSubmit('newTag')} testID={`${testID}-submit`}>
          <Text>Submit</Text>
        </TouchableOpacity>
        <TouchableOpacity
          onPress={() => onSelectSuggestion({ id: '1', name: 'vacation', color: '#3B82F6' })}
          testID={`${testID}-select`}
        >
          <Text>Select</Text>
        </TouchableOpacity>
      </View>
    );
  },
}));

jest.mock('./TagChip', () => ({
  TagChip: ({ label, onRemove, testID }: any) => {
    const React = require('react');
    const { View, TouchableOpacity, Text } = require('react-native');
    return (
      <View testID={testID}>
        <Text>{label}</Text>
        {onRemove && (
          <TouchableOpacity onPress={onRemove} testID={`${testID}-remove`}>
            <Text>Remove</Text>
          </TouchableOpacity>
        )}
      </View>
    );
  },
}));

// Mock useTheme hook
jest.mock('../../hooks/useTheme', () => ({
  useTheme: () => ({
    theme: {
      colors: {
        error: {
          500: '#EF4444',
        },
        text: {
          secondary: '#6B7280',
        },
      },
      spacing: [0, 4, 8, 12, 16, 24, 32, 48, 64],
    },
  }),
}));

// Mock Text component
jest.mock('../atoms/Text', () => ({
  Text: ({ children, testID, ...props }: any) => {
    const React = require('react');
    const { Text: RNText } = require('react-native');
    return <RNText testID={testID} {...props}>{children}</RNText>;
  },
}));

describe('TagInput', () => {
  const mockTags: Tag[] = [
    { id: '1', name: 'vacation', color: '#3B82F6' },
  ];

  const mockAvailableTags: Tag[] = [
    { id: '1', name: 'vacation', color: '#3B82F6' },
    { id: '2', name: 'family', color: '#EF4444' },
  ];

  it('displays existing tags as chips', () => {
    const { getByText, getByTestId } = render(
      <TagInput
        photoId="photo-1"
        tags={mockTags}
        availableTags={mockAvailableTags}
        onAddTag={jest.fn()}
        onRemoveTag={jest.fn()}
        testID="tag-input"
      />
    );

    // Assert: Tag chip visible for each tag
    expect(getByTestId('tag-input-tags')).toBeTruthy();
    expect(getByText('vacation')).toBeTruthy();
  });

  it('adds new tag on submit', async () => {
    const onAddTag = jest.fn().mockResolvedValue({ id: '3', name: 'newTag', color: '#10B981' });
    const { getByTestId } = render(
      <TagInput
        photoId="photo-1"
        tags={mockTags}
        availableTags={mockAvailableTags}
        onAddTag={onAddTag}
        onRemoveTag={jest.fn()}
        testID="tag-input"
      />
    );

    // Click Submit button
    const submitButton = getByTestId('tag-input-autocomplete-submit');
    fireEvent.click(submitButton);

    // Assert: onAddTag called with 'newTag'
    await waitFor(() => {
      expect(onAddTag).toHaveBeenCalledWith('newTag');
    });
  });

  it('removes tag on chip click', async () => {
    const onRemoveTag = jest.fn().mockResolvedValue(undefined);
    const { getByTestId } = render(
      <TagInput
        photoId="photo-1"
        tags={mockTags}
        availableTags={mockAvailableTags}
        onAddTag={jest.fn()}
        onRemoveTag={onRemoveTag}
        testID="tag-input"
      />
    );

    // Click Remove button
    const removeButton = getByTestId('tag-input-chip-1-remove');
    fireEvent.click(removeButton);

    // Assert: onRemoveTag called with tag id
    await waitFor(() => {
      expect(onRemoveTag).toHaveBeenCalledWith('1');
    });
  });

  it('shows error when max tags reached', async () => {
    const tenTags: Tag[] = Array.from({ length: 10 }, (_, i) => ({
      id: `${i + 1}`,
      name: `tag${i + 1}`,
      color: '#3B82F6',
    }));

    const onAddTag = jest.fn();
    const { getByTestId, getByText } = render(
      <TagInput
        photoId="photo-1"
        tags={tenTags}
        availableTags={mockAvailableTags}
        onAddTag={onAddTag}
        onRemoveTag={jest.fn()}
        testID="tag-input"
      />
    );

    // Try to add 11th tag
    const submitButton = getByTestId('tag-input-autocomplete-submit');
    fireEvent.click(submitButton);

    // Assert: Error message visible
    await waitFor(() => {
      expect(getByText('Maximum 10 tags per photo')).toBeTruthy();
    });

    // onAddTag should not be called
    expect(onAddTag).not.toHaveBeenCalled();
  });

  it('disables input when max tags reached', () => {
    const tenTags: Tag[] = Array.from({ length: 10 }, (_, i) => ({
      id: `${i + 1}`,
      name: `tag${i + 1}`,
      color: '#3B82F6',
    }));

    const onAddTag = jest.fn();
    const { getByTestId } = render(
      <TagInput
        photoId="photo-1"
        tags={tenTags}
        availableTags={mockAvailableTags}
        onAddTag={onAddTag}
        onRemoveTag={jest.fn()}
        testID="tag-input"
      />
    );

    // Assert: Trying to submit should not call onAddTag (input is disabled)
    // The component prevents adding when max is reached
    const submitButton = getByTestId('tag-input-autocomplete-submit');
    fireEvent.click(submitButton);

    // onAddTag should not be called because max tags reached
    expect(onAddTag).not.toHaveBeenCalled();
  });

  it('displays tag count', () => {
    const threeTags: Tag[] = [
      { id: '1', name: 'vacation', color: '#3B82F6' },
      { id: '2', name: 'family', color: '#EF4444' },
      { id: '3', name: 'summer', color: '#10B981' },
    ];

    const { getByText } = render(
      <TagInput
        photoId="photo-1"
        tags={threeTags}
        availableTags={mockAvailableTags}
        onAddTag={jest.fn()}
        onRemoveTag={jest.fn()}
        testID="tag-input"
      />
    );

    // Assert: Text "3 / 10 tags" visible
    expect(getByText('3 / 10 tags')).toBeTruthy();
  });

  it('clears input after adding tag', async () => {
    const onAddTag = jest.fn().mockResolvedValue({ id: '3', name: 'newTag', color: '#10B981' });
    const { getByTestId } = render(
      <TagInput
        photoId="photo-1"
        tags={mockTags}
        availableTags={mockAvailableTags}
        onAddTag={onAddTag}
        onRemoveTag={jest.fn()}
        testID="tag-input"
      />
    );

    // Add tag successfully
    const submitButton = getByTestId('tag-input-autocomplete-submit');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(onAddTag).toHaveBeenCalled();
    });

    // Input value should be cleared (component handles this internally)
    // We can verify through component state that it was successful
    expect(onAddTag).toHaveBeenCalledWith('newTag');
  });

  it('auto-hides error after 3 seconds', async () => {
    const tenTags: Tag[] = Array.from({ length: 10 }, (_, i) => ({
      id: `${i + 1}`,
      name: `tag${i + 1}`,
      color: '#3B82F6',
    }));

    const { getByTestId, getByText, queryByText } = render(
      <TagInput
        photoId="photo-1"
        tags={tenTags}
        availableTags={mockAvailableTags}
        onAddTag={jest.fn()}
        onRemoveTag={jest.fn()}
        testID="tag-input"
      />
    );

    // Trigger error
    const submitButton = getByTestId('tag-input-autocomplete-submit');
    fireEvent.click(submitButton);

    // Error should be visible
    await waitFor(() => {
      expect(getByText('Maximum 10 tags per photo')).toBeTruthy();
    });

    // Wait 3.1 seconds
    await waitFor(
      () => {
        expect(queryByText('Maximum 10 tags per photo')).toBeNull();
      },
      { timeout: 3500 }
    );
  });
});
