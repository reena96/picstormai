/**
 * Main entry point for PicStormAI frontend
 * Bootstraps React Native Web application with theme provider
 */

import React from 'react';
import { AppRegistry } from 'react-native';
import { ThemeProvider } from './contexts/ThemeContext';
import App from './App';

const RootApp = () => (
  <ThemeProvider>
    <App />
  </ThemeProvider>
);

// Register the app for React Native
AppRegistry.registerComponent('PicStormAI', () => RootApp);

// Run the app for web
if (typeof document !== 'undefined') {
  AppRegistry.runApplication('PicStormAI', {
    rootTag: document.getElementById('root'),
  });
}

export default RootApp;
