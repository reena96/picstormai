# Story 2.10: Network Loss Detection

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase C - Network Resilience (Weeks 5-6)
**Status**: Ready for Development
**Priority**: High
**Estimated Effort**: 2 days

---

## User Story

**As a** user
**I want to** be notified immediately when network connection is lost
**So that** I understand why uploads paused

---

## Acceptance Criteria

### AC1: Network Loss Banner
**Given** uploads are in progress
**When** network connection is lost
**Then** within 5 seconds I see banner "Connection lost. Uploads paused."
**And** banner appears at top of screen (persistent)
**And** banner has orange/warning color

### AC2: Uploads Paused Automatically
**Given** network connection is lost
**When** uploads are in progress
**Then** all active uploads are paused immediately
**And** queued uploads remain queued (not started)
**And** upload status changes to "paused"

### AC3: Connection Restored Banner
**Given** connection was lost
**When** network is back online
**Then** I see banner "Connection restored. Resuming uploads."
**And** banner has green/success color
**And** banner auto-dismisses after 3 seconds

### AC4: Uploads Resume Automatically
**Given** connection is restored
**When** uploads were paused
**Then** paused uploads resume automatically
**And** queued uploads start processing
**And** upload status changes back to "uploading"

### AC5: SSE Reconnection
**Given** SSE connection was disconnected due to network loss
**When** connection is restored
**Then** SSE reconnects automatically via EventSource API
**And** client resumes receiving session events
**And** state is synced with server via REST API fallback (Story 2.8)

---

## Technical Notes

### CRITICAL: SSE Architecture Compatibility

**IMPORTANT**: This story builds on the SSE (Server-Sent Events) architecture established in Phase B:

- **Story 2.6**: SSE server infrastructure with Spring WebFlux
- **Story 2.7**: Real-time progress broadcasting via Redis pub/sub to SSE
- **Story 2.8**: SSE client integration with `useSSE` hook and `SSEManager`

**SSE Reconnection (AC5)**: The `@microsoft/fetch-event-source` library used in `SSEManager.ts` provides automatic reconnection on network loss. When network is restored:
1. EventSource API automatically reconnects (built-in browser feature)
2. `SSEManager` detects reconnection in `onopen` handler
3. State resync callback triggers REST API call to `/api/upload/sessions/{sessionId}` (AC5 from Story 2.8)
4. UI updates with latest server state
5. SSE stream continues delivering real-time updates

**Network Detection Integration**: This story adds network-aware pause/resume to the existing SSE real-time update system:
- Network loss → Pause uploads + SSE connection drops
- Network restored → Resume uploads + SSE auto-reconnects + State sync

### Network Detection Hook

```typescript
// useNetworkDetection.ts
import { useEffect, useState } from 'react';
import NetInfo from '@react-native-community/netinfo';

export interface NetworkState {
  isConnected: boolean;
  isInternetReachable: boolean;
  type: string | null;
}

export function useNetworkDetection() {
  const [networkState, setNetworkState] = useState<NetworkState>({
    isConnected: true,
    isInternetReachable: true,
    type: null,
  });

  useEffect(() => {
    // Initial check
    NetInfo.fetch().then((state) => {
      setNetworkState({
        isConnected: state.isConnected ?? false,
        isInternetReachable: state.isInternetReachable ?? false,
        type: state.type,
      });
    });

    // Subscribe to network state changes
    const unsubscribe = NetInfo.addEventListener((state) => {
      const newState = {
        isConnected: state.isConnected ?? false,
        isInternetReachable: state.isInternetReachable ?? false,
        type: state.type,
      };

      setNetworkState(newState);

      // Log for debugging
      console.log('Network state changed:', newState);
    });

    return unsubscribe;
  }, []);

  return networkState;
}
```

### Web Network Detection

```typescript
// useNetworkDetectionWeb.ts (for web platform)
import { useEffect, useState } from 'react';

export function useNetworkDetectionWeb() {
  const [isConnected, setIsConnected] = useState(navigator.onLine);

  useEffect(() => {
    const handleOnline = () => {
      console.log('Network: Online');
      setIsConnected(true);
    };

    const handleOffline = () => {
      console.log('Network: Offline');
      setIsConnected(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Periodic connectivity check (ping server)
    const interval = setInterval(async () => {
      try {
        await fetch('/api/health', { method: 'HEAD', cache: 'no-cache' });
        if (!isConnected) {
          setIsConnected(true);
        }
      } catch {
        if (isConnected) {
          setIsConnected(false);
        }
      }
    }, 10000); // Check every 10 seconds

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      clearInterval(interval);
    };
  }, [isConnected]);

  return { isConnected, isInternetReachable: isConnected, type: 'wifi' };
}
```

