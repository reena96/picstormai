import React from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { PhotoGrid } from '../../../src/components/organisms/PhotoGrid';

const meta: Meta<typeof PhotoGrid> = {
  title: 'Organisms/PhotoGrid',
  component: PhotoGrid,
  tags: ['autodocs'],
  parameters: {
    layout: 'fullscreen',
  },
};

export default meta;
type Story = StoryObj<typeof PhotoGrid>;

const mockPhotos = Array.from({ length: 20 }).map((_, i) => ({
  photoUrl: `https://picsum.photos/800/800?random=${i}`,
  thumbnailUrl: `https://picsum.photos/400/400?random=${i}`,
  filename: `photo-${i + 1}.jpg`,
  uploadDate: new Date(Date.now() - i * 86400000),
  fileSize: Math.floor(Math.random() * 5000000) + 500000,
  tags: ['nature', 'landscape'].slice(0, Math.floor(Math.random() * 3)),
  onPress: () => console.log(`Photo ${i} clicked`),
}));

export const WithPhotos: Story = {
  args: {
    photos: mockPhotos,
    onPhotoPress: (index) => console.log('Photo pressed:', index),
    loading: false,
  },
};

export const Loading: Story = {
  args: {
    photos: [],
    onPhotoPress: (index) => console.log('Photo pressed:', index),
    loading: true,
  },
};

export const Empty: Story = {
  args: {
    photos: [],
    onPhotoPress: (index) => console.log('Photo pressed:', index),
    loading: false,
    emptyMessage: 'No photos uploaded yet. Start uploading to see them here!',
  },
};

export const LoadMore: Story = {
  args: {
    photos: mockPhotos,
    onPhotoPress: (index) => console.log('Photo pressed:', index),
    loading: false,
    hasMore: true,
    onLoadMore: () => console.log('Loading more photos...'),
  },
};
