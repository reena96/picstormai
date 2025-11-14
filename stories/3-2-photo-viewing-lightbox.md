# Story 3.2: Photo Viewing - Lightbox

**Epic**: Epic 3 - Photo Gallery, Viewing, Tagging & Download
**Status**: Ready for Review
**Priority**: P0 (Critical)
**Estimated Effort**: 8-10 hours
**Dependencies**: Story 3.1 (Photo Gallery UI) - DONE
**Prerequisites**: PhotoGrid with onPhotoPress handler exists
**Completed**: 2025-11-12

---

## User Story

**As a** user
**I want to** view individual photos in fullscreen lightbox
**So that** I can see details and navigate between photos

---

## Acceptance Criteria

### AC1: Fullscreen Lightbox Display
**Given** I tap a photo in gallery
**When** lightbox opens
**Then** I see:
- Photo displayed in fullscreen mode
- Full-resolution image (not thumbnail)
- Dark background overlay (90% opacity black)
- Photo centered and scaled to fit screen
- UI controls overlaid on photo

### AC2: Navigation Controls
**Given** lightbox is open
**When** I view the controls
**Then** I see:
- Close button (X) in top-right corner
- Previous arrow button on left side (if not first photo)
- Next arrow button on right side (if not last photo)
- All buttons clearly visible with sufficient contrast

### AC3: Keyboard Navigation (Web)
**Given** lightbox is open on web
**When** I press keyboard keys
**Then** navigation works:
- **Left Arrow Key**: Navigate to previous photo
- **Right Arrow Key**: Navigate to next photo
- **Escape Key**: Close lightbox
- **Tab**: Focus trap keeps focus within lightbox
- Keyboard shortcuts work from any focused element

### AC4: Swipe Navigation (Mobile)
**Given** lightbox is open on mobile
**When** I swipe left or right
**Then** I navigate to previous/next photo with smooth animation
**And** swipe gesture feels natural and responsive
**And** bounce animation when reaching first/last photo

### AC5: Photo Metadata Display
**Given** lightbox is open
**When** I view the photo
**Then** I see metadata displayed at bottom:
- Filename
- Upload date (formatted: "Jan 15, 2025")
- File size (formatted: "2.4 MB")
- Metadata displayed on dark semi-transparent bar

### AC6: Zoom Controls
**Given** lightbox is open
**When** I interact with the photo
**Then** zoom functionality works:
- **Web**: Scroll wheel zooms in/out (Ctrl + scroll or pinch trackpad)
- **Mobile**: Pinch gesture zooms in/out
- **Web**: Click zoom in/out buttons (+ and -)
- Zoom range: 50% to 400% of original size
- Smooth zoom animation
- Pan to view zoomed areas (drag while zoomed)
- Double-tap/double-click to toggle zoom (100% ↔ 200%)

### AC7: Image Loading State
**Given** lightbox is opening
**When** full-resolution image is loading
**Then** I see:
- Spinner/loading indicator centered
- Thumbnail image as placeholder (blur up technique)
- Smooth transition from thumbnail to full image
- No jarring layout shifts

### AC8: Body Scroll Lock (Web)
**Given** lightbox is open on web
**When** I scroll with mouse wheel
**Then** page body does not scroll
**And** scroll events are consumed by lightbox zoom
**And** body scroll is restored when lightbox closes

---

## Technical Notes

### What Exists Already

**Components**:
- ✅ `/Users/reena/gauntletai/picstormai/frontend/src/components/organisms/Lightbox.tsx` (basic scaffold)
  - Basic Modal with fullscreen overlay
  - Close button (X)
  - Previous/Next buttons with conditional rendering
  - Image display with contain resizeMode
  - Filename display in info bar
  - **Missing**: Keyboard nav, swipe gestures, zoom, metadata display, loading states

**Frontend Infrastructure**:
- ✅ GalleryScreen.tsx with onPhotoPress handler (logs to console currently)
- ✅ PhotoCard with onPress prop
- ✅ PhotoGrid with photo array and index tracking
- ✅ galleryService.ts with getPhotos API

**Backend**:
- ✅ Photo.java with storageUrl field (full-resolution CDN URL)
- ✅ PhotoDTO with all metadata fields

### What Needs Implementation

