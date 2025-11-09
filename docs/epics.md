# RapidPhotoUpload - Epic Breakdown

**Project:** picstormai
**Author:** Reena
**Date:** 2025-11-09
**Project Level:** BMad Method (MVP with Epics)
**Target Scale:** 100 concurrent photo uploads, real-time progress tracking
**GOLD Brief Alignment:** 100% coverage of mandatory requirements

---

## Overview

This epic breakdown transforms the PRD requirements into 4 comprehensive epics with 33 implementation-ready stories. The structure ensures:

- ✅ **100% GOLD Brief Coverage**: All mandatory requirements from GOLD_Teamfront-RapidPhotoUpload.pdf
- ✅ **Architecture Alignment**: DDD, CQRS, VSA patterns implemented throughout
- ✅ **UI/UX Completeness**: Every user journey captured with design system integration
- ✅ **Testing Rigor**: Integration tests for complete client→backend→cloud validation
- ✅ **Zero Placeholders**: Every story is detailed and actionable

### Epic Sequencing

```
Week 1-2:   Epic 0 - Foundation & Infrastructure
Week 3-5:   Epic 1 - Authentication & Onboarding
Week 6-11:  Epic 2 - Core Upload Experience
  ├─ Week 6-7:   Phase A (Basic Upload)
  ├─ Week 8-9:   Phase B (Real-Time)
  └─ Week 10-11: Phase C (Resilience)
Week 12-14: Epic 3 - Gallery, Viewing, Tagging & Download
```

### Technology Stack

- **Backend**: Java 17+ with Spring Boot 3.x, Spring WebFlux, Spring Data JPA
- **Frontend**: React Native for Web (compiles to Web + iOS + Android)
- **Database**: PostgreSQL 15+ with Flyway migrations
- **Cache**: Redis for session management
- **Storage**: AWS S3 with CloudFront CDN
- **Real-time**: STOMP over WebSocket (Spring WebSocket)
- **Infrastructure**: AWS (VPC, ALB, RDS, ElastiCache, S3, CloudFront)

---

## Epic 0: Foundation & Infrastructure

**Goal**: Establish technical foundation for all subsequent development

**Duration**: 2 weeks
**Dependencies**: None (foundational epic)
**Scope**: AWS infrastructure, domain model, CQRS structure, design system, CI/CD

### Why This Epic Matters

Every subsequent epic depends on these foundations:
- Epic 1 needs JWT infrastructure and user domain model
- Epic 2 needs S3 bucket, upload domain model, and WebSocket config
- Epic 3 needs photo domain model and CDN setup

Completing this epic prevents technical debt and rework.

---

### Story 0.1: AWS Infrastructure Setup

**As a** DevOps engineer
**I want to** provision all required AWS resources using Infrastructure as Code
**So that** the application has a secure, scalable cloud environment

#### Acceptance Criteria

**Given** AWS account credentials are configured
**When** I run the infrastructure provisioning script
**Then** all required AWS resources are created and configured

**Resources Created:**
1. VPC with public/private subnets across 2 AZs
2. RDS PostgreSQL 15.x (Multi-AZ)
3. ElastiCache Redis cluster
4. S3 buckets with CORS and lifecycle policies
5. CloudFront CDN distribution
6. Application Load Balancer
7. EC2 Auto Scaling Group

**Prerequisites:** AWS account with admin access, Terraform installed

**Technical Notes:**
- Use Terraform for IaC
- Store secrets in AWS Secrets Manager
- Enable encryption at rest and in transit
- Tag all resources with Environment, Project, ManagedBy

**Testing:**
- Verify all resources created via AWS Console
- Test connectivity: EC2 → RDS, EC2 → Redis
- Test S3 upload and CloudFront delivery
- Validate security groups and network isolation

---

### Story 0.2: Database Schema & Migrations

**As a** backend developer
**I want to** define the complete database schema using Flyway migrations
**So that** the data model supports all application features with referential integrity

#### Acceptance Criteria

**Given** PostgreSQL database is provisioned
**When** I run Flyway migrations
**Then** all tables, indexes, and constraints are created

**Schema Tables:**
1. users (email, password_hash, display_name, email_verified)
2. user_preferences (animations, sound, theme, concurrent_uploads)
3. upload_sessions (user_id, status, total_photos, progress)
4. photos (user_id, session_id, filename, s3_key, upload_status, progress)
5. tags (user_id, name, color)
6. photo_tags (many-to-many)
7. refresh_tokens (token_hash, expires_at, revoked_at)

**Prerequisites:** Story 0.1 completed

**Technical Notes:**
- Use Flyway for migrations (V1__, V2__, etc.)
- Add indexes for common queries (user_id, session_id, tags)
- Enforce foreign key constraints
- Add check constraints (email format, status enums)

**Testing:**
- Verify all tables and indexes created
- Test foreign key constraints
- Test unique constraints (duplicate email)
- Performance test: 10K photo inserts and queries

---

