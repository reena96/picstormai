/**
 * API service with axios instance and interceptors
 * Handles authentication, token refresh, and automatic retry
 */

import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { storage } from '../utils/storage';
import { LoginRequest, LoginResponse, RefreshTokenResponse, User } from '../types/auth';

// Storage keys
export const STORAGE_KEYS = {
  ACCESS_TOKEN: '@auth:accessToken',
  REFRESH_TOKEN: '@auth:refreshToken',
} as const;

// API base URL - defaults to localhost for development
// @ts-ignore - process.env is provided by webpack
const API_BASE_URL = (typeof process !== 'undefined' && process.env?.REACT_APP_API_URL) || 'http://localhost:8080';

class ApiService {
  private axiosInstance: AxiosInstance;
  private isRefreshing = false;
  private failedQueue: Array<{
    resolve: (value?: unknown) => void;
    reject: (reason?: unknown) => void;
  }> = [];

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 10000,
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor: attach access token
    this.axiosInstance.interceptors.request.use(
      async (config: InternalAxiosRequestConfig) => {
        const token = await storage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor: handle 401 and refresh token
    this.axiosInstance.interceptors.response.use(
      (response: AxiosResponse) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

        // If error is 401 and we haven't tried to refresh yet
        if (error.response?.status === 401 && !originalRequest._retry) {
          if (this.isRefreshing) {
            // Wait for the refresh to complete
            return new Promise((resolve, reject) => {
              this.failedQueue.push({ resolve, reject });
            })
              .then(() => {
                return this.axiosInstance(originalRequest);
              })
              .catch((err) => {
                return Promise.reject(err);
              });
          }

          originalRequest._retry = true;
          this.isRefreshing = true;

          try {
            const newAccessToken = await this.refreshToken();
            this.processQueue(null);

            // Retry original request with new token
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
            }
            return this.axiosInstance(originalRequest);
          } catch (refreshError) {
            this.processQueue(refreshError);
            // Clear tokens and redirect to login
            await this.clearTokens();
            return Promise.reject(refreshError);
          } finally {
            this.isRefreshing = false;
          }
        }

        return Promise.reject(error);
      }
    );
  }

  private processQueue(error: unknown) {
    this.failedQueue.forEach((promise) => {
      if (error) {
        promise.reject(error);
      } else {
        promise.resolve();
      }
    });
    this.failedQueue = [];
  }

  private async refreshToken(): Promise<string> {
    const refreshToken = await storage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await axios.post<RefreshTokenResponse>(
      `${API_BASE_URL}/api/auth/refresh`,
      { refreshToken }
    );

    const { accessToken, refreshToken: newRefreshToken } = response.data;

    // Store new tokens
    await storage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
    await storage.setItem(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken);

    return accessToken;
  }

  private async clearTokens(): Promise<void> {
    await storage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
    await storage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
  }

  // Public API methods

  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await this.axiosInstance.post<LoginResponse>(
      '/api/auth/login',
      credentials
    );
    return response.data;
  }

  async logout(): Promise<void> {
    // Future: Call backend /api/auth/logout to revoke tokens
    await this.clearTokens();
  }

  async saveTokens(tokens: LoginResponse): Promise<void> {
    await storage.setItem(STORAGE_KEYS.ACCESS_TOKEN, tokens.accessToken);
    await storage.setItem(STORAGE_KEYS.REFRESH_TOKEN, tokens.refreshToken);
  }

  async getAccessToken(): Promise<string | null> {
    return storage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
  }

  async getUserProfile(): Promise<User> {
    const response = await this.axiosInstance.get<User>('/api/user/profile');
    return response.data;
  }

  async markOnboardingComplete(): Promise<User> {
    const response = await this.axiosInstance.patch<User>('/api/user/onboarding');
    return response.data;
  }

  getInstance(): AxiosInstance {
    return this.axiosInstance;
  }
}

// Export singleton instance
export const apiService = new ApiService();
export default apiService;
