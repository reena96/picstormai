/**
 * Main entry point for PicStormAI frontend
 * Bootstraps React Native Web application
 */

import React from 'react';
import { AppRegistry } from 'react-native';
import App from './App';

// Register the app for React Native
AppRegistry.registerComponent('PicStormAI', () => App);

// Run the app for web
if (typeof document !== 'undefined') {
  AppRegistry.runApplication('PicStormAI', {
    rootTag: document.getElementById('root'),
  });
}

export default App;