### Story 0.3: Domain Model Implementation (DDD)

**As a** backend developer
**I want to** implement domain aggregates following DDD principles
**So that** business logic is encapsulated and protected

#### Acceptance Criteria

**Given** database schema exists
**When** I implement domain classes
**Then** all aggregates, entities, and value objects are created

**Aggregates:**
1. **User** (root): Email (VO), UserPreferences (entity)
2. **UploadSession** (root): SessionStatus (enum), progress calculations
3. **Photo** (root): PhotoId (VO), S3Location (VO), Tag associations

**Key Business Methods:**
- User: create(), verifyEmail(), recordLogin(), checkPassword()
- UploadSession: start(), recordPhotoUploaded(), complete(), getProgressPercentage()
- Photo: startUpload(), updateProgress(), completeUpload(), failUpload(), retry(), addTag()

**Prerequisites:** Story 0.2 completed

**Technical Notes:**
- No setters - only business methods
- Factory methods for object creation
- Value objects are immutable
- Validate invariants in domain layer

**Testing:**
- Unit tests for all business methods
- Test invariant enforcement
- Test value object validation (invalid email)
- Integration test: save and retrieve from database

---

### Story 0.4: CQRS Structure Setup

**As a** backend developer
**I want to** implement CQRS pattern with command/query handlers
**So that** writes and reads are separated for clarity and scalability

#### Acceptance Criteria

**Given** domain model exists
**When** I implement CQRS structure
**Then** all commands and queries are organized in separate packages

**Commands:**
- RegisterUserCommand, LoginCommand, VerifyEmailCommand
- StartUploadSessionCommand, InitiatePhotoUploadCommand, CompletePhotoUploadCommand
- AddTagToPhotoCommand, RemoveTagFromPhotoCommand

**Queries:**
- GetUserByIdQuery, GetUserPreferencesQuery
- GetUploadSessionQuery, GetActiveSessionsForUserQuery
- GetPhotosForUserQuery, GetPhotosByTagQuery, GetPhotoDetailsQuery

**Prerequisites:** Story 0.3 completed

**Technical Notes:**
- Command handlers: transactional, mutate state, return ID
- Query handlers: read-only, return DTOs (never domain entities)
- Use ApplicationEventPublisher for domain events
- DTOs prevent accidental mutation of domain objects

**Testing:**
- Unit test: RegisterUserCommandHandler
- Unit test: GetPhotosForUserQueryHandler with pagination
- Integration test: Execute command, verify with query
- Performance test: Query 10K photos <500ms

---

### Story 0.5: Design System & Component Library

**As a** frontend developer
**I want to** create a comprehensive design system with React Native components
**So that** UI implementation is consistent and accessible

#### Acceptance Criteria

**Given** UI/UX design system spec exists
**When** I implement component library
**Then** all design tokens and reusable components are available

**Design Tokens:**
- colors (primary, success, error, neutral scales)
- typography (font families, sizes, weights)
- spacing (4px grid: 4, 8, 12, 16, 24, 32...)
- shadows, animations

**Components:**
- **Atoms**: Button, Input, Text, Icon, Badge, Spinner
- **Molecules**: PhotoCard, ProgressBar, UploadStatusIndicator, TagChip, EmptyState
- **Organisms**: PhotoGrid, UploadDashboard, SettingsPanel, Lightbox

**Prerequisites:** UI/UX design system document

**Technical Notes:**
- Atomic Design methodology
- Accessibility: ARIA labels, screen reader support
- Light/dark theme support via context
- Storybook for component documentation
- React Native for Web (compiles to web, iOS, Android)

**Testing:**
- Visual regression tests (Chromatic/Percy)
- Accessibility audit (WCAG 2.1 AA)
- Unit tests for component states
- Storybook stories for all variants

---

### Story 0.6: Infrastructure Integration Tests

**As a** QA engineer
**I want to** validate infrastructure connectivity and configuration
**So that** deployment is confident and reliable

#### Acceptance Criteria

**Given** all infrastructure is provisioned
**When** I run integration tests
**Then** all infrastructure components are verified

**Test Coverage:**
1. **Database**: Connection, migrations, CRUD operations, constraints
2. **Redis**: Connection (PING/PONG), session storage, TTL
3. **S3**: Upload, download, pre-signed URLs, CORS
4. **Health Check**: /actuator/health returns UP for all components

**Prerequisites:** Stories 0.1-0.5 completed

**Technical Notes:**
- Use Testcontainers for Docker-based tests
- LocalStack for S3 testing
- PostgreSQL and Redis containers
- Tests run in CI/CD without external dependencies

**Testing:**
- Database foreign key constraints enforced
- Redis session expiry works correctly
- S3 multipart upload for large files
- End-to-end: Client → Backend → S3 → Database

---

## Epic 1: Authentication & Onboarding

**Goal**: Users can securely access the system and understand key features

**Duration**: 2-3 weeks
**Dependencies**: Epic 0 completed
**Scope**: JWT authentication, registration, login, user settings, onboarding tutorial

