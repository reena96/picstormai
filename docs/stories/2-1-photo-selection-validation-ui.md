# Story 2.1: Photo Selection & Validation UI

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase A - Basic Upload (Weeks 1-2)
**Status**: Draft
**Priority**: High
**Estimated Effort**: 3 days

---

## User Story

**As a** user
**I want to** select up to 100 photos via drag-drop or file picker
**So that** I can prepare them for upload

---

## Acceptance Criteria

### AC1: Photo Selection via File Picker
**Given** I am on upload screen
**When** I click "Select Photos" button
**Then** native file picker opens
**And** I can select multiple image files
**And** selected photos appear in grid with thumbnails

### AC2: Drag-Drop Photo Selection (Web Only)
**Given** I am on upload screen (web)
**When** I drag photos onto dropzone area
**Then** dropzone highlights with visual feedback
**And** on drop, photos appear in grid with thumbnails

### AC3: Maximum Photo Count Validation
**Given** I attempt to select more than 100 photos
**When** selection exceeds limit
**Then** I see error toast "Maximum 100 photos per upload"
**And** only first 100 photos are added

### AC4: File Type Validation
**Given** I select a non-image file (PDF, TXT, etc)
**When** validation runs
**Then** I see error "Only image files allowed (JPG, PNG, GIF, WebP)"
**And** invalid files are NOT added to grid

### AC5: Large File Warning
**Given** I select photo over 50MB
**When** validation runs
**Then** I see warning "Large files may take longer to upload"
**And** photo is still added (not blocked)

### AC6: Thumbnail Generation
**Given** I select photos
**When** photos are added to grid
**Then** client-side thumbnails are generated immediately
**And** thumbnails display before upload starts

---

## Technical Notes

### Frontend Implementation
- **File Picker**: react-native-document-picker for native, HTML5 `<input type="file">` for web
- **Drag-Drop**: HTML5 Drag and Drop API (web only)
- **Validation Rules**:
  - Supported MIME types: image/jpeg, image/png, image/gif, image/webp
  - Max count: 100 photos per session
  - Individual file size limit: 50MB (warning only, not blocking)
- **Thumbnail Generation**:
  - Web: Canvas API to resize images to 150x150px thumbnails
  - Mobile: React Native Image with resize mode
  - Cache thumbnails in memory for performance

### Component Structure
```
UploadScreen (page)
├── PhotoDropzone (organism) - drag-drop area
├── PhotoGrid (organism) - displays selected photos
│   └── PhotoCard (molecule) - individual photo with thumbnail
└── ErrorToast (atom) - validation errors
```

### State Management
```typescript
interface PhotoSelection {
  id: string;
  file: File;
  thumbnail: string; // base64 data URL
  fileName: string;
  fileSize: number;
  mimeType: string;
  status: 'selected' | 'validating' | 'invalid';
  error?: string;
}
```

---

## Prerequisites
- Story 0.5 (Design System) - COMPLETE
- Story 1.3 (Login) - COMPLETE

---

## Testing Requirements

### Unit Tests
- [ ] Validate file type correctly (accept JPG, PNG, GIF, WebP)
- [ ] Validate file type correctly (reject PDF, TXT, etc)
- [ ] Validate max photo count (reject 101st photo)
- [ ] Generate thumbnail from image file
- [ ] Handle corrupt image file gracefully

### Integration Tests
- [ ] Select 10 photos via file picker, verify all appear in grid
- [ ] Select 101 photos, verify only 100 added with error toast
- [ ] Select PDF file, verify error message shown

### E2E Tests
- [ ] User flow: Open upload screen → Select 10 photos → See thumbnails
- [ ] User flow: Drag-drop 5 photos (web) → See dropzone highlight → Verify thumbnails
- [ ] Accessibility: Keyboard navigation works for file picker button
- [ ] Accessibility: Screen reader announces photo count changes

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] E2E test scenarios passing
- [ ] Code reviewed and approved
- [ ] Design system components used consistently
- [ ] Accessibility requirements met (WCAG 2.1 AA)
- [ ] Mobile and web tested
- [ ] No console errors or warnings

---

## Notes
- This story is frontend-only, no backend changes required
- Thumbnails generated client-side to minimize backend load
- Drag-drop is web-only feature (not available on mobile)
- File validation happens immediately, no server round-trip

---

**Status Log:**
- 2025-11-11: Story created (Draft)
