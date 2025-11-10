/**
 * Login Screen
 * Allows users to log in with email and password
 * Includes form validation, error handling, and loading states
 */

import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  ViewStyle,
  TextStyle,
} from 'react-native';
import { useAuth } from '../hooks/useAuth';
import { useTheme } from '../hooks/useTheme';
import { Input } from '../components/atoms/Input';
import { Button } from '../components/atoms/Button';

export const LoginScreen: React.FC = () => {
  const { theme } = useTheme();
  const { login, isLoading, error: authError } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState('');
  const [formError, setFormError] = useState('');

  const validateEmail = (value: string): boolean => {
    const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$/;
    if (!value) {
      setEmailError('Email is required');
      return false;
    }
    if (!emailRegex.test(value)) {
      setEmailError('Please enter a valid email address');
      return false;
    }
    setEmailError('');
    return true;
  };

  const handleLogin = async () => {
    // Clear previous errors
    setFormError('');

    // Validate form
    const isEmailValid = validateEmail(email);
    if (!password) {
      setFormError('Password is required');
      return;
    }
    if (!isEmailValid) {
      return;
    }

    try {
      await login(email, password);
      // Navigation will be handled by AuthContext state change
    } catch (error: any) {
      setFormError(error.message || 'Invalid email or password');
    }
  };

  const isFormValid = email.length > 0 && password.length > 0 && !emailError;

  const styles = StyleSheet.create<{
    container: ViewStyle;
    scrollContent: ViewStyle;
    header: ViewStyle;
    title: TextStyle;
    subtitle: TextStyle;
    form: ViewStyle;
    errorText: TextStyle;
    buttonContainer: ViewStyle;
  }>({
    container: {
      flex: 1,
      backgroundColor: theme.colors.background,
    },
    scrollContent: {
      flexGrow: 1,
      justifyContent: 'center',
      paddingHorizontal: theme.spacing[4],
      paddingVertical: theme.spacing[8],
    },
    header: {
      marginBottom: theme.spacing[8],
      alignItems: 'center',
    },
    title: {
      fontSize: theme.typography.fontSize['3xl'],
      fontWeight: theme.typography.fontWeight.bold as TextStyle['fontWeight'],
      color: theme.colors.text.primary,
      fontFamily: theme.typography.fontFamily.primary,
      marginBottom: theme.spacing[2],
      textAlign: 'center',
    },
    subtitle: {
      fontSize: theme.typography.fontSize.base,
      color: theme.colors.text.secondary,
      fontFamily: theme.typography.fontFamily.primary,
      textAlign: 'center',
    },
    form: {
      width: '100%',
      maxWidth: 400,
      alignSelf: 'center',
    },
    errorText: {
      fontSize: theme.typography.fontSize.sm,
      color: theme.colors.error[500],
      fontFamily: theme.typography.fontFamily.primary,
      marginBottom: theme.spacing[4],
      textAlign: 'center',
    },
    buttonContainer: {
      marginTop: theme.spacing[2],
    },
  });

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 20}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.header}>
          <Text style={styles.title}>Welcome Back</Text>
          <Text style={styles.subtitle}>Log in to continue to RapidPhotoUpload</Text>
        </View>

        <View style={styles.form}>
          {(formError || authError) && (
            <Text style={styles.errorText} accessibilityRole="alert">
              {formError || authError}
            </Text>
          )}

          <Input
            type="email"
            label="Email"
            placeholder="Enter your email"
            value={email}
            onChangeText={(text) => {
              setEmail(text);
              if (emailError) validateEmail(text);
              if (formError) setFormError('');
            }}
            error={emailError}
            disabled={isLoading}
            testID="login-email-input"
            accessibilityLabel="Email address"
          />

          <Input
            type="password"
            label="Password"
            placeholder="Enter your password"
            value={password}
            onChangeText={(text) => {
              setPassword(text);
              if (formError) setFormError('');
            }}
            disabled={isLoading}
            testID="login-password-input"
            accessibilityLabel="Password"
          />

          <View style={styles.buttonContainer}>
            <Button
              variant="primary"
              size="large"
              onPress={handleLogin}
              disabled={!isFormValid || isLoading}
              loading={isLoading}
              testID="login-button"
              accessibilityLabel="Log in"
            >
              Log In
            </Button>
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

export default LoginScreen;