### Upload Manager with Network Awareness

```typescript
// UploadManager.ts (enhanced with network awareness)
export class UploadManager {
  private queue: UploadTask[] = [];
  private activeUploads: Map<string, UploadTask> = new Map();
  private pausedUploads: Map<string, UploadTask> = new Map();
  private isPaused = false;

  setNetworkConnected(isConnected: boolean): void {
    if (!isConnected) {
      this.pauseAllUploads();
    } else {
      this.resumeAllUploads();
    }
  }

  private pauseAllUploads(): void {
    if (this.isPaused) return;

    console.log('Pausing all uploads due to network loss');
    this.isPaused = true;

    // Move active uploads to paused state
    this.activeUploads.forEach((task, photoId) => {
      task.pause();
      this.pausedUploads.set(photoId, task);
    });

    this.activeUploads.clear();

    // Emit event
    this.emit('uploadsPaused', { reason: 'network_loss' });
  }

  private resumeAllUploads(): void {
    if (!this.isPaused) return;

    console.log('Resuming uploads - network restored');
    this.isPaused = false;

    // Move paused uploads back to active
    this.pausedUploads.forEach((task, photoId) => {
      if (this.activeUploads.size < this.maxConcurrentUploads) {
        task.resume();
        this.activeUploads.set(photoId, task);
      } else {
        this.queue.unshift(task); // Add to front of queue
      }
    });

    this.pausedUploads.clear();

    // Process queue
    this.processQueue();

    // Emit event
    this.emit('uploadsResumed', {});
  }
}

class UploadTask {
  private abortController: AbortController | null = null;
  private isPaused = false;

  pause(): void {
    this.isPaused = true;
    this.abortController?.abort();
    this.abortController = null;
  }

  resume(): void {
    this.isPaused = false;
    this.abortController = new AbortController();
    // Restart upload from current progress
  }
}
```

### Connection Banner Component

```typescript
// ConnectionBanner.tsx
import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

interface ConnectionBannerProps {
  status: 'connected' | 'disconnected' | 'reconnecting';
}

export const ConnectionBanner: React.FC<ConnectionBannerProps> = ({ status }) => {
  if (status === 'connected') return null;

  const bannerConfig = {
    disconnected: {
      text: 'Connection lost. Uploads paused.',
      backgroundColor: '#f59e0b',
      icon: '⚠️',
    },
    reconnecting: {
      text: 'Connection restored. Resuming uploads.',
      backgroundColor: '#10b981',
      icon: '✓',
    },
  };

  const config = bannerConfig[status];

  return (
    <View style={[styles.banner, { backgroundColor: config.backgroundColor }]}>
      <Text style={styles.icon}>{config.icon}</Text>
      <Text style={styles.text}>{config.text}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 1000,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  icon: {
    fontSize: 18,
    marginRight: 8,
  },
  text: {
    color: 'white',
    fontSize: 14,
    fontWeight: '600',
  },
});
```

### Integration in Upload Dashboard with SSE

```typescript
// UploadDashboard.tsx
import { useSSE } from '../hooks/useSSE';
import { useNetworkDetection } from '../hooks/useNetworkDetection';

export const UploadDashboard: React.FC<UploadDashboardProps> = ({
  sessionId,
  photos,
}) => {
  const { authToken } = useAuth();
  const networkState = useNetworkDetection();
  const uploadManagerRef = useRef(new UploadManager(apiClient));
  const [connectionStatus, setConnectionStatus] = useState<
    'connected' | 'disconnected' | 'reconnecting'
  >('connected');

  // SSE connection for real-time progress (from Story 2.8)
  const sseUrl = `${process.env.REACT_APP_API_URL}/api/upload/sessions/${sessionId}/stream`;
  const { isConnected: isSSEConnected, connectionStatus: sseStatus, subscribe, reconnect } = useSSE({
    url: sseUrl,
    authToken,
    enabled: !!sessionId && networkState.isConnected,
  });

  // Handle network state changes
  useEffect(() => {
    if (!networkState.isConnected || !networkState.isInternetReachable) {
      // Network lost
      setConnectionStatus('disconnected');
      uploadManagerRef.current.setNetworkConnected(false);
    } else {
      // Network restored
      if (connectionStatus === 'disconnected') {
        setConnectionStatus('reconnecting');
        uploadManagerRef.current.setNetworkConnected(true);

        // SSE will automatically reconnect via EventSource API (AC5)
        // useSSE hook handles reconnection and state sync

        // Auto-hide "reconnecting" banner after 3 seconds
        setTimeout(() => {
          setConnectionStatus('connected');
        }, 3000);
      }
    }
  }, [networkState.isConnected, networkState.isInternetReachable]);

  // Subscribe to SSE messages for real-time progress updates
  useEffect(() => {
    if (!sessionId) return;

    subscribe((message) => {
      // Handle real-time upload progress messages
      console.log('[UploadDashboard] SSE message received:', message);
    });
  }, [sessionId, subscribe]);

  return (
    <View style={styles.container}>
      <ConnectionBanner status={connectionStatus} />
      <PhotoGrid photos={photos} />
    </View>
  );
};
```

