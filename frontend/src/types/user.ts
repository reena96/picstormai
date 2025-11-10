/**
 * User-related types
 */

export interface UserPreferences {
  userId: string;
  animationsEnabled: boolean;
  soundEnabled: boolean;
  theme: 'LIGHT' | 'DARK' | 'SYSTEM';
  concurrentUploads: number;
  uploadCompleteNotifications: boolean;
  autoRetryFailed: boolean;
}

export interface UpdateUserPreferencesRequest {
  animationsEnabled: boolean;
  soundEnabled: boolean;
  theme: string;
  concurrentUploads: number;
  uploadCompleteNotifications: boolean;
  autoRetryFailed: boolean;
}
