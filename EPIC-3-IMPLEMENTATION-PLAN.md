# Epic 3: Implementation Plan & Next Steps

**Date**: 2025-11-12
**Status**: Ready for Implementation
**Current Completion**: ~5% (scaffolding only)

---

## Critical Finding

After comprehensive review: **Epic 3 is NOT complete**. Only domain models and empty scaffolding exist. No functional UI or integrated backend endpoints.

## What Was Created This Session

1. ✅ **EPIC-3-STATUS-REPORT.md** - Comprehensive analysis of what's done/missing
2. ✅ **galleryService.ts** - Complete photo API service
3. ⏸️ **PhotoCard.tsx** - Started but needs completion

## Immediate Next Steps (Priority Order)

### Phase 1: Story 3.1 - Basic Gallery (14-18 hours)

**Backend**:
1. Verify PhotoController.getPhotos() is actually wired to GetPhotosForUserQueryHandler
   - Current code returns `.collectList()` but API might return empty array
   - Test endpoint: `curl -H "Authorization: Bearer <token>" http://localhost:8080/api/photos`

2. If no photos exist, upload test photos or verify Epic 2 upload created them

**Frontend**:
1. Complete PhotoCard component (molecules/)
2. Create PhotoGrid component (organisms/)
3. Rewrite GalleryScreen.tsx:
   - Import galleryService
   - Use useEffect to fetch photos
   - Display in 3-column grid (mobile) / 4-column (web)
   - Add sorting dropdown
   - Implement FlatList with onEndReached for infinite scroll
   - Add empty state UI
   - Add loading states

**Files to Create/Modify**:
```
frontend/src/components/molecules/PhotoCard.tsx ← Complete
frontend/src/components/organisms/PhotoGrid.tsx ← NEW
frontend/src/screens/GalleryScreen.tsx ← REWRITE
```

**Acceptance Criteria**:
- [ ] Gallery displays actual photos from database
- [ ] Photos show as images (not just filenames)
- [ ] Sorting works (newest/oldest/size/name)
- [ ] Infinite scroll loads next page
- [ ] Empty state shows when no photos
- [ ] Loading spinner during fetch

---

### Phase 2: Story 3.2 - Lightbox (6-8 hours)

**Frontend**:
1. Create Lightbox component:
   - Fullscreen modal overlay
   - Display full-resolution image
   - Previous/Next arrows
   - Close button (X)
   - Photo metadata display
   - Keyboard navigation (Arrow keys, Escape)
   - Swipe gestures (mobile)
   - Zoom controls (pinch/scroll)

2. Create custom hooks:
   - useKeyboardNavigation.ts
   - useZoom.ts

3. Integrate with GalleryScreen:
   - Photo Card onPress opens Lightbox
   - Pass all photos for navigation

**Files to Create**:
```
frontend/src/components/organisms/Lightbox.tsx ← NEW
frontend/src/hooks/useKeyboardNavigation.ts ← NEW
frontend/src/hooks/useZoom.ts ← NEW
```

**Acceptance Criteria**:
- [ ] Tapping photo opens lightbox
- [ ] Arrow keys navigate photos
- [ ] Escape closes lightbox
- [ ] Swipe left/right works (mobile)
- [ ] Zoom in/out works
- [ ] Metadata displayed (filename, size, date)

---

### Phase 3: Story 3.3 - Photo Tagging (10-12 hours)

**Backend**:
1. Create Tag domain model:
   ```java
   backend/src/main/java/com/rapidphoto/domain/tag/Tag.java
   backend/src/main/java/com/rapidphoto/domain/tag/TagRepository.java
   ```

2. Create CQRS commands/queries:
   ```java
   commands/AddTagToPhotoCommand.java + handler
   commands/RemoveTagFromPhotoCommand.java + handler
   queries/GetUserTagsQuery.java + handler
   ```

3. Rewrite TagController.java:
   - Replace mock responses
   - Wire to actual command/query handlers
   - POST /api/photos/{photoId}/tags → AddTagToPhotoCommandHandler
   - DELETE /api/photos/{photoId}/tags/{tagId} → RemoveTagFromPhotoCommandHandler
   - GET /api/tags → GetUserTagsQueryHandler

