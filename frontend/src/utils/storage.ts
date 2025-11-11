/**
 * Cross-platform storage utility
 * Uses localStorage for web and AsyncStorage for React Native
 */

import { Platform } from 'react-native';

// Simple localStorage wrapper for web
const webStorage = {
  async getItem(key: string): Promise<string | null> {
    try {
      return localStorage.getItem(key);
    } catch (error) {
      console.error('Error getting item from localStorage:', error);
      return null;
    }
  },

  async setItem(key: string, value: string): Promise<void> {
    try {
      localStorage.setItem(key, value);
    } catch (error) {
      console.error('Error setting item in localStorage:', error);
    }
  },

  async removeItem(key: string): Promise<void> {
    try {
      localStorage.removeItem(key);
    } catch (error) {
      console.error('Error removing item from localStorage:', error);
    }
  },

  async clear(): Promise<void> {
    try {
      localStorage.clear();
    } catch (error) {
      console.error('Error clearing localStorage:', error);
    }
  },
};

// Use localStorage for web, AsyncStorage for native
export const storage = Platform.OS === 'web'
  ? webStorage
  : require('@react-native-async-storage/async-storage').default;