**1. Enhance Lightbox Component** (`/Users/reena/gauntletai/picstormai/frontend/src/components/organisms/Lightbox.tsx`)
- **Props Interface**:
  ```typescript
  interface LightboxProps {
    visible: boolean;
    photos: Photo[];              // Full photo array
    initialIndex: number;         // Starting photo index
    onClose: () => void;
    onPhotoChange?: (index: number) => void;
    testID?: string;
  }
  ```

- **State Management**:
  ```typescript
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [imageLoading, setImageLoading] = useState(true);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [panOffset, setPanOffset] = useState({ x: 0, y: 0 });
  ```

- **Keyboard Navigation** (Web):
  ```typescript
  useEffect(() => {
    if (!visible || Platform.OS !== 'web') return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowLeft') handlePrevious();
      if (e.key === 'ArrowRight') handleNext();
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [visible, currentIndex, onClose]);
  ```

- **Swipe Gestures** (Mobile):
  ```typescript
  // Use PanResponder for swipe detection
  import { PanResponder } from 'react-native';

  const panResponder = PanResponder.create({
    onMoveShouldSetPanResponder: (_, gestureState) =>
      Math.abs(gestureState.dx) > 10,
    onPanResponderRelease: (_, gestureState) => {
      if (gestureState.dx > 50) handlePrevious();
      if (gestureState.dx < -50) handleNext();
    },
  });
  ```

- **Zoom Functionality**:
  ```typescript
  // For Web: Use CSS transforms with wheel event
  const handleWheel = (e: WheelEvent) => {
    if (e.ctrlKey || e.metaKey) {
      e.preventDefault();
      const delta = e.deltaY > 0 ? 0.9 : 1.1;
      setZoomLevel((prev) => Math.min(4, Math.max(0.5, prev * delta)));
    }
  };

  // For Mobile: Use Pinch gesture (react-native-gesture-handler)
  import { PinchGestureHandler } from 'react-native-gesture-handler';
  ```

- **Metadata Display**:
  ```typescript
  const currentPhoto = photos[currentIndex];

  <View style={styles.metadataBar}>
    <Text style={styles.filename}>{currentPhoto.filename}</Text>
    <Text style={styles.metadata}>
      {formatDate(currentPhoto.uploadDate)} • {formatFileSize(currentPhoto.fileSize)}
    </Text>
  </View>
  ```

- **Loading State**:
  ```typescript
  <Image
    source={{ uri: currentPhoto.thumbnailUrl }}
    style={styles.thumbnailImage}
    blurRadius={5}
  />
  <Image
    source={{ uri: currentPhoto.storageUrl }}
    style={styles.fullImage}
    onLoadStart={() => setImageLoading(true)}
    onLoadEnd={() => setImageLoading(false)}
  />
  {imageLoading && <Spinner size="large" color="#FFFFFF" />}
  ```

- **Body Scroll Lock** (Web):
  ```typescript
  useEffect(() => {
    if (!visible || Platform.OS !== 'web') return;

    // Lock body scroll
    document.body.style.overflow = 'hidden';

    return () => {
      // Restore body scroll
      document.body.style.overflow = '';
    };
  }, [visible]);
  ```

**2. Update GalleryScreen** (`/Users/reena/gauntletai/picstormai/frontend/src/screens/GalleryScreen.tsx`)
- Replace console.log in handlePhotoPress with lightbox state management:
  ```typescript
  const [lightboxVisible, setLightboxVisible] = useState(false);
  const [lightboxIndex, setLightboxIndex] = useState(0);

  const handlePhotoPress = (index: number) => {
    setLightboxIndex(index);
    setLightboxVisible(true);
  };

  const handleCloseLightbox = () => {
    setLightboxVisible(false);
  };

  // In JSX:
  <Lightbox
    visible={lightboxVisible}
    photos={photos}
    initialIndex={lightboxIndex}
    onClose={handleCloseLightbox}
  />
  ```

**3. Optional: Install Gesture Handler** (if not already installed)
```bash
npm install react-native-gesture-handler
```

**Note**: Check if already in package.json from previous stories

**4. Create ZoomableImage Component** (optional abstraction)
File: `/Users/reena/gauntletai/picstormai/frontend/src/components/molecules/ZoomableImage.tsx`
- Encapsulates zoom logic
- Handles pinch gestures and wheel events
- Provides pan functionality when zoomed
- Can be reused in future stories

---

## Architecture Notes

### Cross-Platform Zoom Implementation

