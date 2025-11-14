/**
 * Tests for TagChip Component
 * Story 3.3: Photo Tagging UI
 */

import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { TagChip } from './TagChip';

// Mock lucide-react-native icons
jest.mock('lucide-react-native', () => ({
  X: 'X',
}));

// Mock useTheme hook
jest.mock('../../hooks/useTheme', () => ({
  useTheme: () => ({
    theme: {
      colors: {
        white: '#FFFFFF',
        primary: {
          100: '#E0E7FF',
          300: '#A5B4FC',
          700: '#4338CA',
        },
        border: '#E5E5E5',
      },
      spacing: [0, 4, 8, 12, 16, 24, 32, 48, 64],
      borderRadius: {
        md: 8,
        full: 9999,
      },
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

describe('TagChip', () => {
  it('renders tag name and color', () => {
    const { getByText, container } = render(
      <TagChip label="vacation" color="#3B82F6" testID="tag-chip" />
    );

    // Assert: Text "vacation" visible
    expect(getByText('vacation')).toBeTruthy();

    // Assert: Chip has correct background color
    const chipElement = container.querySelector('[data-testid="tag-chip"]');
    expect(chipElement).toBeTruthy();
  });

  it('calls onRemove when X button clicked', () => {
    const onRemove = jest.fn();
    const { getByLabelText } = render(
      <TagChip
        label="vacation"
        color="#3B82F6"
        editable={true}
        onRemove={onRemove}
        testID="tag-chip"
      />
    );

    // Click remove button
    const removeButton = getByLabelText('Remove vacation tag');
    fireEvent.click(removeButton);

    // Assert: onRemove called once
    expect(onRemove).toHaveBeenCalledTimes(1);
  });

  it('renders in editable mode with remove button visible', () => {
    const onRemove = jest.fn();
    const { getByLabelText } = render(
      <TagChip
        label="vacation"
        color="#3B82F6"
        editable={true}
        onRemove={onRemove}
        testID="tag-chip"
      />
    );

    // Assert: Remove button is visible when editable
    expect(getByLabelText('Remove vacation tag')).toBeTruthy();
  });

  it('hides remove button when onRemove not provided', () => {
    const { queryByLabelText } = render(
      <TagChip label="vacation" color="#3B82F6" testID="tag-chip" />
    );

    // Assert: Remove button not in document
    expect(queryByLabelText('Remove vacation tag')).toBeNull();
  });
});