### Health Check Endpoint

```java
// HealthController.java
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void health() {
        // Simple health check endpoint
        // Returns 204 No Content if server is reachable
    }
}
```

---

## Prerequisites
- Story 2.4 (Upload Engine) - MUST BE COMPLETE
- Story 2.6 (SSE Server Setup) - COMPLETE ✅
- Story 2.7 (Real-Time Progress Broadcasting) - COMPLETE ✅
- Story 2.8 (SSE Client Integration) - COMPLETE ✅

---

## Testing Requirements

### Unit Tests
- [ ] useNetworkDetection detects offline state
- [ ] useNetworkDetection detects online state
- [ ] UploadManager pauses uploads when network lost
- [ ] UploadManager resumes uploads when network restored
- [ ] ConnectionBanner renders correct message for each state

### Integration Tests
- [ ] Simulate network loss, verify uploads pause
- [ ] Simulate network restore, verify uploads resume
- [ ] Verify banner appears within 5 seconds of network loss
- [ ] Verify banner auto-dismisses after reconnection

### E2E Tests
- [ ] Upload 10 photos, disconnect network mid-upload, verify pause
- [ ] Reconnect network, verify uploads resume from correct position
- [ ] SSE connection reconnects automatically when network restored (AC5)
- [ ] State syncs correctly after reconnection via REST API fallback (AC5)

### Manual Testing
- [ ] Turn off WiFi, verify banner appears
- [ ] Turn on WiFi, verify banner changes and uploads resume
- [ ] Disconnect Ethernet cable (web), verify detection
- [ ] Airplane mode on/off (mobile), verify detection

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] useNetworkDetection hook implemented (mobile and web)
- [ ] UploadManager supports pause/resume
- [ ] ConnectionBanner component implemented
- [ ] Network state changes detected within 5 seconds
- [ ] Uploads pause automatically on disconnect
- [ ] Uploads resume automatically on reconnect
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] E2E tests passing
- [ ] Manual testing on mobile and web
- [ ] Code reviewed and approved

---

## Notes
- @react-native-community/netinfo provides reliable network detection on mobile
- Web uses navigator.onLine + periodic health checks (more reliable than onLine alone)
- Pausing uploads prevents "network error" failures (cleaner UX)
- Auto-resume reduces user friction (no manual intervention needed)
- Health check endpoint (/api/health) verifies actual internet connectivity, not just WiFi connection
- **SSE Automatic Reconnection (AC5)**: Built into EventSource API and `@microsoft/fetch-event-source` library - no custom reconnection logic needed in this story
- **State Sync (AC5)**: Already implemented in Story 2.8 via `onStateResync` callback in `useSSE` hook
- **Integration Point**: This story adds network detection layer to coordinate upload pausing with existing SSE reconnection behavior

---

**Status Log:**
- 2025-11-11: Story created (Draft)
- 2025-11-11: Updated to "Ready for Development" - Changed AC5 from "WebSocket Reconnection" to "SSE Reconnection" for compatibility with Stories 2.6-2.8 SSE architecture. Updated technical notes to reference useSSE hook, SSEManager, and existing state sync implementation. Added SSE architecture compatibility section explaining integration with EventSource automatic reconnection and Story 2.8 state resync callback. Updated prerequisites to include Stories 2.6, 2.7, 2.8 (all complete). Updated integration example to use useSSE hook from Story 2.8. (SM Bob)