---

### Story 1.1: JWT Authentication Backend

**As a** backend developer
**I want to** implement JWT-based authentication with access and refresh tokens
**So that** users can securely authenticate and maintain sessions

#### Acceptance Criteria

**Given** user provides valid credentials
**When** they authenticate
**Then** they receive access token (15min) and refresh token (30 days)

**Given** access token expires
**When** user presents valid refresh token
**Then** they receive new access token without re-login

**Prerequisites:** Story 0.3 (Domain Model), Story 0.4 (CQRS)

**Technical Notes:**
- Spring Security with JWT filter
- Access token: 15 minutes, contains user ID and email
- Refresh token: 30 days, stored in database with hash
- Refresh token rotation on use
- BCrypt password hashing
- JWT secret from environment variable

**Testing:**
- Unit test: LoginCommandHandler with valid/invalid credentials
- Unit test: RefreshTokenCommandHandler
- Integration test: POST /api/auth/login returns tokens
- Integration test: Authenticated endpoint rejects invalid token
- Security test: JWT secret not exposed

---

### Story 1.2: User Registration & Email Verification

**As a** new user
**I want to** register and verify my email address
**So that** I can create a secure account

#### Acceptance Criteria

**Given** I submit registration form (email, password, name)
**When** registration succeeds
**Then** I receive verification email with link

**Given** I click verification link
**When** token is valid and not expired
**Then** my email is verified and I can log in

**Given** I try to register with existing email
**Then** I see error "Email already registered"

**Prerequisites:** Story 1.1 completed

**Technical Notes:**
- Password validation: min 8 chars, uppercase, number
- Email verification token: 32-char secure random
- Token expires in 24 hours
- Send email via AWS SES or SMTP
- HTML email template with styled button

**Testing:**
- Unit test: RegisterUserCommandHandler rejects duplicate email
- Unit test: Password strength validation
- Unit test: VerifyEmailCommandHandler
- Integration test: Full registration → verification flow
- Test: Cannot login with unverified email

---

### Story 1.3: Login/Logout UI (Web + Mobile)

**As a** user
**I want to** log in and log out from a beautiful UI
**So that** I can access my account on any device

#### Acceptance Criteria

**Given** I enter valid credentials
**When** I tap "Log In"
**Then** I navigate to home screen

**Given** I enter invalid credentials
**Then** I see error "Invalid email or password"

**Given** I am logged in
**When** I tap "Log Out"
**Then** I am logged out and returned to login screen

**Prerequisites:** Story 1.1, Story 1.2, Story 0.5 (Design System)

**Technical Notes:**
- React Native for Web (single codebase)
- useAuth hook for state management
- AsyncStorage for token persistence
- Axios interceptor: auto-refresh on 401
- Form validation: email format, required fields
- Keyboard handling (KeyboardAvoidingView)
- Password visibility toggle

**Testing:**
- Unit test: useAuth hook login success/failure
- Unit test: Axios interceptor refreshes token
- E2E test: Login navigates to home
- E2E test: Logout clears tokens
- Accessibility test: Screen reader navigation

---

### Story 1.4: User Settings Panel

**As a** user
**I want to** customize my app preferences
**So that** the app behaves according to my preferences

#### Acceptance Criteria

**Given** I open settings
**When** I view preferences
**Then** I see toggles for:
- Enable animations (default: true)
- Enable sound effects (default: true)
- Theme: light/dark/auto (default: light)
- Concurrent uploads: 1-20 (default: 10)
- Auto-retry failed uploads (default: true)

**Given** I change a setting
**When** I save
**Then** preference is persisted and applied immediately

**Prerequisites:** Story 1.3 completed

**Technical Notes:**
- UpdateUserPreferencesCommand
- Store in user_preferences table
- React context for global preference access
- Slider for concurrent uploads count
- Segmented control for theme
- Changes apply without app restart

**Testing:**
- Unit test: UpdateUserPreferencesCommandHandler
- Integration test: Save preferences, verify in database
- UI test: Toggle setting, verify change persisted
- Test: Preferences sync across app restart

---

### Story 1.5: Onboarding Tutorial (First-Time Users)

**As a** first-time user
**I want to** see an interactive tutorial
**So that** I understand how to use the app

#### Acceptance Criteria

**Given** I just registered and logged in for first time
**When** I reach home screen
**Then** I see 3-screen onboarding carousel

**Onboarding Screens:**
1. "Upload 100 photos at once" - Animation showing batch upload
2. "Real-time progress tracking" - Animation showing progress bars
3. "Organize with tags" - Animation showing tagging photos

**Given** I complete onboarding
**Then** I never see it again (unless I reset in settings)

**Prerequisites:** Story 1.3 completed

**Technical Notes:**
- Check user.hasSeenOnboarding flag
- React Native carousel (react-native-snap-carousel)
- Lottie animations for illustrations
- Skip button on each screen
- "Get Started" button on final screen
- UpdateUserFlagCommand(hasSeenOnboarding=true)

