import React from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { PhotoCard } from '../../../src/components/molecules/PhotoCard';
import { View } from 'react-native';

const meta: Meta<typeof PhotoCard> = {
  title: 'Molecules/PhotoCard',
  component: PhotoCard,
  tags: ['autodocs'],
  argTypes: {
    loading: { control: 'boolean' },
  },
};

export default meta;
type Story = StoryObj<typeof PhotoCard>;

const mockPhoto = {
  photoUrl: 'https://picsum.photos/400/400',
  thumbnailUrl: 'https://picsum.photos/200/200',
  filename: 'sunset-beach.jpg',
  uploadDate: new Date('2024-11-09'),
  fileSize: 2457600, // 2.4 MB
  tags: ['sunset', 'beach', 'nature'],
  onPress: () => console.log('Photo clicked'),
};

export const Default: Story = {
  args: mockPhoto,
};

export const Loading: Story = {
  args: {
    ...mockPhoto,
    loading: true,
  },
};

export const WithoutTags: Story = {
  args: {
    ...mockPhoto,
    tags: [],
  },
};

export const LongFilename: Story = {
  args: {
    ...mockPhoto,
    filename: 'very-long-filename-that-should-be-truncated-properly.jpg',
  },
};

export const Grid: Story = {
  render: () => (
    <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 16 }}>
      {Array.from({ length: 6 }).map((_, i) => (
        <View key={i} style={{ width: 200 }}>
          <PhotoCard
            photoUrl={`https://picsum.photos/400/400?random=${i}`}
            thumbnailUrl={`https://picsum.photos/200/200?random=${i}`}
            filename={`photo-${i + 1}.jpg`}
            uploadDate={new Date()}
            fileSize={1024 * 1024 * (i + 1)}
            onPress={() => console.log(`Photo ${i} clicked`)}
          />
        </View>
      ))}
    </View>
  ),
};