**Web Approach**:
```typescript
// Use CSS transforms and wheel event
<div
  onWheel={handleWheel}
  style={{
    transform: `scale(${zoomLevel}) translate(${panOffset.x}px, ${panOffset.y}px)`,
    transformOrigin: 'center',
    transition: 'transform 0.2s ease-out',
  }}
>
  <img src={photoUrl} />
</div>
```

**Mobile Approach**:
```typescript
// Use Animated API with PinchGestureHandler
import Animated from 'react-native-reanimated';
import { PinchGestureHandler } from 'react-native-gesture-handler';

const scale = useSharedValue(1);
const savedScale = useSharedValue(1);

const pinchHandler = useAnimatedGestureHandler({
  onActive: (event) => {
    scale.value = savedScale.value * event.scale;
  },
  onEnd: () => {
    savedScale.value = scale.value;
  },
});

<PinchGestureHandler onGestureEvent={pinchHandler}>
  <Animated.Image
    source={{ uri: photoUrl }}
    style={{ transform: [{ scale }] }}
  />
</PinchGestureHandler>
```

### Navigation State Management

**Approach**: Keep navigation state in Lightbox component
- Props: `photos` (array), `initialIndex` (number)
- State: `currentIndex` (managed internally)
- Callback: `onPhotoChange(index)` for external tracking (optional)

**Why**: Simplifies GalleryScreen integration, centralizes lightbox logic

### Performance Considerations

**Image Preloading**:
```typescript
// Preload adjacent photos for smooth navigation
useEffect(() => {
  if (currentIndex > 0) {
    Image.prefetch(photos[currentIndex - 1].storageUrl);
  }
  if (currentIndex < photos.length - 1) {
    Image.prefetch(photos[currentIndex + 1].storageUrl);
  }
}, [currentIndex, photos]);
```

**Memory Management**:
- Only keep 3 images in memory (current, previous, next)
- Unload images when navigating away
- Use `resizeMode="contain"` to prevent large memory footprint

---

## Testing Requirements

### Unit Tests (Frontend)

**File**: `/Users/reena/gauntletai/picstormai/frontend/src/components/organisms/Lightbox.test.tsx`

Test cases:
- [ ] Lightbox renders when visible=true
- [ ] Lightbox does not render when visible=false
- [ ] Close button calls onClose callback
- [ ] Previous button navigates to previous photo
- [ ] Next button navigates to next photo
- [ ] Previous button hidden when on first photo
- [ ] Next button hidden when on last photo
- [ ] Displays photo at initialIndex on mount
- [ ] Displays correct metadata (filename, date, size)
- [ ] Loading state shown while image loading
- [ ] Keyboard navigation works (web)
  - Escape key closes lightbox
  - Left arrow navigates to previous
  - Right arrow navigates to next
- [ ] Body scroll locked when lightbox open (web)
- [ ] Body scroll restored when lightbox closed (web)

**File**: `/Users/reena/gauntletai/picstormai/frontend/src/screens/GalleryScreen.test.tsx` (update)

Additional test cases:
- [ ] Clicking photo opens lightbox at correct index
- [ ] Lightbox closes when onClose called
- [ ] Lightbox receives correct photos array

### Integration Tests

**E2E Test Scenario**:
```typescript
// Test: Open lightbox from gallery
test('opens lightbox when clicking photo', async () => {
  // 1. Login and navigate to Gallery
  // 2. Wait for photos to load
  // 3. Click first photo
  // 4. Verify lightbox visible with correct photo
  // 5. Verify metadata displayed
  // 6. Click next arrow
  // 7. Verify photo changed
  // 8. Press Escape key
  // 9. Verify lightbox closed
});

// Test: Keyboard navigation in lightbox
test('navigates photos with keyboard', async () => {
  // 1. Open lightbox
  // 2. Press Right Arrow key
  // 3. Verify photo changed to next
  // 4. Press Left Arrow key
  // 5. Verify photo changed to previous
  // 6. Press Escape key
  // 7. Verify lightbox closed
});

// Test: Swipe navigation (mobile)
test('navigates photos with swipe gestures', async () => {
  // 1. Open lightbox
  // 2. Swipe left on photo
  // 3. Verify navigated to next photo
  // 4. Swipe right on photo
  // 5. Verify navigated to previous photo
});

// Test: Zoom functionality
test('zooms photo with wheel/pinch', async () => {
  // 1. Open lightbox
  // 2. Ctrl+Scroll (web) or pinch (mobile)
  // 3. Verify photo zoomed in
  // 4. Scroll again
  // 5. Verify photo zoomed out
  // 6. Double-click/tap
  // 7. Verify zoom toggled
});
```

