/**
 * Authentication Context Provider
 * Manages authentication state, login, logout, and token persistence
 */

import React, { createContext, useState, useEffect, ReactNode } from 'react';
import { AuthContextType, AuthState, User } from '../types/auth';
import { apiService, STORAGE_KEYS } from '../services/api';
import { storage } from '../utils/storage';

const initialAuthState: AuthState = {
  user: null,
  isAuthenticated: false,
  isLoading: true,
  error: null,
};

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>(initialAuthState);

  // Check for stored tokens on mount
  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      console.log('AuthContext: Starting checkAuthStatus');
      const accessToken = await storage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      console.log('AuthContext: Access token exists:', !!accessToken);

      if (accessToken) {
        // Token exists, fetch user profile to validate it
        try {
          console.log('AuthContext: Fetching user profile');
          const user = await apiService.getUserProfile();
          console.log('AuthContext: User profile fetched successfully');
          setAuthState({
            user,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          });
        } catch (error) {
          // If profile fetch fails (invalid/expired token), clear tokens and log out
          console.log('AuthContext: Token validation failed, clearing stored tokens', error);
          await storage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
          await storage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
          setAuthState({
            user: null,
            isAuthenticated: false,
            isLoading: false,
            error: null,
          });
        }
      } else {
        console.log('AuthContext: No access token, setting unauthenticated state');
        setAuthState({
          ...initialAuthState,
          isLoading: false,
        });
      }
    } catch (error) {
      console.error('AuthContext: Caught error in checkAuthStatus:', error);
      setAuthState({
        ...initialAuthState,
        isLoading: false,
        error: 'Failed to check authentication status',
      });
    }
  };

  const login = async (email: string, password: string): Promise<void> => {
    try {
      setAuthState((prev) => ({
        ...prev,
        isLoading: true,
        error: null,
      }));

      const response = await apiService.login({ email, password });

      // Save tokens
      await apiService.saveTokens(response);

      // Fetch user profile
      try {
        const user = await apiService.getUserProfile();
        setAuthState({
          user,
          isAuthenticated: true,
          isLoading: false,
          error: null,
        });
      } catch (profileError) {
        // Login succeeded but profile fetch failed - this shouldn't happen normally
        console.error('Login succeeded but profile fetch failed:', profileError);
        setAuthState({
          user: null,
          isAuthenticated: true,
          isLoading: false,
          error: null,
        });
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message ||
                          error.message ||
                          'Invalid email or password';

      setAuthState({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: errorMessage,
      });

      throw new Error(errorMessage);
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await apiService.logout();

      setAuthState({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      });
    } catch (error: any) {
      // Even if logout fails, clear local state
      setAuthState({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      });
    }
  };

  const refreshAccessToken = async (): Promise<string> => {
    // This is handled by the API service interceptor
    const token = await apiService.getAccessToken();
    if (!token) {
      throw new Error('No access token available');
    }
    return token;
  };

  const markOnboardingComplete = async (): Promise<void> => {
    try {
      const updatedUser = await apiService.markOnboardingComplete();
      setAuthState((prev) => ({
        ...prev,
        user: updatedUser,
      }));
    } catch (error) {
      console.error('Failed to mark onboarding complete:', error);
      // Don't throw - onboarding completion is not critical
    }
  };

  const value: AuthContextType = {
    ...authState,
    login,
    logout,
    refreshAccessToken,
    markOnboardingComplete,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
