/**
 * useAuth hook for accessing authentication context
 * Provides login, logout, and auth state to components
 */

import { useContext } from 'react';
import { AuthContext } from '../contexts/AuthContext';
import { AuthContextType } from '../types/auth';

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);

  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  return context;
};

export default useAuth;