**Testing:**
- E2E test: First login shows onboarding
- E2E test: Second login skips onboarding
- Test: Skip button dismisses immediately
- Test: Reset onboarding in settings works

---

### Story 1.6: Authentication Integration Tests

**As a** QA engineer
**I want to** validate complete authentication flows
**So that** all auth scenarios are tested end-to-end

#### Acceptance Criteria

**Given** authentication system is complete
**When** I run integration tests
**Then** all auth flows are validated

**Test Coverage:**
1. Registration → Email verification → Login
2. JWT token generation and validation
3. Refresh token rotation
4. Login with unverified email (rejected)
5. Duplicate email registration (rejected)
6. Weak password validation
7. Logout revokes tokens
8. Expired token returns 401
9. Auto-refresh on 401 response

**Prerequisites:** Stories 1.1-1.5 completed

**Technical Notes:**
- End-to-end tests with Testcontainers
- Mock email service for verification
- Test JWT expiry with time manipulation
- Test refresh token revocation
- Security tests: SQL injection, XSS

**Testing:**
- All integration tests pass
- Security tests pass (OWASP Top 10)
- Load test: 1000 concurrent logins

---

## Epic 2: Core Upload Experience

**Goal**: Users can reliably upload 100 photos with real-time progress visibility

**Duration**: 6 weeks
**Dependencies**: Epic 0, Epic 1 completed
**Scope**: Batch upload, S3 direct upload, WebSocket real-time updates, network resilience

### Phase A: Basic Upload (Weeks 1-2)

---

### Story 2.1: Photo Selection & Validation UI

**As a** user
**I want to** select up to 100 photos via drag-drop or file picker
**So that** I can prepare them for upload

#### Acceptance Criteria

**Given** I am on upload screen
**When** I drag photos onto dropzone OR click "Select Photos"
**Then** selected photos appear in grid with thumbnails

**Given** I select more than 100 photos
**Then** I see error "Maximum 100 photos per upload"

**Given** I select non-image file
**Then** I see error "Only image files allowed (JPG, PNG, GIF, WebP)"

**Given** I select photo over 50MB
**Then** I see warning "Large files may take longer to upload"

**Prerequisites:** Story 0.5 (Design System), Story 1.3 (Login)

**Technical Notes:**
- React Native file picker (react-native-document-picker)
- Web: HTML5 drag-drop API
- Client-side validation: file type, count, size
- Generate client-side thumbnails (Canvas API / React Native Image)
- Supported formats: image/jpeg, image/png, image/gif, image/webp
- Max 100 photos per session
- Individual file size limit: 50MB

**Testing:**
- E2E test: Select 10 photos, verify thumbnails
- Test: Select 101 photos, see error
- Test: Select PDF file, see error
- Test: Drag-drop photos (web only)
- Accessibility test: Keyboard navigation

---

### Story 2.2: Start Upload Session (Backend)

**As a** backend developer
**I want to** create upload session when user starts batch upload
**So that** progress can be tracked across all photos

#### Acceptance Criteria

**Given** user initiates upload of N photos
**When** StartUploadSessionCommand is executed
**Then** UploadSession aggregate is created with status=ACTIVE

**Session tracks:**
- Total photos count
- Total size (bytes)
- Uploaded photos count
- Failed photos count
- Progress percentage

**Prerequisites:** Story 0.3 (Domain Model), Story 0.4 (CQRS)

**Technical Notes:**
- POST /api/upload/sessions
- Request body: {totalPhotos: 50, totalSizeBytes: 104857600}
- Response: {sessionId: "uuid", status: "ACTIVE"}
- Store in upload_sessions table
- Return session ID to client

**Testing:**
- Unit test: StartUploadSessionCommandHandler
- Integration test: POST /api/upload/sessions creates session
- Test: Reject if totalPhotos > 100
- Test: Calculate initial progress (0%)

---

### Story 2.3: S3 Pre-Signed URL Generation

**As a** backend developer
**I want to** generate S3 pre-signed URLs for direct client uploads
**So that** files upload directly to S3 without passing through backend

#### Acceptance Criteria

**Given** user starts photo upload
**When** InitiatePhotoUploadCommand is executed
**Then** S3 pre-signed PUT URL is generated (valid 15 minutes)

**For files >5MB:**
- Generate multipart upload ID
- Return pre-signed URLs for each part

**Prerequisites:** Story 0.1 (AWS S3), Story 2.2 (Upload Session)

**Technical Notes:**
- POST /api/upload/sessions/{sessionId}/photos/initiate
- Request: {filename, fileSizeBytes, mimeType}
- Response: {photoId, uploadUrl, s3Key} OR {photoId, uploadId, partUrls[]}
- S3 key format: uploads/{userId}/{sessionId}/{photoId}-{filename}
- Pre-signed URL expires in 15 minutes
- For >5MB files: Use S3 multipart upload (5MB parts)
- Return array of pre-signed URLs for each part

