# Epic 3: BMAD Orchestrator Handoff Document

**Date**: 2025-11-12
**Branch**: `epic-3-photo-gallery-viewing-tagging-download`
**Status**: Ready for BMAD Orchestration
**Current Completion**: ~5%

---

## Executive Summary

Epic 3 was previously thought to be "complete" but comprehensive review revealed it's only ~5% done (scaffolding only). This handoff provides everything needed to complete it using the BMAD orchestrator approach.

---

## What Was Discovered

### ✅ Actually Complete:
- Photo domain model (Photo.java, PhotoRepository.java)
- PhotoDTO and GetPhotosForUserQueryHandler
- Database migrations (V3__create_tagging_tables.sql exists)
- galleryService.ts (created this session)

### ❌ Not Actually Done (Needs Implementation):
- **Story 3.1**: Gallery UI (empty skeleton, no images)
- **Story 3.2**: Lightbox component (doesn't exist)
- **Story 3.3**: Tagging (mock controllers, no Tag domain model)
- **Story 3.4**: Tag filtering (not implemented)
- **Story 3.5**: Downloads (mock responses)
- **Story 3.6**: ZIP downloads (mock responses)
- **Story 3.7**: Integration tests (empty file with TODOs)

---

## Documents Created This Session

All documentation is in the project root:

1. **EPIC-3-STATUS-REPORT.md**
   - Comprehensive story-by-story breakdown
   - What exists vs what's missing
   - 44-58 hour estimate

2. **EPIC-3-IMPLEMENTATION-PLAN.md**
   - Phase-by-phase implementation guide
   - Exact files to create/modify
   - Acceptance criteria
   - 50-66 hour estimate

3. **This file (EPIC-3-HANDOFF.md)**
   - Orchestrator startup instructions

---

## How to Resume with BMAD Orchestrator

### Step 1: Activate Orchestrator

Copy and paste this prompt to start:

```
Use /Users/reena/gauntletai/picstormai/orchestrator.md to complete Epic 3: Photo Gallery.

Current status:
- Epic 3 is ~5% complete (scaffolding only)
- Need to implement 7 stories: 3.1 through 3.7
- All documentation is in EPIC-3-STATUS-REPORT.md and EPIC-3-IMPLEMENTATION-PLAN.md
- Branch: epic-3-photo-gallery-viewing-tagging-download

Start with Story 3.1: Photo Gallery UI with Infinite Scroll.

Create stories/3-1-photo-gallery-ui.md from docs/epics/epic-3-photo-gallery-viewing-tagging-download.md

Follow the BMAD cycle: @sm-scrum → @dev → @qa-quality for each story.
```

### Step 2: Orchestrator Will Run Automatically

The orchestrator will:
1. Read `docs/epics/epic-3-photo-gallery-viewing-tagging-download.md`
2. Create story files in `stories/` directory
3. For each story:
   - @sm-scrum finalizes story → "Ready for Development"
   - @dev implements → "Ready for Review"
   - @qa-quality reviews → "Done" or "In Progress" (with feedback)
   - If "In Progress": @dev fixes → back to @qa-quality
   - If "Done": Move to next story
4. Continue until all 7 stories are "Done"

---

## Story Order

The orchestrator should implement in this order:

1. **Story 3.1**: Photo Gallery UI (Priority: P0)
   - Fix PhotoController to return real data
   - Create PhotoCard, PhotoGrid components
   - Rewrite GalleryScreen with sorting and infinite scroll

2. **Story 3.2**: Lightbox (Priority: P0)
   - Create Lightbox component
   - Add zoom and keyboard navigation

3. **Story 3.3**: Photo Tagging (Priority: P1)
   - Create Tag domain model
   - Implement CQRS commands/queries
   - Build TagChip and TagInput UI

4. **Story 3.4**: Tag Filter (Priority: P1)
   - Create TagFilter component
   - Integrate with GalleryScreen

5. **Story 3.5**: Individual Download (Priority: P2)
   - Implement DownloadService with signed URLs
   - Add download button to Lightbox

6. **Story 3.6**: Batch ZIP Download (Priority: P2)
   - Implement ZipService
   - Add selection mode to Gallery

7. **Story 3.7**: Integration Tests (Priority: P1)
   - Write comprehensive tests
   - Performance test: <500ms tag filter for 10K photos

---

## Key Files Reference

### Epic Documentation:
```
docs/epics/epic-3-photo-gallery-viewing-tagging-download.md
```

### Implementation Guides:
```
EPIC-3-STATUS-REPORT.md       ← What's done/missing
EPIC-3-IMPLEMENTATION-PLAN.md ← How to implement
```

### Orchestrator Files:
```
orchestrator.md              ← BMAD orchestrator instructions
docs/orchestration-flow.md   ← Will be created/updated
stories/3-*.md              ← Will be created by @sm-scrum
```

### Agents:
```
.claude/agents/sm-scrum.md    ← Story finalization
.claude/agents/dev.md         ← Implementation
.claude/agents/qa-quality.md  ← Review and validation
```

---

## Critical Reminders for Orchestrator

### 1. Story Status Gates (MANDATORY)
Each agent MUST update story status:
- @sm-scrum: Draft → "Ready for Development"
- @dev (new): "Ready for Development" → "Ready for Review"
- @qa-quality: "Ready for Review" → "Done" OR "In Progress"
- @dev (fix): "In Progress" → "Ready for Review"

**If status not updated**: Re-invoke agent with explicit reminder.

### 2. Verification After EVERY Agent
- [ ] Story file has new status?
- [ ] Status matches expected transition?
- [ ] Agent notes/feedback added?
- [ ] Logged to orchestration-flow.md?

### 3. Don't Stop After One Story
After Story 3.1 is "Done":
- Immediately create Story 3.2
- Continue until ALL 7 stories are "Done"
- Only interrupt human when Epic 3 100% complete

### 4. Backend Must Actually Work
Story 3.1 requires:
- PhotoController returns real photos (not empty list)
- Database has photos to display (from Epic 2)
- If no photos exist: @dev must seed test data

Verify with: `curl -H "Authorization: Bearer <token>" http://localhost:8080/api/photos`

### 5. Frontend Must Display Images
Story 3.1 is NOT done until:
- Gallery shows actual photo images
- Photos are rendered (not just filenames)
- Infinite scroll works
- Sorting works
- Empty state displays when no photos

**QA must actually test in browser at http://localhost:8081**

---

## Test Data

If database is empty, @dev can create test photos using:

```sql
INSERT INTO photos (id, user_id, session_id, filename, file_size, upload_status, progress, metadata, created_at, updated_at)
VALUES
  (gen_random_uuid(), '<user-id>', gen_random_uuid(), 'test-photo-1.jpg', 2048000, 'COMPLETED', 100, '{}'::jsonb, NOW(), NOW()),
  (gen_random_uuid(), '<user-id>', gen_random_uuid(), 'test-photo-2.jpg', 1024000, 'COMPLETED', 100, '{}'::jsonb, NOW(), NOW()),
  -- ... more photos
```

Or run Epic 2 upload flow to create real photos.

---

## Success Criteria

Epic 3 is COMPLETE when:

### Backend:
- [ ] PhotoController returns real photos
- [ ] TagController uses CQRS (not mocks)
- [ ] DownloadService generates signed URLs
- [ ] ZipService streams without OOM
- [ ] All backend tests pass: `./gradlew test`

### Frontend:
- [ ] Gallery displays photos as images
- [ ] Lightbox works with zoom and navigation
- [ ] Tagging works end-to-end
- [ ] Tag filtering works
- [ ] Downloads work (individual + ZIP)
- [ ] All frontend tests pass: `npm test`

### Integration:
- [ ] Upload photos (Epic 2) → View in Gallery (Epic 3)
- [ ] Open lightbox → Add tags → Filter by tags → Download
- [ ] All 7 stories marked "Done" in stories/ directory
- [ ] Integration tests pass

---

## Estimated Timeline

Using BMAD orchestrator with 3 agents working in parallel:

| Story | Dev Time | QA Cycles | Total |
|-------|----------|-----------|-------|
| 3.1   | 10-12h   | 2-3 cycles| 12-15h|
| 3.2   | 5-6h     | 1-2 cycles| 6-8h  |
| 3.3   | 8-10h    | 2-3 cycles| 10-12h|
| 3.4   | 3-4h     | 1 cycle   | 4-5h  |
| 3.5   | 3-4h     | 1 cycle   | 4-5h  |
| 3.6   | 5-6h     | 1-2 cycles| 6-8h  |
| 3.7   | 5-6h     | 1 cycle   | 6-7h  |
| **Total** | **39-48h** | **9-13 cycles** | **48-60h** |

With orchestrator parallelization: ~40-50 hours actual time.

---

## Troubleshooting

### Issue: "No photos in database"
**Solution**:
1. Check Epic 2 validation report: Should show 111 photos uploaded
2. If not, run Epic 2 upload flow first
3. Or seed test data (see Test Data section above)

### Issue: "PhotoController returns empty array"
**Solution**:
1. Check PhotoController.java line 30-46
2. Verify GetPhotosForUserQueryHandler is properly wired
3. Check database has photos for current user
4. @dev must fix controller to return real data

### Issue: "Frontend shows 'Failed to load' for all images"
**Solution**:
1. Check photo.storageUrl is not null in database
2. Check CORS on S3/LocalStack
3. Use placeholder images if S3 not configured
4. @dev must handle missing storageUrl gracefully

### Issue: "Tag table doesn't exist"
**Solution**:
1. Migration V3__create_tagging_tables.sql should exist
2. Run: `./gradlew flywayMigrate` to apply migrations
3. Verify tags and photo_tags tables exist

---

## Files Created This Session

```
frontend/src/services/galleryService.ts   ← Complete photo API service
EPIC-3-STATUS-REPORT.md                   ← Comprehensive analysis
EPIC-3-IMPLEMENTATION-PLAN.md             ← Implementation roadmap
EPIC-3-HANDOFF.md                         ← This file
```

---

## Quick Reference: Agent Commands

### Start Orchestrator:
```
Use orchestrator.md to complete Epic 3
```

### Create Story:
```
@sm-scrum Create story 3-1-photo-gallery-ui
Status: Draft
Source: docs/epics/epic-3-photo-gallery-viewing-tagging-download.md
CRITICAL: Update status to "Ready for Development" when complete.
```

### Implement Story:
```
@dev Implement story stories/3-1-photo-gallery-ui.md
Current Status: Ready for Development
CRITICAL: Update status to "Ready for Review" when complete.
```

### Review Story:
```
@qa-quality Review story stories/3-1-photo-gallery-ui.md
Current Status: Ready for Review
CRITICAL: Update status to "Done" OR "In Progress" with feedback.
```

### Fix Issues:
```
@dev Fix story stories/3-1-photo-gallery-ui.md
Current Status: In Progress
QA Feedback: [specific issues from QA]
CRITICAL: Update status to "Ready for Review" when fixed.
```

---

## Final Notes

1. **Trust the Process**: The orchestrator will manage the full cycle automatically
2. **Verify Status Changes**: After each agent, check story file was updated
3. **Don't Skip QA**: Every story must pass @qa-quality before marking "Done"
4. **Test in Browser**: @qa-quality should actually open http://localhost:8081 and verify features work
5. **Keep Going**: Don't stop until all 7 stories are "Done" and Epic 3 is validated

---

## Next Session Startup Command

```
Use /Users/reena/gauntletai/picstormai/orchestrator.md to complete Epic 3: Photo Gallery.

Read EPIC-3-HANDOFF.md for context.

Create stories from docs/epics/epic-3-photo-gallery-viewing-tagging-download.md

Start with Story 3.1 and continue until all 7 stories are Done.
```

---

**Status**: Ready for orchestration
**Blocker**: None
**Risk**: Low - all documentation complete, clear path forward
