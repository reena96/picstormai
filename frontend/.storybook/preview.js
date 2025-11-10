import React from 'react';
import { ThemeProvider } from '../src/contexts/ThemeContext';

export const decorators = [
  (Story) => (
    <ThemeProvider>
      <div style={{ padding: '20px' }}>
        <Story />
      </div>
    </ThemeProvider>
  ),
];

export const parameters = {
  actions: { argTypesRegex: '^on[A-Z].*' },
  controls: {
    matchers: {
      color: /(background|color)$/i,
      date: /Date$/,
    },
  },
};