**Testing:**
- Unit test: InitiatePhotoUploadCommandHandler
- Integration test: Generate pre-signed URL, verify signature
- Test: Multipart for 10MB file (2 parts)
- Test: URL expires after 15 minutes

---

### Story 2.4: Client-Side Upload Engine

**As a** frontend developer
**I want to** upload photos directly to S3 using pre-signed URLs
**So that** uploads are fast and don't burden backend

#### Acceptance Criteria

**Given** I have pre-signed URL for photo
**When** I PUT file to S3 URL
**Then** file uploads directly to S3

**Given** file is >5MB
**When** I upload via multipart
**Then** file is split into 5MB chunks and uploaded in parallel (max 3 concurrent chunks)

**Upload concurrency:**
- Max 10 photos uploading simultaneously (configurable in user settings)
- Photos queued if limit exceeded

**Prerequisites:** Story 2.3 (Pre-signed URLs)

**Technical Notes:**
- Use axios for uploads with progress tracking
- For multipart: Upload chunks in parallel, then complete multipart upload
- Retry failed chunks (max 3 retries per chunk)
- Track upload progress per photo (bytes uploaded / total bytes)
- Emit progress events for UI updates

**Testing:**
- Unit test: Upload manager queues 20 photos, uploads 10 concurrently
- Test: Multipart upload for 10MB file
- Test: Progress tracking for each photo
- Test: Retry failed chunk

---

### Story 2.5: Upload Progress UI

**As a** user
**I want to** see real-time progress for each photo and overall batch
**So that** I know how the upload is progressing

#### Acceptance Criteria

**Given** photos are uploading
**When** I view upload dashboard
**Then** I see:
- Overall progress bar (e.g., "45/100 photos uploaded - 67%")
- Grid of photo cards with individual progress bars
- Estimated time remaining

**Photo states:**
- Queued (gray, no progress bar)
- Uploading (blue progress bar animating)
- Completed (green checkmark)
- Failed (red X icon)

**Prerequisites:** Story 2.4 (Upload Engine), Story 0.5 (Design System)

**Technical Notes:**
- UploadDashboard organism component
- PhotoCard molecule shows individual progress
- ProgressBar molecule for overall progress
- Calculate ETA: (remaining bytes) / (average upload speed)
- Update UI every 500ms (throttled)
- Use React state for upload status

**Testing:**
- E2E test: Upload 10 photos, verify progress updates
- Test: Individual photo progress displays correctly
- Test: Overall progress aggregates correctly
- Test: ETA calculation is reasonable
- Accessibility test: Progress announced to screen readers

---

### Phase B: Real-Time Updates (Weeks 3-4)

---

### Story 2.6: WebSocket Server Setup

**As a** backend developer
**I want to** configure WebSocket server with STOMP protocol
**So that** real-time progress updates can be pushed to clients

#### Acceptance Criteria

**Given** Spring WebSocket is configured
**When** client connects to ws://api/ws
**Then** WebSocket handshake succeeds

**Topic structure:**
- `/topic/upload-sessions/{sessionId}` - Session-level updates
- `/user/queue/notifications` - User-specific notifications

**Prerequisites:** Story 0.1 (Infrastructure)

**Technical Notes:**
- Spring WebSocket with STOMP over SockJS
- Configuration class with @EnableWebSocketMessageBroker
- MessageBrokerRegistry: simple in-memory broker (or Redis for scale)
- Enable SockJS fallback for old browsers
- Authentication: Intercept STOMP CONNECT with JWT token
- Allow origins: Web and mobile clients

**Testing:**
- Integration test: WebSocket connection succeeds
- Test: Subscribe to /topic/upload-sessions/{id}
- Test: Receive message on topic
- Test: Unauthenticated connection rejected

---

### Story 2.7: Real-Time Progress Broadcasting

**As a** backend developer
**I want to** broadcast upload progress to WebSocket subscribers
**So that** clients receive real-time updates without polling

#### Acceptance Criteria

**Given** photo upload completes
**When** CompletePhotoUploadCommand is executed
**Then** progress message is broadcast to WebSocket topic

**Message format:**
```json
{
  "type": "PHOTO_UPLOADED",
  "sessionId": "uuid",
  "photoId": "uuid",
  "uploadedCount": 47,
  "totalCount": 100,
  "progressPercent": 47.0
}
```

**Prerequisites:** Story 2.6 (WebSocket Server)

**Technical Notes:**
- Use SimpMessagingTemplate to broadcast
- Broadcast on: photo uploaded, photo failed, session completed
- Message types: PHOTO_UPLOADED, PHOTO_FAILED, SESSION_COMPLETED
- Include timestamp for ordering
- Broadcast after database commit (TransactionalEventListener)

**Testing:**
- Integration test: Complete photo upload, verify WebSocket message received
- Test: Multiple subscribers receive message
- Test: Message includes correct progress data
- Load test: 1000 concurrent WebSocket connections

