/**
 * Authentication Context Provider
 * Manages authentication state, login, logout, and token persistence
 */

import React, { createContext, useState, useEffect, ReactNode } from 'react';
import { AuthContextType, AuthState, User } from '../types/auth';
import { apiService, STORAGE_KEYS } from '../services/api';
import AsyncStorage from '@react-native-async-storage/async-storage';

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
      const accessToken = await AsyncStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);

      if (accessToken) {
        // Token exists, user is authenticated
        // In a real app, we'd decode the JWT to get user info
        // For now, we'll just set isAuthenticated to true
        setAuthState({
          user: null, // TODO: Decode JWT or fetch user profile
          isAuthenticated: true,
          isLoading: false,
          error: null,
        });
      } else {
        setAuthState({
          ...initialAuthState,
          isLoading: false,
        });
      }
    } catch (error) {
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

      // Update auth state
      setAuthState({
        user: null, // TODO: Decode JWT or fetch user profile
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
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

  const value: AuthContextType = {
    ...authState,
    login,
    logout,
    refreshAccessToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