### Manual Testing Checklist

**Web (Browser)**:
- [ ] Click photo in gallery opens lightbox
- [ ] Lightbox displays full-resolution image
- [ ] Close button (X) closes lightbox
- [ ] Previous/Next arrows navigate photos
- [ ] Arrow keys navigate photos
- [ ] Escape key closes lightbox
- [ ] Ctrl+Scroll zooms in/out
- [ ] Double-click toggles zoom
- [ ] Pan dragging works when zoomed
- [ ] Metadata displayed correctly
- [ ] Page body does not scroll when lightbox open
- [ ] Smooth loading transition (thumbnail → full image)
- [ ] First photo: Previous arrow hidden
- [ ] Last photo: Next arrow hidden

**Mobile (iOS/Android)**:
- [ ] Tap photo in gallery opens lightbox
- [ ] Swipe left navigates to next photo
- [ ] Swipe right navigates to previous photo
- [ ] Pinch gesture zooms in/out
- [ ] Double-tap toggles zoom
- [ ] Pan dragging works when zoomed
- [ ] Metadata displayed correctly
- [ ] Loading spinner shows while image loads
- [ ] Smooth animations for navigation
- [ ] Bounce effect at first/last photo

---

## Implementation Steps (Recommended Order)

### Phase 1: Core Lightbox Functionality (3-4 hours)

**Step 1.1**: Enhance Lightbox component props and state
- Update LightboxProps interface
- Add state for currentIndex, imageLoading, zoomLevel
- Import design system components (Text, Spinner, Button)
- Use theme for styling consistency

**Step 1.2**: Implement navigation logic
- Add handlePrevious() and handleNext() functions
- Update currentIndex state
- Conditionally render Previous/Next buttons
- Add boundary checks (first/last photo)

**Step 1.3**: Implement metadata display
- Extract current photo from photos array
- Display filename, upload date, file size
- Format date and file size with helper functions
- Style metadata bar with semi-transparent background

**Step 1.4**: Implement loading state
- Show thumbnail with blur as placeholder
- Show spinner while full image loading
- Use onLoadStart and onLoadEnd callbacks
- Smooth transition from thumbnail to full image

**Step 1.5**: Update GalleryScreen integration
- Add lightbox state (visible, index)
- Update handlePhotoPress to open lightbox
- Add handleCloseLightbox callback
- Pass photos array to Lightbox component
- Test basic open/close/navigate functionality

### Phase 2: Keyboard Navigation (1-2 hours)

**Step 2.1**: Add keyboard event listener (web)
- useEffect hook for adding/removing listener
- Handle ArrowLeft, ArrowRight, Escape keys
- Prevent default behavior for arrow keys
- Test with Platform.OS === 'web' guard

**Step 2.2**: Implement focus trap
- Ensure Tab key doesn't leave lightbox
- Focus on Close button when lightbox opens
- Use accessibility best practices
- Test keyboard navigation flow

**Step 2.3**: Body scroll lock (web)
- Set document.body.style.overflow = 'hidden' on mount
- Restore overflow on unmount
- Test page body does not scroll

### Phase 3: Swipe Gestures (2-3 hours)

**Step 3.1**: Implement PanResponder for swipe detection
- Create panResponder with onMoveShouldSetPanResponder
- Detect horizontal swipe (dx > 50 or dx < -50)
- Call handlePrevious/handleNext on swipe
- Add threshold to prevent accidental swipes

**Step 3.2**: Add swipe animations
- Use Animated API for smooth transitions
- Slide animation when changing photos
- Bounce animation at boundaries
- Test on mobile simulator/device

### Phase 4: Zoom Functionality (3-4 hours)

**Step 4.1**: Implement web zoom with wheel event
- Add wheel event listener with e.ctrlKey check
- Calculate zoom delta (0.9 or 1.1)
- Clamp zoom level (0.5 to 4)
- Apply CSS transform to image container

**Step 4.2**: Implement mobile zoom with pinch gesture
- Install react-native-gesture-handler (if needed)
- Use PinchGestureHandler component
- Use Animated.Value for smooth zoom
- Clamp zoom level (0.5 to 4)

