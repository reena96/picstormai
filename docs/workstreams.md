# Workstreams & Parallel Execution Map
# PicStormAI - RapidPhotoUpload

**Version:** 1.0
**Status:** Active Development Plan
**Last Updated:** 2025-11-11
**Current Phase:** Epic 2 (Core Upload Experience)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Completion Status](#completion-status)
3. [Epic Dependency Chain](#epic-dependency-chain)
4. [Workstream Definitions](#workstream-definitions)
5. [Epic 2: Parallel Execution Strategy](#epic-2-parallel-execution-strategy)
6. [Epic 3: Parallel Execution Strategy](#epic-3-parallel-execution-strategy)
7. [Critical Path Analysis](#critical-path-analysis)
8. [Resource Allocation Recommendations](#resource-allocation-recommendations)
9. [Risk & Coordination Points](#risk--coordination-points)

---

## Executive Summary

**Current State:**
- ✅ Epic 0 (Foundation & Infrastructure): **COMPLETE**
- ✅ Epic 1 (Authentication & Onboarding): **COMPLETE**
- 🚧 Epic 2 (Core Upload Experience): **READY TO START**
- ⏸️ Epic 3 (Gallery, Viewing, Tagging & Download): **BLOCKED** (requires Epic 2)

**Optimal Execution Strategy:**
- **Epic 2**: 3 parallel workstreams (Backend, Frontend, Infrastructure)
- **Epic 3**: 2 parallel workstreams (Backend, Frontend) after Epic 2 Phase A
- **Time Savings**: 40% reduction in wall-clock time via parallelization

**Key Insight:** While epics are sequential, **stories within each epic can run in parallel** across backend, frontend, and infrastructure teams.

---

## Completion Status

### Epic 0: Foundation & Infrastructure ✅ COMPLETE

| Story | Status | Completion Date |
|-------|--------|-----------------|
| 0.1 AWS Infrastructure Setup | ✅ DONE | 2025-11-09 |
| 0.2 Database Schema & Migrations | ✅ DONE | 2025-11-09 |
| 0.3 Domain Model Implementation (DDD) | ✅ DONE | 2025-11-09 |
| 0.4 CQRS Structure Setup | ✅ DONE | 2025-11-09 |
| 0.5 Design System & Component Library | ✅ DONE | 2025-11-09 |
| 0.6 Infrastructure Integration Tests | ✅ DONE | 2025-11-09 |

**Unblocks:** Epic 1, Epic 2, Epic 3

---

### Epic 1: Authentication & Onboarding ✅ COMPLETE

| Story | Status | Completion Date |
|-------|--------|-----------------|
| 1.1 JWT Authentication Backend | ✅ DONE | 2025-11-10 |
| 1.2 User Registration & Email Verification | ✅ DONE | 2025-11-10 |
| 1.3 Login/Logout UI (Web + Mobile) | ✅ DONE | 2025-11-10 |
| 1.4 User Settings Panel | ✅ DONE | 2025-11-10 |
| 1.5 Onboarding Tutorial (First-Time Users) | ✅ DONE | 2025-11-10 |
| 1.6 Authentication Integration Tests | ✅ DONE | 2025-11-10 |

**Unblocks:** Epic 2, Epic 3

---

## Epic Dependency Chain

```
┌─────────────────────────────────────────────────────────────┐
│                    EPIC DEPENDENCY FLOW                     │
└─────────────────────────────────────────────────────────────┘

Epic 0: Foundation (COMPLETE)
  ↓
  ├──→ Epic 1: Authentication (COMPLETE)
  │      ↓
  │      └──→ Epic 2: Upload Experience (NEXT)
  │             ├──→ Phase A: Basic Upload (Weeks 1-2)
  │             ├──→ Phase B: Real-Time (Weeks 3-4)
  │             └──→ Phase C: Resilience (Weeks 5-6)
  │                    ↓
  │                    └──→ Epic 3: Gallery & Download
  │                           (Weeks 7-9)
  └──→ All epics depend on Epic 0
```

**Critical Blocking Relationships:**
1. **Epic 0 blocks everything** - Must complete first
2. **Epic 1 blocks Epic 2 & 3** - Authentication required for protected endpoints
3. **Epic 2 blocks Epic 3** - Can't view/download photos that haven't been uploaded
4. **Epic 2 Phase A blocks Phase B** - Real-time updates require basic upload functionality

**Partial Unblocking:**
- Epic 3 can start once **Epic 2 Phase A (Stories 2.1-2.5)** completes
- Reason: Gallery/download only needs photos to exist in S3, not full real-time features

---

## Workstream Definitions

### Workstream 1: Backend Development
**Focus:** REST APIs, domain logic, database, AWS integrations

**Tech Stack:**
- Java 17, Spring Boot 3.x, Spring WebFlux
- PostgreSQL with R2DBC (reactive DB access)
- Redis for caching
- AWS S3 SDK for storage

**Team Skills Required:**
- Reactive programming (Mono/Flux)
- DDD (Domain-Driven Design)
- CQRS pattern
- AWS services (S3, CloudFront)

---

### Workstream 2: Frontend Development
**Focus:** UI components, user interactions, state management

**Tech Stack:**
- React Native for Web (compiles to web + mobile)
- TypeScript
- Axios for API calls
- WebSocket client (STOMP.js)

**Team Skills Required:**
- React Native
- Responsive design
- Accessibility (WCAG 2.1 AA)
- WebSocket clients

---

### Workstream 3: Infrastructure & Testing
**Focus:** AWS setup, integration tests, performance benchmarks

**Tech Stack:**
- Testcontainers (PostgreSQL, Redis, LocalStack)
- Playwright/Cypress for E2E tests
- Gatling for load testing
- GitHub Actions for CI/CD

**Team Skills Required:**
- Infrastructure as Code (Terraform)
- Test automation
- Performance engineering
- CI/CD pipelines

---

## Epic 2: Parallel Execution Strategy

**Duration:** 6 weeks
**Parallel Workstreams:** 3
**Sequential Phases:** 3 (A, B, C)

### Phase A: Basic Upload (Weeks 1-2)

```
┌──────────────────────────────────────────────────────────────┐
│                   PHASE A - BASIC UPLOAD                     │
│                      (Weeks 1-2)                             │
└──────────────────────────────────────────────────────────────┘

Week 1:
  ║
  ║  BACKEND STREAM                 FRONTEND STREAM
  ║  ───────────────               ────────────────
  ║  2.2 Upload Session            2.1 Photo Selection UI
  ║  2.3 S3 Pre-signed URLs             ↓
  ║       ↓                         (Can start in parallel)
  ║  (Can start in parallel)
  ║

Week 2:
  ║
  ║  BACKEND (cont.)               FRONTEND (cont.)
  ║  2.3 Complete                  2.4 Upload Engine
  ║       ↓                        2.5 Progress UI
  ║  Integration point:                 ↓
  ║  Backend provides URLs ────→  Frontend uploads to S3
  ║                                     ↓
  ║                            INTEGRATION POINT
  ║                          Test full upload flow
  ║
```

**Parallel Work Opportunities:**

| Story | Workstream | Duration | Can Start When | Blocks |
|-------|------------|----------|----------------|--------|
| 2.1 Photo Selection UI | Frontend | 2 days | Immediately | 2.4 |
| 2.2 Upload Session Backend | Backend | 3 days | Immediately | 2.3, 2.5 |
| 2.3 S3 Pre-signed URLs | Backend | 2 days | After 2.2 | 2.4 |
| 2.4 Client Upload Engine | Frontend | 3 days | After 2.1, 2.3 | 2.5 |
| 2.5 Progress UI | Frontend | 2 days | After 2.1, 2.2 | Phase B |

**Integration Point:** End of Week 2
- Backend team provides working pre-signed URL API
- Frontend team can upload files to S3
- Manual test: Upload 10 photos successfully

**Completion Criteria (Phase A):**
- [ ] User can select photos via drag-drop or file picker
- [ ] Backend generates S3 pre-signed URLs
- [ ] Photos upload directly to S3 (no server proxy)
- [ ] UI shows basic progress per photo
- [ ] Integration test: 10 photos → S3 → database metadata

---

### Phase B: Real-Time Updates (Weeks 3-4)

```
┌──────────────────────────────────────────────────────────────┐
│                  PHASE B - REAL-TIME UPDATES                 │
│                      (Weeks 3-4)                             │
└──────────────────────────────────────────────────────────────┘

Week 3:
  ║
  ║  BACKEND STREAM                 FRONTEND STREAM
  ║  ───────────────               ────────────────
  ║  2.6 WebSocket Server          2.8 WebSocket Client
  ║  2.7 Progress Broadcasting          ↓
  ║       ↓                         (Can start in parallel)
  ║  (Backend completes first)
  ║

Week 4:
  ║
  ║  BACKEND (cont.)               FRONTEND (cont.)
  ║  2.7 Broadcasting complete     2.8 Client receives updates
  ║       ↓                        2.9 Completion Notification
  ║  Integration point:                 ↓
  ║  Backend pushes ────────────→  Frontend displays
  ║  progress via WebSocket        real-time progress
  ║                                     ↓
  ║                            INTEGRATION POINT
  ║                     Test WebSocket push updates
  ║
```

**Parallel Work Opportunities:**

| Story | Workstream | Duration | Can Start When | Blocks |
|-------|------------|----------|----------------|--------|
| 2.6 WebSocket Server | Backend | 3 days | After 2.2 | 2.7 |
| 2.7 Progress Broadcasting | Backend | 2 days | After 2.6 | 2.8 |
| 2.8 WebSocket Client | Frontend | 3 days | After 2.6 | 2.9 |
| 2.9 Completion Notification | Frontend | 2 days | After 2.8, 1.4 (settings) | Phase C |

**Dependencies:**
- 2.8 needs 2.6 (WebSocket server must exist before client connects)
- 2.7 can run in parallel with 2.8 (backend broadcasting + frontend receiving)
- 2.9 needs 1.4 (reads user settings for sound/confetti preferences)

**Integration Point:** End of Week 4
- Backend pushes progress updates via WebSocket
- Frontend receives and displays updates in real-time
- Manual test: Upload 100 photos, verify real-time progress

**Completion Criteria (Phase B):**
- [ ] WebSocket server configured and accepting connections
- [ ] Backend broadcasts progress after each photo upload
- [ ] Frontend receives WebSocket updates < 500ms latency
- [ ] Completion notification with confetti (if enabled)
- [ ] Integration test: WebSocket updates for 100 concurrent uploads

---

### Phase C: Network Resilience (Weeks 5-6)

```
┌──────────────────────────────────────────────────────────────┐
│                 PHASE C - NETWORK RESILIENCE                 │
│                      (Weeks 5-6)                             │
└──────────────────────────────────────────────────────────────┘

Week 5:
  ║
  ║  BACKEND STREAM                 FRONTEND STREAM
  ║  ───────────────               ────────────────
  ║  2.13 Cancellation API         2.10 Network Detection
  ║       ↓                        2.11 Error Handling
  ║  (Can start in parallel)       2.12 Retry & Resume
  ║                                     ↓
  ║                               (Can start in parallel)
  ║

Week 6:
  ║
  ║  BACKEND (cont.)               FRONTEND (cont.)
  ║  2.13 Complete                 2.12 Complete
  ║       ↓                              ↓
  ║  Integration point:            Integration point:
  ║  Cancel uploads ←──────────→   Network resilience
  ║  via API                       auto-retry
  ║       ↓                              ↓
  ║                   TESTING STREAM
  ║              ─────────────────────
  ║              2.14 Upload Integration Tests
  ║                   (All scenarios)
  ║                        ↓
  ║                 EPIC 2 COMPLETE
  ║
```

**Parallel Work Opportunities:**

| Story | Workstream | Duration | Can Start When | Blocks |
|-------|------------|----------|----------------|--------|
| 2.10 Network Detection | Frontend | 2 days | Immediately | 2.12 |
| 2.11 Error Handling | Frontend | 2 days | After 2.4 | 2.12 |
| 2.12 Retry & Resume | Frontend | 3 days | After 2.10, 2.11 | 2.14 |
| 2.13 Cancellation | Backend + Frontend | 3 days | After 2.5 | 2.14 |
| 2.14 Integration Tests | Testing | 4 days | After all 2.1-2.13 | Epic 3 |

**Integration Point:** End of Week 6
- Frontend detects network loss and pauses uploads
- Uploads auto-resume when network restored
- Failed uploads retry with exponential backoff
- User can cancel individual or all uploads
- All integration tests pass

**Completion Criteria (Phase C):**
- [ ] Network loss detected within 5 seconds
- [ ] Uploads automatically pause and resume
- [ ] Failed uploads retry up to 3 times
- [ ] User can cancel uploads with confirmation
- [ ] Integration tests cover all scenarios (100% pass)
- [ ] Performance benchmark: 100 photos in <90 seconds

---

## Epic 3: Parallel Execution Strategy

**Duration:** 3 weeks
**Parallel Workstreams:** 2
**Start Condition:** Epic 2 Phase A complete (basic upload working)

### Phase 1: Gallery & Viewing (Week 1)

```
┌──────────────────────────────────────────────────────────────┐
│                  EPIC 3 - GALLERY & VIEWING                  │
│                         (Week 1)                             │
└──────────────────────────────────────────────────────────────┘

BACKEND STREAM                     FRONTEND STREAM
───────────────                    ────────────────
3.1 Gallery API                    3.1 Gallery UI
(Photos query endpoint)            (PhotoGrid component)
     ↓                                  ↓
3.2 Photo Details API              3.2 Lightbox UI
(Individual photo metadata)        (Fullscreen viewer)
     ↓                                  ↓
     └──────────────────────────────────┘
             INTEGRATION POINT
          Test gallery + lightbox
```

**Parallel Work Opportunities:**

| Story | Workstream | Duration | Can Start When | Blocks |
|-------|------------|----------|----------------|--------|
| 3.1 Gallery (BE) | Backend | 2 days | After 2.5 | 3.1 (FE) |
| 3.1 Gallery (FE) | Frontend | 3 days | After 3.1 (BE), 0.5 | 3.2 (FE) |
| 3.2 Lightbox (BE) | Backend | 1 day | After 3.1 (BE) | 3.2 (FE) |
| 3.2 Lightbox (FE) | Frontend | 2 days | After 3.1 (FE) | 3.3 |

---

### Phase 2: Tagging (Week 2)

```
┌──────────────────────────────────────────────────────────────┐
│                     EPIC 3 - TAGGING                         │
│                        (Week 2)                              │
└──────────────────────────────────────────────────────────────┘

BACKEND STREAM                     FRONTEND STREAM
───────────────                    ────────────────
3.3 Tagging API                    3.3 Tagging UI
(Add/remove tags)                  (Tag input + chips)
     ↓                                  ↓
3.4 Tag Filter API                 3.4 Tag Filter UI
(Query photos by tags)             (Filter controls)
     ↓                                  ↓
     └──────────────────────────────────┘
             INTEGRATION POINT
           Test tag filtering
```

**Parallel Work Opportunities:**

| Story | Workstream | Duration | Can Start When | Blocks |
|-------|------------|----------|----------------|--------|
| 3.3 Tagging (BE) | Backend | 2 days | After 3.2 (BE) | 3.3 (FE) |
| 3.3 Tagging (FE) | Frontend | 2 days | After 3.2 (FE) | 3.4 (FE) |
| 3.4 Filter (BE) | Backend | 2 days | After 3.3 (BE) | 3.4 (FE) |
| 3.4 Filter (FE) | Frontend | 2 days | After 3.3 (FE) | 3.5 |

---

### Phase 3: Download & Testing (Week 3)

```
┌──────────────────────────────────────────────────────────────┐
│                 EPIC 3 - DOWNLOAD & TESTING                  │
│                        (Week 3)                              │
└──────────────────────────────────────────────────────────────┘

BACKEND STREAM                     FRONTEND STREAM
───────────────                    ────────────────
3.5 Individual Download API        3.5 Download UI
(CloudFront signed URLs)           (Download button)
     ↓                                  ↓
3.6 Batch Download API             3.6 Selection Mode UI
(ZIP streaming)                    (Multi-select + ZIP download)
     ↓                                  ↓
     └──────────────────────────────────┘
                      ↓
              TESTING STREAM
          ─────────────────────
          3.7 Gallery Integration Tests
          (All scenarios validated)
                      ↓
              EPIC 3 COMPLETE
```

**Parallel Work Opportunities:**

| Story | Workstream | Duration | Can Start When | Blocks |
|-------|------------|----------|----------------|--------|
| 3.5 Download (BE) | Backend | 1 day | After 3.4 (BE) | 3.5 (FE) |
| 3.5 Download (FE) | Frontend | 2 days | After 3.4 (FE) | 3.6 (FE) |
| 3.6 Batch ZIP (BE) | Backend | 2 days | After 3.5 (BE) | 3.6 (FE) |
| 3.6 Batch ZIP (FE) | Frontend | 2 days | After 3.5 (FE) | 3.7 |
| 3.7 Integration Tests | Testing | 3 days | After all 3.1-3.6 | MVP Complete |

---

## Critical Path Analysis

### Epic 2 Critical Path (6 weeks)

**Longest Sequential Chain:**
```
2.2 (3d) → 2.3 (2d) → 2.4 (3d) → 2.6 (3d) → 2.7 (2d) → 2.8 (3d) → 2.12 (3d) → 2.14 (4d)
= 23 days (~4.5 weeks)
```

**With Parallelization:**
- Week 1-2: Phase A (2.1-2.5) in parallel
- Week 3-4: Phase B (2.6-2.9) in parallel
- Week 5-6: Phase C (2.10-2.14) in parallel
- **Total: 6 weeks**

**Without Parallelization:**
- Sequential execution of all 14 stories
- **Total: ~10 weeks** (4 weeks saved!)

---

### Epic 3 Critical Path (3 weeks)

**Longest Sequential Chain:**
```
3.1 BE (2d) → 3.1 FE (3d) → 3.2 FE (2d) → 3.3 FE (2d) → 3.4 FE (2d) → 3.6 FE (2d) → 3.7 (3d)
= 16 days (~3.5 weeks)
```

**With Parallelization:**
- Week 1: Gallery + Lightbox (backend/frontend parallel)
- Week 2: Tagging (backend/frontend parallel)
- Week 3: Download + Testing (backend/frontend parallel)
- **Total: 3 weeks**

**Without Parallelization:**
- Sequential execution of all 7 stories
- **Total: ~5 weeks** (2 weeks saved!)

---

## Resource Allocation Recommendations

### Optimal Team Structure

**Option 1: 3-Person Team (Backend, Frontend, Infra/QA)**

| Role | Responsibilities | Epic 2 Focus | Epic 3 Focus |
|------|------------------|--------------|--------------|
| Backend Dev | REST APIs, domain logic, AWS | 2.2, 2.3, 2.6, 2.7, 2.13 | 3.1-3.6 (backend APIs) |
| Frontend Dev | React Native, UI/UX | 2.1, 2.4, 2.5, 2.8-2.12 | 3.1-3.6 (frontend UI) |
| QA/DevOps | Infrastructure, integration tests | 2.14 | 3.7 |

**Pros:**
- Clear separation of concerns
- Minimal coordination overhead
- Each person owns their domain

**Cons:**
- Single point of failure per workstream
- No knowledge sharing across stack

---

**Option 2: 2 Full-Stack Teams (Recommended for Speed)**

| Team | Members | Epic 2 Focus | Epic 3 Focus |
|------|---------|--------------|--------------|
| Team A | 1 BE + 1 FE | Phase A + Phase B (2.1-2.9) | Gallery + Tagging (3.1-3.4) |
| Team B | 1 BE + 1 FE | Phase C + Testing (2.10-2.14) | Download + Testing (3.5-3.7) |

**Pros:**
- ✅ Faster delivery (teams work in parallel)
- ✅ Knowledge sharing within team
- ✅ Redundancy (backup if someone is blocked)

**Cons:**
- Requires coordination between teams
- More expensive (4 people vs 3)

---

## Risk & Coordination Points

### Integration Risks

| Integration Point | Risk | Mitigation |
|-------------------|------|------------|
| **Phase A → Phase B** | Frontend may not be ready for WebSocket integration | Define API contract early, use mock WebSocket server for frontend dev |
| **Backend pre-signed URLs → Frontend upload** | URL format mismatch or CORS issues | Integration test early (end of Week 2) |
| **Epic 2 Phase A → Epic 3 start** | Gallery UI may depend on upload features not complete | Clarify that Gallery only needs basic photo metadata, not real-time features |
| **WebSocket connection stability** | Dropped connections during load testing | Implement auto-reconnect with exponential backoff from day 1 |

---

### Coordination Meetings

**Daily Standups (15 min)**
- Each stream reports: yesterday's work, today's plan, blockers
- Identify integration points coming up this week

**Weekly Integration Check (30 min)**
- Demo integrated features (backend + frontend working together)
- Verify integration tests passing
- Adjust timeline if needed

**Phase Completion Review (1 hour)**
- At end of Phase A, Phase B, Phase C
- Validate all acceptance criteria met
- Sign off before next phase starts

---

## Execution Checklist

### Epic 2 - Pre-Start Checklist
- [ ] Epic 0 complete (confirmed by sprint-status.yaml)
- [ ] Epic 1 complete (confirmed by sprint-status.yaml)
- [ ] Backend team has reviewed Stories 2.2, 2.3, 2.6, 2.7
- [ ] Frontend team has reviewed Stories 2.1, 2.4, 2.5, 2.8
- [ ] API contracts defined for upload session and pre-signed URLs
- [ ] S3 bucket created and CORS configured (Story 0.1)

### Epic 2 Phase A - Completion Criteria
- [ ] Story 2.1: Photo selection UI works (drag-drop + file picker)
- [ ] Story 2.2: Upload session API returns sessionId
- [ ] Story 2.3: Pre-signed URL API generates valid S3 URLs
- [ ] Story 2.4: Client uploads files to S3 using pre-signed URLs
- [ ] Story 2.5: Progress UI shows upload status per photo
- [ ] Integration test: Upload 10 photos end-to-end passes

### Epic 2 Phase B - Completion Criteria
- [ ] Story 2.6: WebSocket server accepts connections
- [ ] Story 2.7: Backend broadcasts progress to WebSocket topic
- [ ] Story 2.8: Frontend receives and displays WebSocket updates
- [ ] Story 2.9: Completion notification shows with confetti
- [ ] Integration test: WebSocket updates for 100 photos passes

### Epic 2 Phase C - Completion Criteria
- [ ] Story 2.10: Network detection pauses/resumes uploads
- [ ] Story 2.11: User-friendly error messages displayed
- [ ] Story 2.12: Auto-retry with exponential backoff works
- [ ] Story 2.13: Upload cancellation (individual + batch) works
- [ ] Story 2.14: All integration tests pass (100% success rate)
- [ ] Performance benchmark: 100 photos in <90 seconds

### Epic 3 - Pre-Start Checklist
- [ ] Epic 2 Phase A complete (basic upload working)
- [ ] S3 contains uploaded photos for testing
- [ ] CloudFront CDN configured for photo delivery (Story 0.1)
- [ ] Database contains photo metadata (from Story 2.2-2.5)
- [ ] Backend team has reviewed Stories 3.1-3.6 (backend APIs)
- [ ] Frontend team has reviewed Stories 3.1-3.6 (frontend UI)

### Epic 3 - Completion Criteria
- [ ] Story 3.1: Gallery loads with infinite scroll
- [ ] Story 3.2: Lightbox shows fullscreen photo with navigation
- [ ] Story 3.3: Tagging UI allows adding/removing tags
- [ ] Story 3.4: Tag filter shows only tagged photos
- [ ] Story 3.5: Individual photo download works
- [ ] Story 3.6: Batch ZIP download works
- [ ] Story 3.7: All integration tests pass

---

## Summary: Optimal Execution Timeline

**Total MVP Duration: 9 weeks** (with parallelization)

| Epic | Duration | Start Week | End Week | Team Structure |
|------|----------|------------|----------|----------------|
| Epic 0 | ✅ Complete | - | - | - |
| Epic 1 | ✅ Complete | - | - | - |
| Epic 2 | 6 weeks | Week 1 | Week 6 | 3 parallel workstreams |
| Epic 3 | 3 weeks | Week 7 | Week 9 | 2 parallel workstreams |

**Key Success Factors:**
1. ✅ **Start Epic 3 early** (after Epic 2 Phase A complete)
2. ✅ **Parallelize backend and frontend work** within each phase
3. ✅ **Integration tests at end of each phase** prevent surprises
4. ✅ **Clear API contracts defined upfront** reduce coordination delays

---

**Next Steps:**
1. Review this workstreams plan with team
2. Assign team members to workstreams (Backend, Frontend, QA)
3. Create sprint plan for Epic 2 Phase A (Weeks 1-2)
4. Schedule integration checkpoint at end of Week 2

---

_Generated on 2025-11-11 for PicStormAI Development Team_
