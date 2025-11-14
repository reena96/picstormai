/**
 * TagFilterBar Tests
 * Story 3.4: Tag Filter & Search
 */

import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { TagFilterBar, Tag } from './TagFilterBar';

// Mock useTheme hook
jest.mock('../../hooks/useTheme', () => ({
  useTheme: () => ({
    theme: {
      colors: {
        surface: '#FFFFFF',
        background: '#F5F5F5',
        primary: '#007AFF',
        text: {
          primary: '#000000',
          secondary: '#6B7280',
        },
      },
    },
  }),
}));

const mockTags: Tag[] = [
  { id: '1', name: 'vacation', color: '#3B82F6' },
  { id: '2', name: 'family', color: '#EF4444' },
  { id: '3', name: 'work', color: '#10B981' },
];

describe('TagFilterBar', () => {
  it('renders all available tags sorted alphabetically', () => {
    const { getByText } = render(
      <TagFilterBar
        availableTags={mockTags}
        selectedTagIds={[]}
        onToggleTag={jest.fn()}
        onClearAll={jest.fn()}
      />
    );

    expect(getByText('family')).toBeTruthy();
    expect(getByText('vacation')).toBeTruthy();
    expect(getByText('work')).toBeTruthy();
  });

  it('calls onToggleTag when tag is clicked', () => {
    const onToggleTag = jest.fn();
    const { getByTestId } = render(
      <TagFilterBar
        availableTags={mockTags}
        selectedTagIds={[]}
        onToggleTag={onToggleTag}
        onClearAll={jest.fn()}
        testID="test-filter-bar"
      />
    );

    fireEvent.press(getByTestId('test-filter-bar-tag-1'));
    expect(onToggleTag).toHaveBeenCalledWith('1');
  });

  it('shows selected state for active tags', () => {
    const { getByTestId } = render(
      <TagFilterBar
        availableTags={mockTags}
        selectedTagIds={['1', '2']}
        onToggleTag={jest.fn()}
        onClearAll={jest.fn()}
        testID="test-filter-bar"
      />
    );

    const tag1 = getByTestId('test-filter-bar-tag-1');
    const tag3 = getByTestId('test-filter-bar-tag-3');

    // Selected tags should have accessibility state
    expect(tag1.props.accessibilityState).toEqual({ selected: true });
    expect(tag3.props.accessibilityState).toEqual({ selected: false });
  });

  it('calls onClearAll when clear button is clicked', () => {
    const onClearAll = jest.fn();
    const { getByTestId } = render(
      <TagFilterBar
        availableTags={mockTags}
        selectedTagIds={['1', '2']}
        onToggleTag={jest.fn()}
        onClearAll={onClearAll}
        testID="test-filter-bar"
      />
    );

    fireEvent.press(getByTestId('test-filter-bar-clear-button'));
    expect(onClearAll).toHaveBeenCalled();
  });

  it('shows filtered photo count when provided', () => {
    const { getByTestId, getByText } = render(
      <TagFilterBar
        availableTags={mockTags}
        selectedTagIds={['1']}
        onToggleTag={jest.fn()}
        onClearAll={jest.fn()}
        filteredPhotoCount={5}
        testID="test-filter-bar"
      />
    );

    expect(getByText('Showing 5 photos')).toBeTruthy();
  });

  it('shows singular "photo" when count is 1', () => {
    const { getByText } = render(
      <TagFilterBar
        availableTags={mockTags}
        selectedTagIds={['1']}
        onToggleTag={jest.fn()}
        onClearAll={jest.fn()}
        filteredPhotoCount={1}
      />
    );

    expect(getByText('Showing 1 photo')).toBeTruthy();
  });

  it('shows clear button with count when tags are selected', () => {
    const { getByText } = render(
      <TagFilterBar
        availableTags={mockTags}
        selectedTagIds={['1', '2']}
        onToggleTag={jest.fn()}
        onClearAll={jest.fn()}
      />
    );

    expect(getByText('Clear all (2)')).toBeTruthy();
  });

  it('hides clear button when no tags are selected', () => {
    const { queryByText } = render(
      <TagFilterBar
        availableTags={mockTags}
        selectedTagIds={[]}
        onToggleTag={jest.fn()}
        onClearAll={jest.fn()}
      />
    );

    expect(queryByText(/Clear all/)).toBeNull();
  });

  it('returns null when no tags are available', () => {
    const { queryByTestId } = render(
      <TagFilterBar
        availableTags={[]}
        selectedTagIds={[]}
        onToggleTag={jest.fn()}
        onClearAll={jest.fn()}
        testID="test-filter-bar"
      />
    );

    expect(queryByTestId('test-filter-bar')).toBeNull();
  });

  it('has correct accessibility labels', () => {
    const { getByTestId } = render(
      <TagFilterBar
        availableTags={[mockTags[0]]}
        selectedTagIds={[]}
        onToggleTag={jest.fn()}
        onClearAll={jest.fn()}
        testID="test-filter-bar"
      />
    );

    const tag = getByTestId('test-filter-bar-tag-1');
    expect(tag.props.accessibilityLabel).toBe('Filter by vacation');
    expect(tag.props.accessibilityRole).toBe('button');
  });

  it('renders horizontal ScrollView for tags', () => {
    const { getByTestId } = render(
      <TagFilterBar
        availableTags={mockTags}
        selectedTagIds={[]}
        onToggleTag={jest.fn()}
        onClearAll={jest.fn()}
        testID="test-filter-bar"
      />
    );

    const scrollView = getByTestId('test-filter-bar-scroll');
    expect(scrollView.props.horizontal).toBe(true);
    expect(scrollView.props.showsHorizontalScrollIndicator).toBe(false);
  });
});
