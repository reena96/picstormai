/**
 * Main entry point for PicStormAI frontend
 * Bootstraps React Native Web application
 */

import React from 'react';
import { AppRegistry } from 'react-native';
import App from './App';

console.log('=== index.tsx: Starting application bootstrap ===');
console.log('index.tsx: Platform:', typeof document !== 'undefined' ? 'web' : 'native');

// Register the app for React Native
console.log('index.tsx: Registering component "PicStormAI"');
AppRegistry.registerComponent('PicStormAI', () => App);

// Run the app for web
if (typeof document !== 'undefined') {
  console.log('index.tsx: Running application for web');
  const rootElement = document.getElementById('root');
  console.log('index.tsx: Root element found:', !!rootElement);

  if (!rootElement) {
    console.error('index.tsx: ERROR - Root element not found! Cannot mount app.');
  } else {
    console.log('index.tsx: Calling AppRegistry.runApplication');
    AppRegistry.runApplication('PicStormAI', {
      rootTag: rootElement,
    });
    console.log('index.tsx: AppRegistry.runApplication completed');
  }
} else {
  console.log('index.tsx: Not running on web platform (document is undefined)');
}

console.log('=== index.tsx: Bootstrap complete ===');

export default App;
