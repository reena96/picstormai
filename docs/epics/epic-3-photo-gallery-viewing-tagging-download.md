# Epic 3: Photo Gallery, Viewing, Tagging & Download

**Goal**: Users can browse, view, organize, and download photos

**Duration**: 3 weeks
**Dependencies**: Epic 0, Epic 1, Epic 2 completed
**Scope**: Photo gallery with infinite scroll, lightbox viewer, tagging, download

---

## Story 3.1: Photo Gallery UI with Infinite Scroll

**As a** user
**I want to** view my uploaded photos in a responsive grid
**So that** I can browse my photo collection

### Acceptance Criteria

**Given** I navigate to Gallery tab
**When** I view the screen
**Then** I see photos in 3-column grid (mobile) or 4-column (tablet/desktop)

**Given** I scroll to bottom of gallery
**When** more photos are available
**Then** next page loads automatically (infinite scroll)

**Sorting options:**
- Upload date (newest first) - default
- Upload date (oldest first)
- File size (largest first)
- File name (A-Z)

**Prerequisites:** Story 0.5 (Design System), Story 2.5 (Upload completed)

**Technical Notes:**
- PhotoGrid organism component
- FlatList with onEndReached for pagination
- Load 30 photos per page
- GET /api/photos?page=0&size=30&sort=createdAt,desc
- Show loading spinner while fetching
- Empty state: "No photos yet. Start uploading!"
- Thumbnail URLs from CloudFront CDN

**Testing:**
- E2E test: Gallery loads with photos
- Test: Infinite scroll loads next page
- Test: Empty state shows when no photos
- Test: Sorting changes photo order
- Performance test: Smooth scroll with 1000 photos

---

## Story 3.2: Photo Viewing - Lightbox

**As a** user
**I want to** view individual photos in fullscreen lightbox
**So that** I can see details and navigate between photos

### Acceptance Criteria

**Given** I tap a photo in gallery
**When** lightbox opens
**Then** I see:
- Photo in fullscreen
- Navigation arrows (previous/next)
- Close button (X)
- Zoom controls
- Photo metadata (filename, size, upload date)

**Given** I swipe left/right (mobile) OR press arrow keys (web)
**Then** I navigate to previous/next photo

**Given** I pinch-to-zoom (mobile) OR scroll wheel (web)
**Then** photo zooms in/out smoothly

**Prerequisites:** Story 3.1 (Gallery)

**Technical Notes:**
- Lightbox organism component
- Full-resolution image from CloudFront CDN
- React Native: react-native-image-zoom-viewer
- Web: Custom zoom component with CSS transforms
- Keyboard navigation: Arrow keys, Escape to close
- Prevent body scroll when lightbox open (web)
- Preload adjacent photos for smooth navigation

**Testing:**
- E2E test: Open lightbox, navigate photos
- Test: Zoom in/out works
- Test: Keyboard navigation works (web)
- Test: Swipe navigation works (mobile)
- Accessibility test: Focus trap in lightbox

---

## Story 3.3: Photo Tagging UI

**As a** user
**I want to** add tags to photos for organization
**So that** I can find photos by category later

### Acceptance Criteria

**Given** I open photo in lightbox OR select photo in gallery
**When** I click "Add Tag" button
**Then** I see tag input with autocomplete of existing tags

**Given** I type tag name and press Enter
**When** tag is added
**Then** tag appears as chip on photo

**Given** I click X on tag chip
**Then** tag is removed from photo

**Tag features:**
- Create new tags on-the-fly
- Autocomplete suggests existing tags
- Tag colors assigned automatically
- Max 10 tags per photo

**Prerequisites:** Story 3.2 (Lightbox)

**Technical Notes:**
- TagChip molecule component
- AutocompleteInput for tag creation
- POST /api/photos/{photoId}/tags {tagName}
- DELETE /api/photos/{photoId}/tags/{tagId}
- GET /api/tags - Returns user's existing tags
- Tags stored in tags and photo_tags tables
- Assign random color to new tag (from predefined palette)

**Testing:**
- E2E test: Add tag to photo, verify displayed
- Test: Autocomplete suggests existing tags
- Test: Remove tag from photo
- Test: Max 10 tags enforced
- Test: Tag persists across app restart

---

## Story 3.4: Tag Filter & Search

**As a** user
**I want to** filter photos by tag
**So that** I can find specific photos quickly

### Acceptance Criteria

