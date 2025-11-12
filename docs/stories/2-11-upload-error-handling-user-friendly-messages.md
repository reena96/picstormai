# Story 2.11: Upload Error Handling & User-Friendly Messages

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase C - Network Resilience (Weeks 5-6)
**Status**: Draft
**Priority**: High
**Estimated Effort**: 2 days

---

## User Story

**As a** user
**I want to** see clear error messages when uploads fail
**So that** I understand what went wrong and can fix it

---

## Acceptance Criteria

### AC1: User-Friendly Error Messages
**Given** photo upload fails
**When** error occurs
**Then** I see user-friendly error message (not technical stack trace)
**And** message explains what went wrong in plain language
**And** message suggests action if possible

### AC2: Error Message Mapping
**Given** various technical errors occur
**Then** appropriate user message is displayed:
- Network timeout (408) → "Network issue. Upload will retry automatically."
- S3 permission denied (403) → "Upload failed. Please contact support."
- File too large (413) → "File exceeds 50MB limit."
- Invalid file type (400) → "Only image files are supported."
- Server error (500) → "Server error. Please try again."
- Unknown error → "Upload failed. Please try again."

### AC3: Error Display in Photo Card
**Given** photo upload fails
**Then** photo card shows:
- Red "Failed" badge
- Error message (user-friendly)
- "Retry" button
- Red X icon overlay on thumbnail

### AC4: Error Toast Notification
**Given** photo upload fails
**When** error occurs
**Then** toast notification appears with error message
**And** toast auto-dismisses after 5 seconds
**And** toast has error styling (red background)

### AC5: Error Logging to Backend
**Given** upload error occurs
**When** error is caught
**Then** full error details logged to backend for debugging
**And** log includes: userId, sessionId, photoId, error type, stack trace, timestamp

---

## Technical Notes

### Error Mapping Service

```typescript
// ErrorMapper.ts
export interface UserFriendlyError {
  title: string;
  message: string;
  action?: string;
  retryable: boolean;
}

export class ErrorMapper {
  static mapError(error: any): UserFriendlyError {
    // Network errors
    if (error.code === 'ECONNABORTED' || error.code === 'ERR_NETWORK') {
      return {
        title: 'Network Issue',
        message: 'Connection timeout. Upload will retry automatically.',
        retryable: true,
      };
    }

    // HTTP errors
    if (error.response) {
      const status = error.response.status;

      switch (status) {
        case 400:
          return {
            title: 'Invalid File',
            message: 'Only image files are supported (JPG, PNG, GIF, WebP).',
            retryable: false,
          };

        case 403:
          return {
            title: 'Access Denied',
            message: 'Upload not authorized. Please contact support.',
            action: 'Contact Support',
            retryable: false,
          };

        case 408:
          return {
            title: 'Timeout',
            message: 'Network timeout. Retrying...',
            retryable: true,
          };

        case 413:
          return {
            title: 'File Too Large',
            message: 'File exceeds 50MB limit. Please choose a smaller file.',
            retryable: false,
          };

        case 500:
        case 502:
        case 503:
          return {
            title: 'Server Error',
            message: 'Server temporarily unavailable. Please try again.',
            retryable: true,
          };

        default:
          return {
            title: 'Upload Failed',
            message: `Error ${status}. Please try again.`,
            retryable: true,
          };
      }
    }

    // S3-specific errors
    if (error.message?.includes('NoSuchBucket')) {
      return {
        title: 'Configuration Error',
        message: 'Storage not configured correctly. Please contact support.',
        action: 'Contact Support',
        retryable: false,
      };
    }

    if (error.message?.includes('AccessDenied')) {
      return {
        title: 'Access Denied',
        message: 'Upload not authorized. Please contact support.',
        action: 'Contact Support',
        retryable: false,
      };
    }

    // Default fallback
    return {
      title: 'Upload Failed',
      message: 'Something went wrong. Please try again.',
      retryable: true,
    };
  }
}
```

### Error Logging Service

