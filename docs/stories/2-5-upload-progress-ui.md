# Story 2.5: Upload Progress UI

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase A - Basic Upload (Weeks 1-2)
**Status**: Draft
**Priority**: High
**Estimated Effort**: 3 days

---

## User Story

**As a** user
**I want to** see real-time progress for each photo and overall batch
**So that** I know how the upload is progressing

---

## Acceptance Criteria

### AC1: Overall Progress Display
**Given** photos are uploading
**When** I view upload dashboard
**Then** I see overall progress bar showing "45/100 photos uploaded - 67%"
**And** estimated time remaining (e.g., "5 minutes remaining")

### AC2: Individual Photo Progress
**Given** photos are uploading
**Then** each photo card shows:
- Thumbnail preview
- Progress bar (0-100%)
- File name
- Current status badge

### AC3: Photo Status States
**Given** photo in upload queue
**Then** photo displays correct state:
- **Queued**: Gray badge, no progress bar, "Waiting..."
- **Uploading**: Blue animated progress bar, percentage (e.g., "47%")
- **Completed**: Green checkmark icon, "Uploaded"
- **Failed**: Red X icon, error message, "Retry" button

### AC4: Progress Bar Animation
**Given** photo is uploading
**When** progress updates
**Then** progress bar animates smoothly (not jumpy)
**And** percentage text updates every 500ms (throttled)

### AC5: ETA Calculation
**Given** 50 photos uploaded, 50 remaining
**When** average upload speed is 2MB/s
**Then** ETA calculates: (remaining bytes) / (average speed)
**And** displays as "About 3 minutes remaining"

---

## Technical Notes

### Component Structure

```typescript
// UploadDashboard.tsx (Organism)
export const UploadDashboard: React.FC<UploadDashboardProps> = ({
  sessionId,
  photos,
  uploadProgress,
}) => {
  const overallProgress = calculateOverallProgress(photos, uploadProgress);
  const eta = calculateETA(photos, uploadProgress);

  return (
    <View style={styles.dashboard}>
      <OverallProgressBar
        uploadedCount={overallProgress.uploaded}
        totalCount={overallProgress.total}
        percent={overallProgress.percent}
        eta={eta}
      />
      <PhotoGrid>
        {photos.map((photo) => (
          <PhotoCard
            key={photo.id}
            photo={photo}
            progress={uploadProgress.get(photo.id)}
          />
        ))}
      </PhotoGrid>
    </View>
  );
};

// OverallProgressBar.tsx (Molecule)
export const OverallProgressBar: React.FC<OverallProgressBarProps> = ({
  uploadedCount,
  totalCount,
  percent,
  eta,
}) => {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>
        {uploadedCount}/{totalCount} photos uploaded
      </Text>
      <ProgressBar percent={percent} color="blue" animated />
      <Text style={styles.percent}>{Math.round(percent)}%</Text>
      {eta && <Text style={styles.eta}>{formatETA(eta)}</Text>}
    </View>
  );
};

// PhotoCard.tsx (Molecule)
export const PhotoCard: React.FC<PhotoCardProps> = ({ photo, progress }) => {
  const status = determineStatus(photo, progress);

  return (
    <View style={styles.card}>
      <Image source={{ uri: photo.thumbnail }} style={styles.thumbnail} />
      <View style={styles.info}>
        <Text style={styles.fileName} numberOfLines={1}>
          {photo.fileName}
        </Text>
        {status === 'uploading' && (
          <>
            <ProgressBar percent={progress?.percent || 0} size="small" />
            <Text style={styles.percent}>{Math.round(progress?.percent || 0)}%</Text>
          </>
        )}
        {status === 'queued' && <StatusBadge type="queued" text="Waiting..." />}
        {status === 'completed' && <StatusBadge type="success" text="Uploaded" icon="check" />}
        {status === 'failed' && (
          <>
            <StatusBadge type="error" text="Failed" icon="x" />
            <Button size="small" onPress={() => retryUpload(photo.id)}>
              Retry
            </Button>
          </>
        )}
      </View>
    </View>
  );
};

// ProgressBar.tsx (Atom)
export const ProgressBar: React.FC<ProgressBarProps> = ({ percent, color, animated, size }) => {
  const animatedWidth = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(animatedWidth, {
      toValue: percent,
      duration: 300,
      useNativeDriver: false,
    }).start();
  }, [percent]);

  return (
    <View style={[styles.track, size === 'small' && styles.trackSmall]}>
      <Animated.View
        style={[
          styles.fill,
          { width: animatedWidth.interpolate({
            inputRange: [0, 100],
            outputRange: ['0%', '100%'],
          }) },
          { backgroundColor: color || '#3b82f6' },
        ]}
      />
    </View>
  );
};
```

### State Management