---

### Story 2.8: WebSocket Client Integration

**As a** frontend developer
**I want to** connect to WebSocket and receive real-time progress updates
**So that** UI updates without polling

#### Acceptance Criteria

**Given** I start upload session
**When** I subscribe to WebSocket topic
**Then** I receive real-time progress messages

**Given** I receive PHOTO_UPLOADED message
**Then** UI updates to show photo as completed with animation

**Given** WebSocket disconnects
**When** connection is restored
**Then** I re-subscribe and sync current state

**Prerequisites:** Story 2.7 (WebSocket Broadcasting)

**Technical Notes:**
- Use @stomp/stompjs for WebSocket client
- SockJS for fallback
- Connect with JWT token in STOMP headers
- Subscribe to /topic/upload-sessions/{sessionId}
- Handle reconnection with exponential backoff
- Sync state on reconnect (fetch latest progress from REST API)

**Testing:**
- E2E test: Upload photo, receive WebSocket update
- Test: WebSocket disconnects, reconnects, re-subscribes
- Test: Progress updates reflected in UI
- Test: Multiple tabs receive updates

---

### Story 2.9: Upload Completion Notification

**As a** user
**I want to** see a celebration notification when upload completes
**So that** I feel rewarded and know upload succeeded

#### Acceptance Criteria

**Given** all photos finish uploading
**When** final photo completes
**Then** I see:
- Modal overlay with "Upload Complete! 🎉"
- Confetti animation
- Success sound effect (if enabled in settings)
- Button "View Photos"

**Given** some photos failed
**Then** I see:
- "Upload completed with X failures"
- Button "Retry Failed" and "View Photos"

**Prerequisites:** Story 2.8 (WebSocket Client), Story 1.4 (User Settings)

**Technical Notes:**
- Trigger modal when session status changes to COMPLETED
- Confetti animation: react-native-confetti-cannon or web canvas
- Sound effect: play from local asset if user settings allow
- Haptic feedback on mobile (Vibration API)
- Auto-dismiss after 5 seconds OR user clicks button

**Testing:**
- E2E test: Upload completes, see confetti
- Test: Sound plays if enabled in settings
- Test: Partial failure shows retry button
- Accessibility test: Screen reader announces completion

---

### Phase C: Network Resilience (Weeks 5-6)

---

### Story 2.10: Network Loss Detection

**As a** user
**I want to** be notified immediately when network connection is lost
**So that** I understand why uploads paused

#### Acceptance Criteria

**Given** uploads are in progress
**When** network connection is lost
**Then** within 5 seconds I see banner "Connection lost. Uploads paused."

**Given** connection is restored
**When** network is back online
**Then** I see banner "Connection restored. Resuming uploads."
**And** uploads automatically resume

**Prerequisites:** Story 2.4 (Upload Engine)

**Technical Notes:**
- Use NetInfo library (@react-native-community/netinfo)
- Subscribe to network state changes
- Detect: navigator.onLine (web) or NetInfo.fetch() (mobile)
- Pause upload queue when offline
- Resume queue when online
- Show persistent banner at top of screen (dismissible)

**Testing:**
- E2E test: Simulate network loss, verify banner appears
- Test: Network restored, uploads resume
- Test: Detection happens <5 seconds
- Test: WebSocket reconnects when network restored

---

### Story 2.11: Upload Error Handling & User-Friendly Messages

**As a** user
**I want to** see clear error messages when uploads fail
**So that** I understand what went wrong and can fix it

#### Acceptance Criteria

**Given** photo upload fails
**When** error occurs
**Then** I see user-friendly error message (not technical stack trace)

**Error scenarios:**
- Network timeout → "Network issue. Upload will retry automatically."
- S3 permission denied → "Upload failed. Please contact support."
- File too large → "File exceeds 50MB limit."
- Invalid file type → "Only image files are supported."

**Prerequisites:** Story 2.4 (Upload Engine)

**Technical Notes:**
- Map technical errors to user messages:
  - 403 Forbidden → "Upload not authorized"
  - 408 Timeout → "Network timeout. Retrying..."
  - 413 Payload Too Large → "File too large"
  - 500 Server Error → "Server error. Please try again."
- Display error in PhotoCard with retry button
- Log full error to backend for debugging
- Show error toast notification

**Testing:**
- Test: Timeout error shows retry message
- Test: 403 error shows authorization message
- Test: 500 error logs to backend
- Test: User can dismiss error and retry

---

### Story 2.12: Upload Retry & Resume

**As a** user
**I want to** automatically retry failed uploads and resume interrupted ones
**So that** I don't lose progress from temporary failures

#### Acceptance Criteria

**Given** photo upload fails
**When** error is network-related
**Then** upload retries automatically (max 3 retries, exponential backoff)

**Given** I close app during upload
**When** I reopen app
**Then** I see option "Resume 15 pending uploads?"

