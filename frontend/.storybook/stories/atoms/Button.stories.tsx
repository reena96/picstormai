import React from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Button } from '../../../src/components/atoms/Button';
import { View } from 'react-native';

const meta: Meta<typeof Button> = {
  title: 'Atoms/Button',
  component: Button,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'text', 'fab'],
    },
    size: {
      control: 'select',
      options: ['small', 'medium', 'large'],
    },
    disabled: { control: 'boolean' },
    loading: { control: 'boolean' },
  },
};

export default meta;
type Story = StoryObj<typeof Button>;

export const Primary: Story = {
  args: {
    variant: 'primary',
    children: 'Primary Button',
    onPress: () => console.log('Pressed'),
  },
};

export const Secondary: Story = {
  args: {
    variant: 'secondary',
    children: 'Secondary Button',
    onPress: () => console.log('Pressed'),
  },
};

export const Text: Story = {
  args: {
    variant: 'text',
    children: 'Text Button',
    onPress: () => console.log('Pressed'),
  },
};

export const FAB: Story = {
  args: {
    variant: 'fab',
    children: '+',
    onPress: () => console.log('Pressed'),
  },
};

export const Loading: Story = {
  args: {
    variant: 'primary',
    loading: true,
    children: 'Loading',
    onPress: () => console.log('Pressed'),
  },
};

export const Disabled: Story = {
  args: {
    variant: 'primary',
    disabled: true,
    children: 'Disabled',
    onPress: () => console.log('Pressed'),
  },
};

export const AllSizes: Story = {
  render: () => (
    <View style={{ gap: 16 }}>
      <Button variant="primary" size="small" onPress={() => {}}>Small Button</Button>
      <Button variant="primary" size="medium" onPress={() => {}}>Medium Button</Button>
      <Button variant="primary" size="large" onPress={() => {}}>Large Button</Button>
    </View>
  ),
};

export const AllVariants: Story = {
  render: () => (
    <View style={{ gap: 16 }}>
      <Button variant="primary" onPress={() => {}}>Primary</Button>
      <Button variant="secondary" onPress={() => {}}>Secondary</Button>
      <Button variant="text" onPress={() => {}}>Text</Button>
    </View>
  ),
};