**Frontend**:
1. Create TagChip component (molecule)
2. Create TagInput component with autocomplete (molecule)
3. Add tagging UI to Lightbox:
   - "Add Tag" button
   - Tag input with autocomplete
   - Display existing tags as chips
   - Remove tag on chip X click

**Files to Create**:
```
backend/src/main/java/com/rapidphoto/domain/tag/Tag.java
backend/src/main/java/com/rapidphoto/domain/tag/TagRepository.java
backend/src/main/java/com/rapidphoto/cqrs/commands/AddTagToPhotoCommand.java
backend/src/main/java/com/rapidphoto/cqrs/commands/handlers/AddTagToPhotoCommandHandler.java
backend/src/main/java/com/rapidphoto/cqrs/commands/RemoveTagFromPhotoCommand.java
backend/src/main/java/com/rapidphoto/cqrs/commands/handlers/RemoveTagFromPhotoCommandHandler.java
backend/src/main/java/com/rapidphoto/cqrs/queries/GetUserTagsQuery.java
backend/src/main/java/com/rapidphoto/cqrs/queries/handlers/GetUserTagsQueryHandler.java
frontend/src/components/molecules/TagChip.tsx
frontend/src/components/molecules/TagInput.tsx
frontend/src/services/tagService.ts
```

**Files to Modify**:
```
backend/src/main/java/com/rapidphoto/api/TagController.java ← REWRITE
frontend/src/components/organisms/Lightbox.tsx ← Add tagging UI
```

**Acceptance Criteria**:
- [ ] Add tag to photo works
- [ ] Autocomplete suggests existing tags
- [ ] Remove tag works
- [ ] Max 10 tags enforced
- [ ] Tags persist across sessions

---

### Phase 4: Story 3.4 - Tag Filter (4-6 hours)

**Frontend**:
1. Create TagFilter component (organism)
2. Add to GalleryScreen:
   - Horizontal scrollable tag chips
   - Tap to toggle filter
   - Show photo count per tag
   - Multi-select (OR logic)
   - Clear all button

3. Update galleryService:
   - Add getPhotosByTags(tagIds: string[])

**Files to Create**:
```
frontend/src/components/organisms/TagFilter.tsx
```

**Files to Modify**:
```
frontend/src/screens/GalleryScreen.tsx ← Add TagFilter
frontend/src/services/galleryService.ts ← Add tag filtering
```

**Acceptance Criteria**:
- [ ] Clicking tag filters photos
- [ ] Multiple tags work (OR logic)
- [ ] Clear filter shows all photos
- [ ] Photo count per tag displayed
- [ ] URL reflects filter state (web)

---

### Phase 5: Story 3.5 - Individual Download (4-6 hours)

**Backend**:
1. Create DownloadService.java:
   - Generate CloudFront signed URLs
   - Fallback to S3 presigned URLs
   - 1-hour expiration

2. Update PhotoController:
   - GET /api/photos/{photoId}/download returns real signed URL

**Frontend**:
1. Add download button to Lightbox
2. Handle download:
   - Web: Trigger browser download
   - Mobile: Save to gallery (permissions)

**Files to Create**:
```
backend/src/main/java/com/rapidphoto/services/DownloadService.java
```

**Files to Modify**:
```
backend/src/main/java/com/rapidphoto/api/PhotoController.java
frontend/src/components/organisms/Lightbox.tsx ← Add download button
```

**Acceptance Criteria**:
- [ ] Download button works
- [ ] Web: File downloads to Downloads folder
- [ ] Mobile: Photo saves to gallery
- [ ] Permission prompt on first download (mobile)
- [ ] Success notification shown

---

### Phase 6: Story 3.6 - Batch ZIP Download (6-8 hours)

**Backend**:
1. Create ZipService.java:
   - Stream ZIP using ZipOutputStream
   - Don't load all files in memory
   - 100-photo limit

2. Update PhotoController:
   - POST /api/photos/download-batch streams ZIP

**Frontend**:
1. Add selection mode to GalleryScreen:
   - Long-press enters selection mode
   - Checkboxes on PhotoCards
   - Selection toolbar with count