**Given** upload is interrupted mid-file
**When** I resume
**Then** upload resumes from last completed chunk (multipart)

**Prerequisites:** Story 2.4 (Upload Engine), Story 2.11 (Error Handling)

**Technical Notes:**
- Retry strategy: 1s, 2s, 4s exponential backoff
- Max 3 retries per photo
- Store pending uploads in AsyncStorage (mobile) or IndexedDB (web)
- For multipart uploads: Track completed parts, resume from next part
- Manual retry button on failed photos
- Bulk "Retry All Failed" button in dashboard

**Testing:**
- Test: Network error triggers auto-retry
- Test: 3 retries exhaust, marked as failed
- Test: Close app during upload, reopen shows resume prompt
- Test: Resume from last completed multipart part
- E2E test: Upload 20 photos with 5 intermittent failures, all eventually succeed

---

### Story 2.13: Upload Cancellation

**As a** user
**I want to** cancel in-progress uploads (individual or entire batch)
**So that** I can stop unwanted uploads and free up bandwidth

#### Acceptance Criteria

**Given** photos are uploading
**When** I click "Cancel" button on individual photo
**Then** that specific upload is cancelled immediately
**And** partial upload is cleaned up from S3
**And** cancelled photo is removed from queue

**Given** I have 23 uploads in progress
**When** I click "Cancel All" button in dashboard header
**Then** I see confirmation modal: "Cancel all 23 remaining uploads?"
**And** if I confirm, all uploads stop immediately
**And** all partial uploads are cleaned up

**Given** I cancel an upload that's 50% complete
**Then** the incomplete file is deleted from S3
**And** metadata is NOT saved to database
**And** upload session count is updated (failed_photos++)

**Prerequisites:** Story 2.5 (Upload Progress UI), Story 2.11 (Error Handling)

**Technical Notes:**
- Individual cancel: X icon on PhotoCard with upload progress
- Batch cancel: "Cancel All" button in UploadDashboard header
- Confirmation modal for batch cancel (prevents accidental clicks)
- Backend: DELETE /api/upload/sessions/{sessionId}/photos/{photoId}/cancel
- Backend: POST /api/upload/sessions/{sessionId}/cancel (cancel all)
- S3 cleanup: AbortMultipartUpload for in-progress multipart uploads
- Update UploadSession aggregate: increment failedPhotos count
- CancelPhotoUploadCommand and CancelUploadSessionCommand handlers

**Testing:**
- Unit test: CancelPhotoUploadCommandHandler cancels upload
- Integration test: Cancel upload, verify S3 object deleted
- E2E test: Cancel individual upload from UI
- E2E test: Cancel all uploads with confirmation modal
- Test: Cannot cancel completed upload

---

### Story 2.14: Upload Integration Tests

**As a** QA engineer
**I want to** validate complete upload flow from client to cloud
**So that** critical upload path is tested end-to-end

#### Acceptance Criteria

**Given** upload system is complete
**When** I run integration tests
**Then** all upload scenarios are validated

**Test Coverage:**
1. **Basic Upload**: 10 photos → S3 → Database metadata
2. **Concurrent Upload**: 100 photos uploaded with 10 concurrent connections
3. **Multipart Upload**: 20MB file uploaded in chunks
4. **Performance**: 100 photos (2MB each) complete in <90 seconds
5. **WebSocket**: Progress updates received within 500ms
6. **Network Resilience**: Upload paused on disconnect, resumed on reconnect
7. **Retry Logic**: Failed upload retries 3 times, then marked failed
8. **Resume**: Interrupted upload resumes from correct chunk
9. **Cancellation**: Individual and batch cancel work correctly

**Prerequisites:** Stories 2.1-2.13 completed

**Technical Notes:**
- Use Testcontainers for PostgreSQL, Redis, LocalStack (S3)
- Mock network failures with proxy (Toxiproxy)
- Simulate file uploads with generated binary data
- Measure upload time for performance benchmarks
- Verify S3 objects exist and match uploaded data
- Verify database metadata matches S3 objects

**Testing:**
- All integration tests pass in CI/CD
- Performance benchmark: <90 seconds for 100 photos
- Load test: 50 concurrent users uploading
- Chaos test: Random network interruptions, all uploads eventually complete

---

## Epic 3: Photo Gallery, Viewing, Tagging & Download

**Goal**: Users can browse, view, organize, and download photos

**Duration**: 3 weeks
**Dependencies**: Epic 0, Epic 1, Epic 2 completed
**Scope**: Photo gallery with infinite scroll, lightbox viewer, tagging, download

---

### Story 3.1: Photo Gallery UI with Infinite Scroll

**As a** user
**I want to** view my uploaded photos in a responsive grid
**So that** I can browse my photo collection

#### Acceptance Criteria

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

### Story 3.2: Photo Viewing - Lightbox

**As a** user
**I want to** view individual photos in fullscreen lightbox
**So that** I can see details and navigate between photos

#### Acceptance Criteria

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

