# Epic 0: Foundation & Infrastructure

**Goal**: Establish technical foundation for all subsequent development

**Duration**: 2 weeks
**Dependencies**: None (foundational epic)
**Scope**: AWS infrastructure, domain model, CQRS structure, design system, CI/CD

## Why This Epic Matters

Every subsequent epic depends on these foundations:
- Epic 1 needs JWT infrastructure and user domain model
- Epic 2 needs S3 bucket, upload domain model, and WebSocket config
- Epic 3 needs photo domain model and CDN setup

Completing this epic prevents technical debt and rework.

---

## Story 0.1: AWS Infrastructure Setup

**As a** DevOps engineer
**I want to** provision all required AWS resources using Infrastructure as Code
**So that** the application has a secure, scalable cloud environment

### Acceptance Criteria

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

## Story 0.2: Database Schema & Migrations

**As a** backend developer
**I want to** define the complete database schema using Flyway migrations
**So that** the data model supports all application features with referential integrity

### Acceptance Criteria

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

## Story 0.3: Domain Model Implementation (DDD)

**As a** backend developer
**I want to** implement domain aggregates following DDD principles
**So that** business logic is encapsulated and protected

### Acceptance Criteria

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

## Story 0.4: CQRS Structure Setup

**As a** backend developer
**I want to** implement CQRS pattern with command/query handlers
**So that** writes and reads are separated for clarity and scalability

### Acceptance Criteria

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

## Story 0.5: Design System & Component Library

**As a** frontend developer
**I want to** create a comprehensive design system with React Native components
**So that** UI implementation is consistent and accessible

### Acceptance Criteria

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

## Story 0.6: Infrastructure Integration Tests

**As a** QA engineer
**I want to** validate infrastructure connectivity and configuration
**So that** deployment is confident and reliable

### Acceptance Criteria

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