2. Create SelectionToolbar component

**Files to Create**:
```
backend/src/main/java/com/rapidphoto/services/ZipService.java
frontend/src/components/organisms/SelectionToolbar.tsx
```

**Files to Modify**:
```
backend/src/main/java/com/rapidphoto/api/PhotoController.java
frontend/src/screens/GalleryScreen.tsx ← Add selection mode
frontend/src/components/molecules/PhotoCard.tsx ← Add checkbox
```

**Acceptance Criteria**:
- [ ] Long-press enters selection mode
- [ ] Select multiple photos
- [ ] Download as ZIP works
- [ ] ZIP filename includes date and count
- [ ] 100-photo limit enforced
- [ ] Progress bar shown

---

### Phase 7: Story 3.7 - Integration Tests (6-8 hours)

**Backend**:
1. Rewrite GalleryIntegrationTest.java:
   - Test gallery loading with pagination
   - Test lightbox navigation
   - Test tagging (add/remove)
   - Test tag filter performance (<500ms for 10K photos)
   - Test individual download
   - Test batch ZIP download
   - Test empty state

2. Setup:
   - Testcontainers for PostgreSQL
   - Seed 1000 test photos
   - Mock CloudFront URLs

**Files to Modify**:
```
backend/src/test/java/com/rapidphoto/integration/GalleryIntegrationTest.java ← REWRITE
```

**Acceptance Criteria**:
- [ ] All integration tests pass
- [ ] Performance test: Tag filter <500ms
- [ ] Load test: 100 concurrent users

---

## Verification Checklist

Before marking Epic 3 complete:

### Backend:
- [ ] All PhotoController endpoints return real data (not mocks)
- [ ] TagController uses CQRS handlers (not mocks)
- [ ] DownloadService generates signed URLs
- [ ] ZipService streams without OOM
- [ ] All backend tests pass (./gradlew test)
- [ ] Integration tests pass

### Frontend:
- [ ] Gallery displays actual photos with images
- [ ] Lightbox works with navigation and zoom
- [ ] Tagging works end-to-end
- [ ] Tag filtering works
- [ ] Downloads work (individual + ZIP)
- [ ] All frontend tests pass (npm test)

### End-to-End:
- [ ] Upload photos in Epic 2
- [ ] View photos in Gallery (Epic 3)
- [ ] Open photo in Lightbox
- [ ] Add tags to photo
- [ ] Filter by tags
- [ ] Download individual photo
- [ ] Download multiple as ZIP

---

## Estimated Timeline

| Phase | Time | Priority |
|-------|------|----------|
| Story 3.1 (Gallery) | 14-18h | P0 |
| Story 3.2 (Lightbox) | 6-8h | P0 |
| Story 3.3 (Tagging) | 10-12h | P1 |
| Story 3.4 (Tag Filter) | 4-6h | P1 |
| Story 3.5 (Download) | 4-6h | P2 |
| Story 3.6 (ZIP) | 6-8h | P2 |
| Story 3.7 (Tests) | 6-8h | P1 |
| **TOTAL** | **50-66h** | |

---

## Risk Mitigation

1. **No photos in database**: Run Epic 2 upload first, or seed test data
2. **CloudFront not configured**: Use S3 presigned URLs as fallback
3. **LocalStack S3 issues**: Test with real S3 bucket
4. **Frontend hot reload issues**: Restart webpack dev server

---

## Files Created This Session

1. `/EPIC-3-STATUS-REPORT.md` - Analysis
2. `/EPIC-3-IMPLEMENTATION-PLAN.md` - This file
3. `/frontend/src/services/galleryService.ts` - Complete

---

## Recommended Approach

**Option A: Complete Implementation (50-66 hours)**
- Implement all 7 stories
- Full production-ready gallery

**Option B: MVP (20-26 hours)**
- Stories 3.1 + 3.2 only
- Basic gallery + lightbox
- Ship early, iterate later

**Option C: Use BMAD Orchestrator**
- Create stories/ directory with 7 story files
- Use @sm-scrum, @dev, @qa-quality agents
- Automate the implementation cycle

---

**Next Action**: Choose approach and begin systematic implementation of Story 3.1.