```typescript
// ErrorLogger.ts
export interface ErrorLog {
  userId: string;
  sessionId: string;
  photoId: string;
  errorType: string;
  errorMessage: string;
  stackTrace?: string;
  httpStatus?: number;
  timestamp: string;
  userAgent: string;
  platform: string;
}

export class ErrorLogger {
  constructor(private apiClient: ApiClient) {}

  async logError(
    error: any,
    context: {
      userId: string;
      sessionId: string;
      photoId: string;
    }
  ): Promise<void> {
    try {
      const errorLog: ErrorLog = {
        userId: context.userId,
        sessionId: context.sessionId,
        photoId: context.photoId,
        errorType: error.constructor.name,
        errorMessage: error.message || 'Unknown error',
        stackTrace: error.stack,
        httpStatus: error.response?.status,
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        platform: Platform.OS,
      };

      await this.apiClient.logUploadError(errorLog);
    } catch (logError) {
      // Don't let logging errors crash the app
      console.error('Failed to log error:', logError);
    }
  }
}
```

### Enhanced Upload Manager with Error Handling

```typescript
// UploadManager.ts (enhanced with error handling)
export class UploadManager {
  private errorLogger: ErrorLogger;
  private errorMapper = ErrorMapper;

  async uploadPhoto(
    photoSelection: PhotoSelection,
    sessionId: string,
    userId: string
  ): Promise<UploadResult> {
    try {
      // ... existing upload logic ...
      await this.uploadToS3(uploadUrl, file);

      return { success: true };
    } catch (error) {
      // Map to user-friendly error
      const userError = this.errorMapper.mapError(error);

      // Log full error to backend
      await this.errorLogger.logError(error, {
        userId,
        sessionId,
        photoId: photoSelection.id,
      });

      // Emit error event for UI
      this.emit('uploadError', {
        photoId: photoSelection.id,
        error: userError,
      });

      return {
        success: false,
        error: userError,
      };
    }
  }
}
```

### Error Display Components

```typescript
// PhotoCard.tsx (with error display)
export const PhotoCard: React.FC<PhotoCardProps> = ({ photo, progress, error }) => {
  if (error) {
    return (
      <View style={styles.card}>
        <View style={styles.thumbnailContainer}>
          <Image source={{ uri: photo.thumbnail }} style={styles.thumbnail} />
          <View style={styles.errorOverlay}>
            <Text style={styles.errorIcon}>✕</Text>
          </View>
        </View>

        <View style={styles.info}>
          <Text style={styles.fileName} numberOfLines={1}>
            {photo.fileName}
          </Text>

          <StatusBadge type="error" text="Failed" />

          <Text style={styles.errorMessage} numberOfLines={2}>
            {error.message}
          </Text>

          {error.retryable && (
            <Button
              size="small"
              variant="secondary"
              onPress={() => retryUpload(photo.id)}
              accessibilityLabel={`Retry uploading ${photo.fileName}`}
            >
              Retry
            </Button>
          )}

          {error.action === 'Contact Support' && (
            <Button
              size="small"
              variant="link"
              onPress={() => openSupportChat()}
              accessibilityLabel="Contact support"
            >
              Contact Support
            </Button>
          )}
        </View>
      </View>
    );
  }

  // ... normal photo card rendering ...
};

const styles = StyleSheet.create({
  errorOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(239, 68, 68, 0.8)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorIcon: {
    fontSize: 32,
    color: 'white',
    fontWeight: 'bold',
  },
  errorMessage: {
    fontSize: 12,
    color: '#ef4444',
    marginTop: 4,
    marginBottom: 8,
  },
});
```

