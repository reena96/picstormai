import React, { useEffect, useRef } from 'react';
import { View, Text, StyleSheet, Modal, Pressable, Platform } from 'react-native';
import { Check, AlertCircle } from 'lucide-react-native';

interface UploadCompletionModalProps {
  visible: boolean;
  totalPhotos: number;
  successfulPhotos: number;
  failedPhotos: number;
  onViewPhotos: () => void;
  onRetryFailed?: () => void;
  onClose: () => void;
  soundEnabled?: boolean;
}

export const UploadCompletionModal: React.FC<UploadCompletionModalProps> = ({
  visible,
  totalPhotos,
  successfulPhotos,
  failedPhotos,
  onViewPhotos,
  onRetryFailed,
  onClose,
  soundEnabled = true,
}) => {
  const modalShownRef = useRef(false);

  useEffect(() => {
    if (visible && !modalShownRef.current) {
      modalShownRef.current = true;

      // Trigger confetti
      if (Platform.OS === 'web') {
        triggerWebConfetti();
      }

      // Play success sound if enabled
      if (soundEnabled && successfulPhotos > 0) {
        playSuccessSound();
      }

      // Trigger haptic feedback on mobile
      if (Platform.OS !== 'web') {
        triggerHapticFeedback();
      }

      // Auto-dismiss after 5 seconds ONLY for full success
      if (allSuccessful) {
        const timer = setTimeout(() => {
          onClose();
        }, 5000);

        return () => clearTimeout(timer);
      }
    }

    if (!visible) {
      modalShownRef.current = false;
    }
  }, [visible, successfulPhotos, soundEnabled, onClose]);

  const triggerWebConfetti = () => {
    if (Platform.OS === 'web' && typeof window !== 'undefined') {
      // Dynamically import canvas-confetti for web
      import('canvas-confetti').then((confetti) => {
        const count = 200;
        const defaults = {
          origin: { y: 0.7 },
          zIndex: 10000,
        };

        function fire(particleRatio: number, opts: any) {
          confetti.default({
            ...defaults,
            ...opts,
            particleCount: Math.floor(count * particleRatio),
          });
        }

        fire(0.25, {
          spread: 26,
          startVelocity: 55,
        });

        fire(0.2, {
          spread: 60,
        });

        fire(0.35, {
          spread: 100,
          decay: 0.91,
          scalar: 0.8,
        });

        fire(0.1, {
          spread: 120,
          startVelocity: 25,
          decay: 0.92,
          scalar: 1.2,
        });

        fire(0.1, {
          spread: 120,
          startVelocity: 45,
        });
      });
    }
  };

  const playSuccessSound = () => {
    // Placeholder for sound effect
    // In production, you would load and play an audio file
    // Example: new Audio('/sounds/success.mp3').play();
    console.log('Playing success sound');
  };

  const triggerHapticFeedback = () => {
    // Placeholder for haptic feedback
    // In React Native, you would use: Vibration.vibrate(100);
    console.log('Triggering haptic feedback');
  };

  const hasFailures = failedPhotos > 0;
  const allSuccessful = successfulPhotos === totalPhotos;

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <Pressable style={styles.overlay} onPress={onClose}>
        <Pressable style={styles.modal} onPress={(e) => e.stopPropagation()}>
          <View style={styles.content}>
            {allSuccessful ? (
              <>
                <View style={styles.iconContainer}>
                  <View style={[styles.iconCircle, styles.successCircle]}>
                    <Check size={48} color="#10b981" strokeWidth={3} />
                  </View>
                </View>
                <Text style={styles.title}>Upload Complete! 🎉</Text>
                <Text style={styles.message}>
                  All {totalPhotos} photos uploaded successfully
                </Text>
              </>
            ) : (
              <>
                <View style={styles.iconContainer}>
                  <View style={[styles.iconCircle, styles.warningCircle]}>
                    <AlertCircle size={48} color="#f59e0b" strokeWidth={3} />
                  </View>
                </View>
                <Text style={styles.title}>Upload Completed</Text>
                <Text style={styles.message}>
                  {successfulPhotos} of {totalPhotos} photos uploaded successfully
                </Text>
                {hasFailures && (
                  <Text style={styles.errorMessage}>
                    {failedPhotos} {failedPhotos === 1 ? 'photo' : 'photos'} failed
                  </Text>
                )}
              </>
            )}

            <View style={styles.buttonContainer}>
              <Pressable
                style={[styles.button, styles.primaryButton]}
                onPress={onViewPhotos}
              >
                <Text style={styles.primaryButtonText}>View Photos</Text>
              </Pressable>

              {hasFailures && onRetryFailed && (
                <Pressable
                  style={[styles.button, styles.secondaryButton]}
                  onPress={onRetryFailed}
                >
                  <Text style={styles.secondaryButtonText}>Retry Failed</Text>
                </Pressable>
              )}
            </View>
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modal: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 32,
    maxWidth: 400,
    width: '90%',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 12,
    elevation: 8,
  },
  content: {
    alignItems: 'center',
  },
  iconContainer: {
    marginBottom: 24,
  },
  iconCircle: {
    width: 96,
    height: 96,
    borderRadius: 48,
    justifyContent: 'center',
    alignItems: 'center',
  },
  successCircle: {
    backgroundColor: '#d1fae5',
  },
  warningCircle: {
    backgroundColor: '#fef3c7',
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 12,
    textAlign: 'center',
  },
  message: {
    fontSize: 16,
    color: '#6b7280',
    marginBottom: 8,
    textAlign: 'center',
  },
  errorMessage: {
    fontSize: 14,
    color: '#ef4444',
    marginBottom: 24,
    textAlign: 'center',
  },
  buttonContainer: {
    width: '100%',
    marginTop: 24,
  },
  button: {
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 12,
  },
  primaryButton: {
    backgroundColor: '#3b82f6',
  },
  primaryButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  secondaryButton: {
    backgroundColor: '#f3f4f6',
    borderWidth: 1,
    borderColor: '#d1d5db',
  },
  secondaryButtonText: {
    color: '#374151',
    fontSize: 16,
    fontWeight: '600',
  },
});
