# Epic 2 Manual Validation Guide

**Epic**: Epic 2 - Core Upload Experience
**Phase**: B (Real-Time Updates) - Complete
**Date**: 2025-11-11
**Status**: Ready for Manual Validation

---

## Overview

This document provides step-by-step manual validation instructions for Epic 2 Phase B (Stories 2.6-2.9). All automated tests are passing (82/82), but manual validation is recommended to verify the complete user experience.

---

## Prerequisites

### Environment Setup

1. **Backend Running**:
   ```bash
   cd backend
   AWS_S3_ENDPOINT=http://localhost:4566 \
   AWS_ACCESS_KEY_ID=test \
   AWS_SECRET_ACCESS_KEY=test \
   ./gradlew bootRun
   ```
   - Should start on port 8080
   - Verify: `http://localhost:8080/api/health` returns 204

2. **Frontend Running**:
   ```bash
   cd frontend
   npm run web
   ```
   - Should start on port 3000
   - Open: `http://localhost:3000`

3. **LocalStack Running** (for S3):
   ```bash
   docker run -d -p 4566:4566 localstack/localstack
   ```

4. **Redis Running** (for SSE pub/sub):
   ```bash
   docker run -d -p 6379:6379 redis:alpine
   ```

### Test Account

- **Email**: test@example.com
- **Password**: Test123!
- Create account via UI if needed: `/signup`

---

## Story 2.6: SSE Infrastructure

**Goal**: Verify SSE endpoints are working and authenticated

### Test 1: SSE Endpoint Returns 401 Without Auth

**Steps**:
1. Open browser DevTools (F12) → Network tab
2. In console, run:
   ```javascript
   fetch('http://localhost:8080/api/upload/sessions/test-session/stream')
   ```
3. **Expected**: Response status 401 Unauthorized

**Result**: ☐ Pass ☐ Fail

---

### Test 2: SSE Endpoint Returns 200 With Valid JWT

**Steps**:
1. Login to app → Copy JWT token from localStorage
   ```javascript
   localStorage.getItem('authToken')
   ```
2. In console, test SSE connection:
   ```javascript
   const token = localStorage.getItem('authToken');
   const eventSource = new EventSource(
     `http://localhost:8080/api/upload/sessions/test-session/stream?token=${token}`
   );
   eventSource.onopen = () => console.log('SSE Connected!');
   eventSource.onerror = (e) => console.error('SSE Error:', e);
   ```
3. **Expected**: Console shows "SSE Connected!"
4. Close connection: `eventSource.close()`

**Result**: ☐ Pass ☐ Fail

---

### Test 3: SSE Health Info Endpoint

**Steps**:
1. Navigate to: `http://localhost:8080/api/upload/sessions/sse-info`
2. **Expected**: JSON response with:
   ```json
   {
     "sseSupported": true,
     "endpoint": "/api/upload/sessions/{sessionId}/stream",
     "messageTypes": ["PHOTO_UPLOADED", "PHOTO_FAILED", "SESSION_COMPLETED"]
   }
   ```

**Result**: ☐ Pass ☐ Fail

---

## Story 2.7: Real-Time Progress Broadcasting

**Goal**: Verify upload progress events are published via SSE

### Test 4: Upload Photo and Receive SSE Event

**Steps**:
1. Login to app
2. Navigate to Upload screen (`/upload`)
3. Open DevTools → Network tab → Filter by "stream"
4. Select 1-3 photos (small files recommended)
5. Click "Start Upload"
6. **Expected**:
   - Network tab shows EventSource connection to `/stream` endpoint
   - Connection status: "pending" (stays open)
   - Events tab shows incoming messages:
     - `PHOTO_UPLOADED` events for each successful photo
     - `SESSION_COMPLETED` event when all photos uploaded

**Result**: ☐ Pass ☐ Fail

**Screenshot**: ☐ Captured (DevTools Network → Events tab)

---

### Test 5: Multiple Photos Upload Progress

**Steps**:
1. Upload 10 small photos (~100KB each)
2. Watch DevTools Events tab
3. **Expected**:
   - See 10 `PHOTO_UPLOADED` events
   - Each event has:
     - `type: "PHOTO_UPLOADED"`
     - `sessionId`: matches your session
     - `photoId`: unique for each photo
     - `uploadedCount`: increments (1, 2, 3... 10)
     - `totalCount: 10`
     - `progressPercent`: increases (10%, 20%... 100%)
   - Final `SESSION_COMPLETED` event with:
     - `uploadedCount: 10`
     - `failedCount: 0`

**Result**: ☐ Pass ☐ Fail

**Notes**: ___________________________________________

---

## Story 2.8: SSE Client Integration

**Goal**: Verify SSE client reconnection and state sync

### Test 6: SSE Connection Status Banner