```typescript
interface UploadDashboardState {
  sessionId: string;
  photos: PhotoSelection[];
  uploadProgress: Map<string, UploadProgress>;
  startTime: number;
  completedPhotos: Set<string>;
  failedPhotos: Map<string, string>; // photoId -> error message
}

interface UploadProgress {
  photoId: string;
  bytesUploaded: number;
  totalBytes: number;
  percent: number;
  timestamp: number;
}

// Calculate overall progress
function calculateOverallProgress(
  photos: PhotoSelection[],
  progressMap: Map<string, UploadProgress>
): { uploaded: number; total: number; percent: number } {
  const uploaded = Array.from(progressMap.values()).filter((p) => p.percent === 100).length;
  const total = photos.length;
  const percent = (uploaded / total) * 100;
  return { uploaded, total, percent };
}

// Calculate ETA
function calculateETA(
  photos: PhotoSelection[],
  progressMap: Map<string, UploadProgress>
): number | null {
  const now = Date.now();
  const progressArray = Array.from(progressMap.values());

  if (progressArray.length === 0) return null;

  // Calculate average upload speed (bytes per second)
  const totalBytesUploaded = progressArray.reduce((sum, p) => sum + p.bytesUploaded, 0);
  const elapsedSeconds = (now - Math.min(...progressArray.map((p) => p.timestamp))) / 1000;
  const averageSpeed = totalBytesUploaded / elapsedSeconds;

  if (averageSpeed === 0) return null;

  // Calculate remaining bytes
  const totalBytes = photos.reduce((sum, p) => sum + p.fileSize, 0);
  const remainingBytes = totalBytes - totalBytesUploaded;

  // ETA in seconds
  return remainingBytes / averageSpeed;
}

function formatETA(seconds: number): string {
  if (seconds < 60) return 'Less than a minute';
  const minutes = Math.round(seconds / 60);
  if (minutes === 1) return 'About 1 minute';
  if (minutes < 60) return `About ${minutes} minutes`;
  const hours = Math.round(minutes / 60);
  return `About ${hours} hour${hours > 1 ? 's' : ''}`;
}

function determineStatus(
  photo: PhotoSelection,
  progress?: UploadProgress
): 'queued' | 'uploading' | 'completed' | 'failed' {
  if (photo.status === 'failed') return 'failed';
  if (!progress) return 'queued';
  if (progress.percent === 100) return 'completed';
  return 'uploading';
}
```

### Throttled Progress Updates

```typescript
// useThrottledProgress.ts
export function useThrottledProgress(
  uploadManager: UploadManager,
  photos: PhotoSelection[],
  throttleMs = 500
): Map<string, UploadProgress> {
  const [progressMap, setProgressMap] = useState<Map<string, UploadProgress>>(new Map());

  useEffect(() => {
    const unsubscribers: (() => void)[] = [];

    photos.forEach((photo) => {
      let lastUpdate = 0;

      const unsubscribe = uploadManager.onProgress(photo.id, (progress) => {
        const now = Date.now();
        if (now - lastUpdate >= throttleMs) {
          setProgressMap((prev) => new Map(prev).set(photo.id, progress));
          lastUpdate = now;
        }
      });

      unsubscribers.push(unsubscribe);
    });

    return () => {
      unsubscribers.forEach((unsub) => unsub());
    };
  }, [uploadManager, photos, throttleMs]);

  return progressMap;
}
```

---

## Prerequisites
- Story 2.4 (Upload Engine) - MUST BE COMPLETE
- Story 0.5 (Design System) - COMPLETE

---

## Testing Requirements

### Unit Tests
- [ ] calculateOverallProgress() with 50/100 photos = 50%
- [ ] calculateETA() with known upload speed returns correct seconds
- [ ] formatETA() formats seconds correctly (e.g., 125s -> "About 2 minutes")
- [ ] determineStatus() returns correct status for each state
- [ ] ProgressBar animates width change smoothly

### Integration Tests
- [ ] Progress updates reflect in UI within 500ms
- [ ] Overall progress bar updates as photos complete
- [ ] ETA displays and updates correctly

### E2E Tests
- [ ] Upload 10 photos, verify progress bars animate
- [ ] Verify individual photo progress displays correctly
- [ ] Verify overall progress aggregates correctly
- [ ] Verify ETA calculation is reasonable (±20% accuracy)

### Accessibility Tests
- [ ] Screen reader announces progress changes
- [ ] Progress percentage announced every 10% change
- [ ] Upload completion announced to screen reader

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] All UI components implemented using Design System
- [ ] Progress updates throttled to 500ms
- [ ] ETA calculation working and reasonable
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] E2E tests passing
- [ ] Accessibility requirements met (WCAG 2.1 AA)
- [ ] Mobile and web tested
- [ ] Code reviewed and approved

---

## Notes
- Throttling progress updates prevents UI jank
- ETA is approximate, should not be treated as exact
- Photo grid should virtualize for large uploads (100 photos)
- Smooth animations improve perceived performance

---

**Status Log:**
- 2025-11-11: Story created (Draft)