**Step 4.3**: Add zoom controls UI
- Add + and - buttons for manual zoom
- Position buttons in top-left or bottom-right
- Style with semi-transparent background
- Test zoom in/out with buttons

**Step 4.4**: Implement pan functionality when zoomed
- Allow dragging image when zoomLevel > 1
- Use PanResponder or GestureHandler
- Constrain pan to image bounds
- Reset pan when zoom level = 1

**Step 4.5**: Add double-tap/click zoom toggle
- Detect double-tap/click events
- Toggle between 100% and 200% zoom
- Smooth transition with animation
- Test on both web and mobile

### Phase 5: Testing & Polish (1-2 hours)

**Step 5.1**: Write unit tests
- Create Lightbox.test.tsx
- Test all ACs with comprehensive coverage
- Mock images and gestures
- Use @testing-library/react (not react-native)

**Step 5.2**: Update GalleryScreen tests
- Add tests for lightbox integration
- Test photo press opens lightbox
- Test lightbox receives correct props

**Step 5.3**: Manual testing
- Test on web browser (all features)
- Test on iOS simulator (swipe, pinch)
- Test on Android emulator (swipe, pinch)
- Test edge cases (first photo, last photo, single photo)

**Step 5.4**: Performance optimization
- Add image preloading for adjacent photos
- Optimize re-renders with useCallback
- Test with 1000 photos in gallery
- Verify smooth navigation and zoom

---

## Definition of Done

### Functional Requirements
- [ ] Lightbox opens when clicking photo in gallery
- [ ] Fullscreen display with dark background overlay
- [ ] Close button (X) closes lightbox
- [ ] Previous/Next arrow buttons navigate photos
- [ ] Keyboard navigation works (Arrow keys, Escape) on web
- [ ] Swipe gestures work (left/right) on mobile
- [ ] Photo metadata displayed (filename, date, size)
- [ ] Zoom controls work (scroll wheel, pinch, buttons)
- [ ] Zoom range: 50% to 400%
- [ ] Pan dragging works when zoomed
- [ ] Double-tap/click toggles zoom
- [ ] Loading state with thumbnail blur-up
- [ ] Body scroll locked when lightbox open (web)
- [ ] Smooth animations for all interactions
- [ ] Previous/Next buttons hidden at boundaries

### Code Quality
- [ ] All unit tests passing (Lightbox component)
- [ ] All integration tests passing (GalleryScreen)
- [ ] E2E test scenarios passing
- [ ] Code reviewed and approved
- [ ] Design system components used consistently
- [ ] No console errors or warnings
- [ ] TypeScript types properly defined
- [ ] Proper error handling for failed image loads
- [ ] Accessibility: Focus trap, keyboard nav, ARIA labels