**Steps**:
1. Login and navigate to Upload screen
2. Start uploading 5 photos
3. **Expected**:
   - No connection banner visible (connected state)
   - Upload progress updates in real-time

**Result**: ☐ Pass ☐ Fail

---

### Test 7: SSE Reconnection After Network Loss

**Steps**:
1. Start uploading 10 photos
2. Open DevTools → Network tab → Enable "Offline" mode
3. **Expected**:
   - Connection banner appears: "Connection lost. Uploads paused."
   - Banner color: orange/warning
4. Wait 5 seconds, then disable "Offline" mode
5. **Expected**:
   - Banner changes to: "Connection restored. Resuming uploads."
   - Banner color: green/success
   - SSE reconnects automatically
   - Upload progress resumes
   - Banner auto-dismisses after 3 seconds

**Result**: ☐ Pass ☐ Fail

**Notes**: ___________________________________________

---

### Test 8: State Sync on Reconnect

**Steps**:
1. Start uploading 20 photos
2. When 10 photos are uploaded, go offline (DevTools → Network → Offline)
3. Wait 10 seconds while remaining 10 upload via backend
4. Go back online
5. **Expected**:
   - SSE reconnects
   - UI fetches latest session state from REST API
   - Progress jumps to correct position (shows 20/20 complete)
   - No stale data displayed

**Result**: ☐ Pass ☐ Fail

**Notes**: ___________________________________________

---

## Story 2.9: Upload Completion Notification

**Goal**: Verify completion modal with confetti

### Test 9: Full Success Modal with Confetti

**Steps**:
1. Upload 5 photos successfully (no failures)
2. Wait for all uploads to complete
3. **Expected**:
   - Modal appears with:
     - Title: "Upload Complete! 🎉"
     - Message: "All 5 photos uploaded successfully"
     - Confetti animation plays from top of screen
     - Single "View Photos" button (primary style)
   - Modal auto-dismisses after 5 seconds

**Result**: ☐ Pass ☐ Fail

**Screenshot**: ☐ Captured (modal with confetti)

---

### Test 10: Partial Success Modal (No Auto-Dismiss)

**Steps**:
1. Upload 10 photos where 2-3 will fail (e.g., invalid file types or corrupt files)
2. Wait for session to complete
3. **Expected**:
   - Modal appears with:
     - Title: "Upload Completed"
     - Message: "7 of 10 photos uploaded successfully"
     - Message: "3 photos failed"
     - No confetti animation
     - Two buttons:
       - "Retry Failed" (secondary style)
       - "View Photos" (primary style)
   - Modal does NOT auto-dismiss (must click button)

**Result**: ☐ Pass ☐ Fail

**Notes**: ___________________________________________

---

### Test 11: View Photos Navigation

**Steps**:
1. Complete upload (trigger completion modal)
2. Click "View Photos" button
3. **Expected**:
   - Modal closes
   - Navigates to Gallery screen (`/gallery`)
   - Uploaded photos visible in gallery

**Result**: ☐ Pass ☐ Fail

---

### Test 12: Retry Failed Button (Placeholder)

**Steps**:
1. Trigger partial success modal (some failures)
2. Click "Retry Failed" button
3. **Expected**:
   - Console logs: "Retry failed uploads not yet implemented (Story 2.12)"
   - Modal closes
   - (Full retry logic will be Story 2.12)

**Result**: ☐ Pass ☐ Fail

---

## Browser Compatibility Testing

### Test 13: Chrome/Edge

**Steps**:
1. Run Tests 1-12 in Chrome or Edge
2. Verify all SSE connections, modals, and animations work

**Result**: ☐ Pass ☐ Fail

**Browser Version**: ___________________________________________

---

### Test 14: Firefox

**Steps**:
1. Run Tests 1-12 in Firefox
2. Verify all SSE connections, modals, and animations work

**Result**: ☐ Pass ☐ Fail

**Browser Version**: ___________________________________________

---

### Test 15: Safari (if available)

**Steps**:
1. Run Tests 1-12 in Safari
2. Verify all SSE connections, modals, and animations work

**Result**: ☐ Pass ☐ Fail

**Browser Version**: ___________________________________________

---

## Performance Testing

### Test 16: Large Upload (50 photos)

**Steps**:
1. Upload 50 photos simultaneously
2. Observe:
   - SSE connection stability
   - Real-time progress updates
   - Memory usage (DevTools → Performance → Memory)
   - Completion modal triggers correctly

**Expected**:
- All 50 photos complete successfully
- No SSE connection drops
- Memory usage stable (<500MB increase)

**Result**: ☐ Pass ☐ Fail

**Notes**: ___________________________________________

---

### Test 17: Multiple Sessions (2+ Browser Tabs)

**Steps**:
1. Open 2 browser tabs with same account
2. Start uploads in both tabs (different photos)
3. **Expected**:
   - Each tab has independent SSE connection
   - Each tab receives its own session's progress events
   - No cross-contamination of events