### Story 3.3: Photo Tagging UI

**As a** user
**I want to** add tags to photos for organization
**So that** I can find photos by category later

#### Acceptance Criteria

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

### Story 3.4: Tag Filter & Search

**As a** user
**I want to** filter photos by tag
**So that** I can find specific photos quickly

#### Acceptance Criteria

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

### Story 3.5: Photo Download (Individual)

**As a** user
**I want to** download individual photos to my device
**So that** I can save photos locally or share them

#### Acceptance Criteria

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

### Story 3.6: Batch Photo Download (ZIP)

**As a** user
**I want to** download multiple selected photos as ZIP
**So that** I can save or share many photos at once

#### Acceptance Criteria

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

### Story 3.7: Gallery Integration Tests

**As a** QA engineer
**I want to** validate complete gallery and tagging flows
**So that** critical user journeys are tested end-to-end

#### Acceptance Criteria

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

## Epic Completion Checklist

### Epic 0: Foundation ✅
- [ ] Story 0.1: AWS Infrastructure
- [ ] Story 0.2: Database Schema
- [ ] Story 0.3: Domain Model (DDD)
- [ ] Story 0.4: CQRS Structure
- [ ] Story 0.5: Design System
- [ ] Story 0.6: Infrastructure Integration Tests

### Epic 1: Authentication ✅
- [ ] Story 1.1: JWT Authentication Backend
- [ ] Story 1.2: User Registration & Email Verification
- [ ] Story 1.3: Login/Logout UI
- [ ] Story 1.4: User Settings Panel
- [ ] Story 1.5: Onboarding Tutorial
- [ ] Story 1.6: Authentication Integration Tests

### Epic 2: Upload Experience ✅
- [ ] Story 2.1: Photo Selection & Validation UI
- [ ] Story 2.2: Start Upload Session (Backend)
- [ ] Story 2.3: S3 Pre-Signed URL Generation
- [ ] Story 2.4: Client-Side Upload Engine
- [ ] Story 2.5: Upload Progress UI
- [ ] Story 2.6: WebSocket Server Setup
- [ ] Story 2.7: Real-Time Progress Broadcasting
- [ ] Story 2.8: WebSocket Client Integration
- [ ] Story 2.9: Upload Completion Notification
- [ ] Story 2.10: Network Loss Detection
- [ ] Story 2.11: Upload Error Handling
- [ ] Story 2.12: Upload Retry & Resume
- [ ] Story 2.13: Upload Cancellation
- [ ] Story 2.14: Upload Integration Tests

### Epic 3: Gallery & Download ✅
- [ ] Story 3.1: Photo Gallery UI with Infinite Scroll
- [ ] Story 3.2: Photo Viewing - Lightbox
- [ ] Story 3.3: Photo Tagging UI
- [ ] Story 3.4: Tag Filter & Search
- [ ] Story 3.5: Photo Download (Individual)
- [ ] Story 3.6: Batch Photo Download (ZIP)
- [ ] Story 3.7: Gallery Integration Tests

---

## GOLD Brief Requirements Coverage

| Requirement | Epic | Stories | Status |
|-------------|------|---------|--------|
| 100 concurrent uploads | Epic 2 | 2.2, 2.4, 2.13 | ✅ Covered |
| Asynchronous UI | Epic 2 | 2.5, 2.6-2.8 | ✅ Covered |
| Real-time progress | Epic 2 | 2.5, 2.7, 2.8 | ✅ Covered |
| Web viewing & tagging & downloading | Epic 3 | 3.1-3.6 | ✅ Covered |
| Mobile upload & viewing | All | React Native for Web | ✅ Covered |
| Backend concurrency & streaming | Epic 2 | 2.3, 2.4 (multipart) | ✅ Covered |
| JWT Authentication | Epic 1 | 1.1, 1.2 | ✅ Covered |
| DDD, CQRS, VSA | Epic 0 | 0.3, 0.4 | ✅ Covered |
| Spring Boot + React + PostgreSQL + S3 | Epic 0 | 0.1, 0.2, 0.5 | ✅ Covered |
| <90s performance (100 photos) | Epic 2 | 2.13 (integration test) | ✅ Covered |
| UI responsiveness | Epic 2 | 2.5, NFR-P2 | ✅ Covered |
| Integration tests (client→backend→cloud) | All | 0.6, 1.6, 2.13, 3.7 | ✅ Covered |

---

## Next Steps

1. **Shard this document** (optional): Run `/bmad:core:tools:shard-doc` to split into separate epic files
2. **Create individual stories**: Run `/bmad:bmm:workflows:create-story` to generate story markdown files
3. **Sprint planning**: Run `/bmad:bmm:workflows:sprint-planning` to create sprint tracking
4. **Start development**: Run `/bmad:bmm:workflows:dev-story` to implement first story

---

_Generated by BMAD Method on 2025-11-09_
_For implementation, use BMAD workflows to convert epics into executable stories_