### Cross-Platform Verification
- [ ] Verified working in web browser (http://localhost:8081)
- [ ] Verified working on iOS simulator (swipe, pinch)
- [ ] Verified working on Android emulator (swipe, pinch)
- [ ] Keyboard navigation works on web
- [ ] Touch gestures work on mobile
- [ ] Zoom functionality works on all platforms

### Performance
- [ ] Smooth photo navigation (<200ms transition)
- [ ] Zoom/pan gestures feel responsive
- [ ] No jank during swipe animations
- [ ] Image preloading for adjacent photos works
- [ ] No memory leaks when navigating many photos
- [ ] Lightbox closes cleanly without errors

### Documentation
- [ ] Code comments for complex logic (zoom, gestures)
- [ ] Component props documented with JSDoc
- [ ] Testing patterns documented
- [ ] Cross-platform implementation notes

---

## File Paths Reference

### Files to Create
- `/Users/reena/gauntletai/picstormai/frontend/src/components/organisms/Lightbox.test.tsx`
- `/Users/reena/gauntletai/picstormai/frontend/src/components/molecules/ZoomableImage.tsx` (optional)
- `/Users/reena/gauntletai/picstormai/frontend/src/components/molecules/ZoomableImage.test.tsx` (optional)

### Files to Modify
- `/Users/reena/gauntletai/picstormai/frontend/src/components/organisms/Lightbox.tsx` (MAJOR REWRITE)
- `/Users/reena/gauntletai/picstormai/frontend/src/screens/GalleryScreen.tsx` (Add lightbox state and integration)
- `/Users/reena/gauntletai/picstormai/frontend/src/screens/GalleryScreen.test.tsx` (Add lightbox tests)

### Files That Exist (Ready to Use)
- `/Users/reena/gauntletai/picstormai/frontend/src/components/molecules/PhotoCard.tsx` ✅
- `/Users/reena/gauntletai/picstormai/frontend/src/components/organisms/PhotoGrid.tsx` ✅
- `/Users/reena/gauntletai/picstormai/frontend/src/services/galleryService.ts` ✅
- `/Users/reena/gauntletai/picstormai/frontend/src/hooks/useTheme.ts` ✅

---

## Verification Steps (Manual Testing)

### Web Browser Testing (Primary)
1. **Setup**:
   - Start backend: `cd backend && ./gradlew bootRun`
   - Start frontend: `cd frontend && npm start`
   - Open http://localhost:8081
   - Login with test account

2. **Basic Lightbox**:
   - Navigate to Gallery tab
   - Click any photo → Verify lightbox opens fullscreen
   - Verify photo centered with dark background
   - Verify metadata displayed at bottom (filename, date, size)
   - Click Close button (X) → Verify lightbox closes

3. **Navigation**:
   - Open lightbox on middle photo
   - Click Next arrow → Verify navigates to next photo
   - Click Previous arrow → Verify navigates to previous photo
   - Navigate to first photo → Verify Previous arrow hidden
   - Navigate to last photo → Verify Next arrow hidden

4. **Keyboard Navigation**:
   - Open lightbox
   - Press Right Arrow key → Verify navigates next
   - Press Left Arrow key → Verify navigates previous
   - Press Escape key → Verify lightbox closes
   - Open lightbox and press Tab → Verify focus stays in lightbox

5. **Zoom (Web)**:
   - Open lightbox
   - Ctrl+Scroll up → Verify photo zooms in
   - Ctrl+Scroll down → Verify photo zooms out
   - Double-click photo → Verify zoom toggles to 200%
   - Double-click again → Verify zoom returns to 100%
   - Click + button → Verify zooms in
   - Click - button → Verify zooms out
   - Zoom to 200% → Drag photo → Verify pans correctly

6. **Body Scroll Lock**:
   - Open lightbox
   - Scroll with mouse wheel → Verify page body doesn't scroll
   - Close lightbox
   - Scroll with mouse wheel → Verify page body scrolls normally

7. **Loading State**:
   - Open lightbox on slow connection (or throttle network)
   - Verify thumbnail shown with blur
   - Verify spinner displayed
   - Wait for full image to load
   - Verify smooth transition from thumbnail to full image

### Mobile Testing (iOS Simulator)
1. **Setup**:
   - Run `npm run ios`
   - Login to app

2. **Basic Lightbox**:
   - Navigate to Gallery tab
   - Tap any photo → Verify lightbox opens
   - Tap Close button → Verify lightbox closes

3. **Swipe Navigation**:
   - Open lightbox
   - Swipe left → Verify navigates to next photo
   - Swipe right → Verify navigates to previous photo
   - Try to swipe left on last photo → Verify bounce animation
   - Try to swipe right on first photo → Verify bounce animation

4. **Pinch Zoom**:
   - Open lightbox
   - Pinch out (spread fingers) → Verify photo zooms in
   - Pinch in (close fingers) → Verify photo zooms out
   - Zoom to 200% → Drag photo → Verify pans correctly
   - Double-tap photo → Verify zoom toggles

5. **Loading State**:
   - Open lightbox
   - Verify thumbnail shown first
   - Verify spinner displayed
   - Verify smooth transition to full image

### Mobile Testing (Android Emulator)
Repeat all iOS tests on Android emulator:
- Run `npm run android`
- Test swipe gestures
- Test pinch zoom
- Verify all functionality works identically

---

## Known Issues & Limitations

1. **Zoom on Mobile**: Requires react-native-gesture-handler for smooth pinch gestures
2. **Image Preloading**: May consume bandwidth; consider preloading only on WiFi
3. **Large Images**: Photos >10MB may take time to load; thumbnail blur-up helps UX
4. **Keyboard Shortcuts**: Web-only feature; mobile users must use touch gestures
5. **Focus Trap**: Complex to implement perfectly; may need a11y library like `react-focus-lock`
6. **Swipe Conflicts**: Swipe gestures may conflict with system gestures (iOS back gesture)

---

## Related Stories

**Depends On**:
- Story 3.1: Photo Gallery UI (Complete - DONE)

**Blocks**:
- Story 3.3: Photo Tagging (tags will be displayed in lightbox metadata)
- Story 3.5: Photo Download (download button will be added to lightbox)

**Related**:
- Story 3.4: Tag Filter (lightbox photos will respect active filters)

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Gesture handler not installed | High | Check package.json first; install if needed |
| Zoom implementation complexity | High | Start with simple CSS transform (web) and basic Animated API (mobile) |
| Performance issues with large images | Medium | Use resizeMode="contain" and limit preloading to 2 adjacent photos |
| Keyboard shortcuts conflict with browser | Low | Use e.preventDefault() for handled keys |
| Swipe gestures feel unnatural | Medium | Test with real devices; adjust thresholds and animations |
| Focus trap breaks accessibility | Medium | Use react-focus-lock library or follow WAI-ARIA guidelines |
| Body scroll lock breaks on iOS | Low | Use body-scroll-lock library if needed |

---

## Implementation Guidelines

### Design Principles
1. **Performance First**: Smooth animations are critical for good UX
2. **Accessibility**: Keyboard navigation and focus management are required
3. **Progressive Enhancement**: Basic functionality (open/close/navigate) before advanced (zoom)
4. **Cross-Platform Parity**: Web and mobile should feel equally polished

### Code Quality Standards
- **TypeScript**: Strict typing for all props and state
- **Testing**: Use @testing-library/react for web compatibility
- **Mocking**: Mock complex gesture handlers in tests
- **Design System**: Use theme for all colors, spacing, shadows
- **Comments**: Document gesture thresholds, zoom constraints, animation timings

### Testing Strategy
1. **Unit Tests**: Component logic (navigation, state, callbacks)
2. **Integration Tests**: GalleryScreen → Lightbox flow
3. **E2E Tests**: Real user interactions (click, swipe, zoom)
4. **Manual Tests**: Cross-platform verification on real devices

---

## Success Metrics

### User Experience
- [ ] Lightbox opens in <300ms
- [ ] Photo navigation feels instant (<200ms)
- [ ] Zoom gestures feel smooth (60 FPS)
- [ ] No user confusion on navigation (clear UI)

### Technical Quality
- [ ] 100% test pass rate
- [ ] Zero console errors or warnings
- [ ] TypeScript compile with no errors
- [ ] Accessibility score: A+ (keyboard nav, focus trap)

### Performance
- [ ] Lighthouse Performance Score: >90
- [ ] No jank during swipe animations
- [ ] Image loading optimized (blur-up technique)
- [ ] Memory usage stable (no leaks)

---

**Status Log**:
- 2025-11-12: Story created and marked **Ready for Development**
- Dependencies verified: Story 3.1 (Photo Gallery UI) - DONE
- Prerequisites met: PhotoGrid with onPhotoPress handler exists
- Backend: Photos API fully functional
- Estimated effort: 8-10 hours
- Quality bar: EXCELLENT code, 100% test pass, production ready
- Testing approach: Use @testing-library/react for react-native-web compatibility

---

## Context from Story 3.1

**Session 1 Learnings**:
- Story 3.1 took 4 hours total (implementation + testing + QA)
- Testing pattern: Use `@testing-library/react` (not `react-native`) for react-native-web
- Design system components work well across platforms
- PhotoGrid and PhotoCard provide solid foundation for lightbox
- API integration via galleryService.ts is clean and type-safe

**Components to Leverage**:
- PhotoCard.tsx: Already has metadata formatting functions (formatDate, formatFileSize)
- PhotoGrid.tsx: Provides photos array and index tracking
- galleryService.ts: Photo interface and API integration
- Design system atoms: Text, Spinner, Button, Icon

**Testing Lessons**:
- Mock complex components in tests (Select, PhotoGrid)
- Use @testing-library/react for DOM-based components
- Focus on screen logic, not component implementation details
- Comprehensive mocks make tests more maintainable

---

## Next Steps After This Story

When Story 3.2 is marked Done:
1. Story 3.3: Photo Tagging - Add tags to lightbox metadata display
2. Story 3.5: Photo Download - Add download button to lightbox controls
3. Consider: Share button in lightbox for social media integration

---

**Epic Progress**: Story 3.1 ✅ DONE → Story 3.2 ⏳ READY → Stories 3.3-3.7 🔜 PENDING