**Given** I tap "Filter" button in gallery
**When** I select tags from list
**Then** gallery shows only photos with selected tags

**Given** I select multiple tags
**Then** gallery shows photos matching ANY selected tag (OR logic)

**Given** I clear filter
**Then** all photos are displayed again

**Tag filter UI:**
- Horizontal scrollable list of tag chips
- Active filter chips highlighted
- Show photo count for each tag

**Prerequisites:** Story 3.3 (Tagging)

**Technical Notes:**
- GET /api/photos?tags=vacation,family&page=0&size=30
- Backend: SQL query with JOIN on photo_tags
- Optimize with database index on (photo_id, tag_id)
- Show loading state while filtering
- Maintain filter state in URL query params (web) for shareable links

**Testing:**
- E2E test: Filter by tag, verify correct photos shown
- Test: Multiple tag filter (OR logic)
- Test: Clear filter shows all photos
- Performance test: Filter 10K photos by tag <500ms
- Test: URL reflects filter state (web)

---

## Story 3.5: Photo Download (Individual)

**As a** user
**I want to** download individual photos to my device
**So that** I can save photos locally or share them

### Acceptance Criteria

**Given** I view photo in lightbox
**When** I click "Download" button
**Then** photo downloads to default download location

**Mobile:**
- Photo saves to device gallery
- "Photo saved to gallery" toast notification
- Requires photo library permission

**Web:**
- Browser download prompt
- Filename: original photo name

**Prerequisites:** Story 3.2 (Lightbox)

**Technical Notes:**
- Download button in lightbox header
- Generate CloudFront signed URL for download
- Web: Trigger download with anchor tag + download attribute
- Mobile: Use react-native-fs to save to gallery
- Request WRITE_EXTERNAL_STORAGE permission (Android)
- Request photo library access (iOS)
- Track download analytics

**Testing:**
- E2E test: Download photo, verify in Downloads folder
- Test: Mobile saves to gallery with correct filename
- Test: Permission prompt appears on first download (mobile)
- Test: Download fails gracefully without permission

---

## Story 3.6: Batch Photo Download (ZIP)

**As a** user
**I want to** download multiple selected photos as ZIP
**So that** I can save or share many photos at once

### Acceptance Criteria

**Given** I long-press photo in gallery (mobile) OR click checkbox (web)
**When** I enter selection mode
**Then** I can select multiple photos with checkmarks

**Given** I select 10 photos
**When** I click "Download Selected"
**Then** ZIP file downloads containing all 10 photos

**ZIP filename:** `photos-2025-11-09-10-items.zip`

**Limits:**
- Max 100 photos per ZIP
- Show progress bar for large ZIPs

**Prerequisites:** Story 3.5 (Individual Download)

**Technical Notes:**
- POST /api/photos/download {photoIds: [...]}
- Backend: Stream ZIP using Java ZipOutputStream
- Don't load all files in memory - stream each file
- Return Content-Type: application/zip
- Client: Show download progress
- Selection mode: Toggle checkboxes on each PhotoCard

**Testing:**
- E2E test: Select 10 photos, download ZIP, verify contents
- Test: ZIP filename includes date and count
- Test: Max 100 photos enforced
- Performance test: 50-photo ZIP downloads in <30 seconds
- Test: Progress bar updates during download

---

## Story 3.7: Gallery Integration Tests

**As a** QA engineer
**I want to** validate complete gallery and tagging flows
**So that** critical user journeys are tested end-to-end

### Acceptance Criteria

**Given** gallery system is complete
**When** I run integration tests
**Then** all gallery scenarios are validated

**Test Coverage:**
1. **Gallery Loading**: Fetch photos with pagination
2. **Lightbox Navigation**: Open, navigate, close
3. **Tagging**: Add tag, filter by tag, remove tag
4. **Tag Query Performance**: Filter 10K photos by tag <500ms
5. **Download**: Individual download, batch ZIP download
6. **Selection Mode**: Select photos, download as ZIP
7. **Empty State**: Gallery with no photos shows empty state

**Prerequisites:** Stories 3.1-3.6 completed

**Technical Notes:**
- Testcontainers for database with seed data
- Generate 1000 test photos for performance testing
- Mock CloudFront URLs
- Verify database queries use indexes
- E2E tests with real browser (Playwright or Cypress)

**Testing:**
- All integration tests pass
- Performance benchmark: Tag filter <500ms
- E2E test: Complete user journey (upload → tag → filter → download)
- Load test: 100 concurrent users browsing gallery

---
