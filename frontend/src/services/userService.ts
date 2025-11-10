/**
 * User service for user-related API calls
 * Handles user preferences
 */

import { apiService } from './api';
import { UserPreferences, UpdateUserPreferencesRequest } from '../types/user';

export const userService = {
  /**
   * Get current user preferences
   */
  async getPreferences(): Promise<UserPreferences> {
    const response = await apiService.getInstance().get<UserPreferences>('/api/user/preferences');
    return response.data;
  },

  /**
   * Update user preferences
   */
  async updatePreferences(preferences: UpdateUserPreferencesRequest): Promise<UserPreferences> {
    const response = await apiService.getInstance().put<UserPreferences>(
      '/api/user/preferences',
      preferences
    );
    return response.data;
  },
};

export default userService;
