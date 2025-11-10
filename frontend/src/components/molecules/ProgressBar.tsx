/**
 * ProgressBar Component (Molecule)
 * Linear progress indicator with gradient fill and indeterminate state
 * WCAG compliant with role="progressbar" and aria-valuenow
 */

import React, { useEffect, useRef } from 'react';
import { View, Animated, StyleSheet, ViewStyle } from 'react-native';
import { useTheme } from '../../hooks/useTheme';

export interface ProgressBarProps {
  progress: number; // 0-100
  indeterminate?: boolean;
  color?: string;
  height?: number;
  testID?: string;
  style?: ViewStyle;
}

export const ProgressBar: React.FC<ProgressBarProps> = ({
  progress,
  indeterminate = false,
  color,
  height = 8,
  testID,
  style,
}) => {
  const { theme } = useTheme();
  const animatedProgress = useRef(new Animated.Value(0)).current;
  const indeterminateAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!indeterminate) {
      Animated.timing(animatedProgress, {
        toValue: Math.min(Math.max(progress, 0), 100),
        duration: theme.animations.duration.normal,
        useNativeDriver: false,
      }).start();
    }
  }, [progress, indeterminate, animatedProgress, theme.animations.duration.normal]);

  useEffect(() => {
    if (indeterminate) {
      Animated.loop(
        Animated.sequence([
          Animated.timing(indeterminateAnim, {
            toValue: 1,
            duration: 1500,
            useNativeDriver: true,
          }),
          Animated.timing(indeterminateAnim, {
            toValue: 0,
            duration: 1500,
            useNativeDriver: true,
          }),
        ])
      ).start();
    }
  }, [indeterminate, indeterminateAnim]);

  const containerStyle: ViewStyle = {
    height,
    backgroundColor: theme.colors.gray[200],
    borderRadius: theme.borderRadius.full,
    overflow: 'hidden',
  };

  const fillStyle: ViewStyle = {
    height: '100%',
    backgroundColor: color || theme.colors.primary[500],
    borderRadius: theme.borderRadius.full,
  };

  const progressWidth = animatedProgress.interpolate({
    inputRange: [0, 100],
    outputRange: ['0%', '100%'],
  });

  const indeterminateTranslate = indeterminateAnim.interpolate({
    inputRange: [0, 1],
    outputRange: ['-100%', '200%'],
  });

  return (
    <View
      style={[containerStyle, style]}
      accessibilityRole="progressbar"
      accessibilityValue={{
        min: 0,
        max: 100,
        now: indeterminate ? undefined : progress,
      }}
      aria-valuenow={indeterminate ? undefined : progress}
      aria-valuemin={0}
      aria-valuemax={100}
      testID={testID}
    >
      {indeterminate ? (
        <Animated.View
          style={[
            fillStyle,
            {
              width: '50%',
              transform: [{ translateX: indeterminateTranslate }],
            },
          ]}
        />
      ) : (
        <Animated.View style={[fillStyle, { width: progressWidth }]} />
      )}
    </View>
  );
};