**Result**: ☐ Pass ☐ Fail

---

## Accessibility Testing

### Test 18: Screen Reader Announcement

**Steps**:
1. Enable screen reader (macOS VoiceOver, Windows Narrator, NVDA)
2. Upload photos and trigger completion modal
3. **Expected**:
   - Screen reader announces: "Upload complete. 10 photos uploaded successfully"
   - Focus moves to "View Photos" button

**Result**: ☐ Pass ☐ Fail

**Screen Reader Used**: ___________________________________________

---

### Test 19: Keyboard Navigation

**Steps**:
1. Trigger completion modal
2. Press Tab key
3. **Expected**:
   - Focus moves to "View Photos" button
   - (For partial success) Tab moves to "Retry Failed" button
   - Enter key activates focused button
   - Escape key closes modal

**Result**: ☐ Pass ☐ Fail

---

## Edge Cases

### Test 20: Network Offline During Upload

**Steps**:
1. Start uploading 10 photos
2. After 3 photos uploaded, disconnect network (WiFi off or Offline mode)
3. Wait 10 seconds
4. Reconnect network
5. **Expected**:
   - Connection banner shows "Connection lost"
   - Uploads pause at 3/10
   - On reconnect, banner shows "Connection restored"
   - Uploads resume from 4/10
   - SSE reconnects automatically
   - State syncs correctly

**Result**: ☐ Pass ☐ Fail

---

### Test 21: Backend Server Restart During Upload

**Steps**:
1. Start uploading 10 photos
2. After 5 photos uploaded, restart backend server
3. Wait for backend to restart (~10-20 seconds)
4. **Expected**:
   - SSE connection drops
   - Connection banner shows "Connection lost"
   - When backend restarts, SSE reconnects
   - State syncs via REST API
   - Shows correct progress (5/10 or higher)

**Result**: ☐ Pass ☐ Fail

**Notes**: ___________________________________________

---

### Test 22: Zero Photos Uploaded (All Failures)

**Steps**:
1. Upload 5 invalid files (e.g., .exe files or corrupt images)
2. Wait for session to complete
3. **Expected**:
   - Partial success modal appears
   - Message: "0 of 5 photos uploaded successfully"
   - Message: "5 photos failed"
   - "Retry Failed" button present
   - No "View Photos" button (nothing to view)

**Result**: ☐ Pass ☐ Fail

---

## Security Testing

### Test 23: JWT Expiration

**Steps**:
1. Start upload with valid JWT
2. Manually expire JWT (set expiration in past via backend)
3. **Expected**:
   - SSE connection receives 401 error
   - Frontend redirects to login screen
   - Upload pauses gracefully

**Result**: ☐ Pass ☐ Fail

---

### Test 24: Cross-User Session Access

**Steps**:
1. Login as User A, start upload (note sessionId)
2. Logout, login as User B
3. Try to connect to User A's SSE stream via console:
   ```javascript
   const token = localStorage.getItem('authToken');
   const eventSource = new EventSource(
     `http://localhost:8080/api/upload/sessions/USER_A_SESSION_ID/stream?token=${token}`
   );
   ```
4. **Expected**:
   - Connection fails with 403 Forbidden (user doesn't own session)

**Result**: ☐ Pass ☐ Fail

---

## Known Limitations (Acceptable)

The following are known limitations documented in Phase B completion report:

1. **E2E Tests**: Not implemented (deferred to Playwright/Cypress)
2. **Sound/Haptic Feedback**: Placeholder (requires Story 1.4)
3. **Mobile Confetti**: Web-only (no React Native confetti yet)
4. **Full Accessibility Audit**: Basic structure only (full WCAG 2.1 AA audit pending)

---

## Validation Summary

**Total Tests**: 24
**Tests Passed**: _____ / 24
**Tests Failed**: _____ / 24
**Pass Rate**: _____ %

**Blocker Issues Found**: ___________________________________________

**Minor Issues Found**: ___________________________________________

**Recommendations**: ___________________________________________

---

## Sign-Off

**Tester Name**: ___________________________________________
**Date**: ___________________________________________
**Environment**: ___________________________________________
**Overall Status**: ☐ Approved ☐ Approved with Minor Issues ☐ Rejected

**Notes**:

___________________________________________

___________________________________________

___________________________________________

---

## Next Steps

If validation passes:
- ✅ Epic 2 Phase B ready for production deployment
- ✅ Proceed with Phase C (Stories 2.10-2.14)

If validation fails:
- Document blocker issues
- Create bug tickets
- Re-test after fixes

---

**Document Version**: 1.0
**Last Updated**: 2025-11-11
**Related Documents**:
- PHASE-B-COMPLETION-REPORT.md
- PHASE-C-HANDOFF.md
- docs/orchestration-flow.md