```typescript
// ErrorToast.tsx
import React, { useEffect } from 'react';
import { View, Text, StyleSheet, Animated } from 'react-native';

interface ErrorToastProps {
  visible: boolean;
  title: string;
  message: string;
  onDismiss: () => void;
}

export const ErrorToast: React.FC<ErrorToastProps> = ({
  visible,
  title,
  message,
  onDismiss,
}) => {
  const translateY = useRef(new Animated.Value(-100)).current;

  useEffect(() => {
    if (visible) {
      // Slide down
      Animated.timing(translateY, {
        toValue: 0,
        duration: 300,
        useNativeDriver: true,
      }).start();

      // Auto-dismiss after 5 seconds
      const timer = setTimeout(() => {
        Animated.timing(translateY, {
          toValue: -100,
          duration: 300,
          useNativeDriver: true,
        }).start(() => onDismiss());
      }, 5000);

      return () => clearTimeout(timer);
    }
  }, [visible]);

  if (!visible) return null;

  return (
    <Animated.View
      style={[styles.toast, { transform: [{ translateY }] }]}
      accessibilityLiveRegion="polite"
      accessibilityLabel={`Error: ${title}. ${message}`}
    >
      <View style={styles.iconContainer}>
        <Text style={styles.icon}>✕</Text>
      </View>
      <View style={styles.content}>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.message}>{message}</Text>
      </View>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  toast: {
    position: 'absolute',
    top: 16,
    left: 16,
    right: 16,
    backgroundColor: '#ef4444',
    borderRadius: 8,
    padding: 16,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
    zIndex: 9999,
  },
  iconContainer: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  icon: {
    fontSize: 20,
    color: 'white',
    fontWeight: 'bold',
  },
  content: {
    flex: 1,
  },
  title: {
    fontSize: 16,
    fontWeight: '600',
    color: 'white',
    marginBottom: 4,
  },
  message: {
    fontSize: 14,
    color: 'rgba(255, 255, 255, 0.9)',
  },
});
```

### Backend Error Logging Endpoint

```java
// ErrorLogController.java
@RestController
@RequestMapping("/api/errors")
public class ErrorLogController {

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @PostMapping("/upload-errors")
    public Mono<ResponseEntity<Void>> logUploadError(
        @RequestBody @Valid ErrorLogRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        ErrorLog errorLog = new ErrorLog(
            user.getUserId(),
            new UploadSessionId(request.sessionId()),
            new PhotoId(request.photoId()),
            request.errorType(),
            request.errorMessage(),
            request.stackTrace(),
            request.httpStatus(),
            Instant.now(),
            request.userAgent(),
            request.platform()
        );

        return errorLogRepository.save(errorLog)
            .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).build()));
    }
}

record ErrorLogRequest(
    @NotBlank String sessionId,
    @NotBlank String photoId,
    @NotBlank String errorType,
    @NotBlank String errorMessage,
    String stackTrace,
    Integer httpStatus,
    @NotBlank String userAgent,
    @NotBlank String platform
) {}
```

---

## Prerequisites
- Story 2.4 (Upload Engine) - MUST BE COMPLETE

---

## Testing Requirements

### Unit Tests
- [ ] ErrorMapper correctly maps 403 error
- [ ] ErrorMapper correctly maps 408 timeout
- [ ] ErrorMapper correctly maps network error
- [ ] ErrorMapper sets retryable=true for network errors
- [ ] ErrorMapper sets retryable=false for 413 (file too large)
- [ ] ErrorLogger sends error to backend API

### Integration Tests
- [ ] Upload fails with 403, verify user sees "Access Denied" message
- [ ] Upload fails with timeout, verify user sees retry message
- [ ] Error is logged to backend with correct fields
- [ ] Error toast appears and auto-dismisses after 5 seconds

### E2E Tests
- [ ] Upload invalid file type, see "Only image files supported" error
- [ ] Simulate 500 error, see "Server error" message with retry button
- [ ] Retry failed upload, verify it retries correctly
- [ ] Contact support button opens support chat (if applicable)

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] ErrorMapper implemented with all error codes
- [ ] ErrorLogger implemented
- [ ] Photo card displays error with retry button
- [ ] Error toast component implemented
- [ ] Backend error logging endpoint working
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] E2E tests passing
- [ ] Code reviewed and approved

---

## Notes
- User-friendly messages reduce user frustration and support tickets
- Full error logging to backend enables debugging production issues
- Retryable flag helps UI decide whether to show retry button
- Action field (e.g., "Contact Support") provides next steps for user
- Toast notification provides immediate feedback without blocking UI

---

**Status Log:**
- 2025-11-11: Story created (Draft)
