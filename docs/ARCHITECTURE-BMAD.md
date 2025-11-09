# Technical Architecture Document
# RapidPhotoUpload - BMAD Architecture Framework

**Version:** 1.0
**Status:** Draft for Architecture Review
**Last Updated:** 2025-11-09
**Methodology:** BMAD (Business, Market, Architecture, Development)
**Related Documents:** PRD-RapidPhotoUpload.md v1.2

---

## Document Control

| Role | Name | Date | Signature |
|------|------|------|-----------|
| **Solution Architect** | [Name] | [Date] | [Signature] |
| **Technical Lead** | [Name] | [Date] | [Signature] |
| **Backend Lead** | [Name] | [Date] | [Signature] |
| **Frontend Lead** | [Name] | [Date] | [Signature] |
| **DevOps Lead** | [Name] | [Date] | [Signature] |

**Review History:**

| Version | Date | Reviewer | Changes |
|---------|------|----------|---------|
| 1.0 | 2025-11-09 | Architecture Team | Initial BMAD architecture document |

---

## Table of Contents

### Part I: Business Architecture (B)
1. [Business Drivers & Constraints](#1-business-drivers--constraints)
2. [Architecture Success Metrics](#2-architecture-success-metrics)
3. [Business Capabilities Mapping](#3-business-capabilities-mapping)
4. [Technical Risk & ROI](#4-technical-risk--roi)

### Part II: Market Architecture (M)
5. [Competitive Technical Landscape](#5-competitive-technical-landscape)
6. [Technology Positioning](#6-technology-positioning)
7. [Scalability Roadmap](#7-scalability-roadmap)

### Part III: Architecture (A)
8. [Architecture Principles & Patterns](#8-architecture-principles--patterns)
9. [System Context & Boundaries](#9-system-context--boundaries)
10. [Domain-Driven Design (DDD)](#10-domain-driven-design-ddd)
11. [CQRS Implementation](#11-cqrs-implementation)
12. [Vertical Slice Architecture (VSA)](#12-vertical-slice-architecture-vsa)
13. [Component Architecture](#13-component-architecture)
14. [Data Architecture](#14-data-architecture)
15. [Integration Architecture](#15-integration-architecture)
16. [Security Architecture](#16-security-architecture)
17. [Infrastructure Architecture](#17-infrastructure-architecture)

### Part IV: Development (D)
18. [Development Workflow](#18-development-workflow)
19. [Code Organization](#19-code-organization)
20. [Testing Strategy](#20-testing-strategy)
21. [Deployment Pipeline](#21-deployment-pipeline)
22. [Monitoring & Observability](#22-monitoring--observability)
23. [Operational Excellence](#23-operational-excellence)

---

# Part I: Business Architecture (B)

## 1. Business Drivers & Constraints

### 1.1 Primary Business Driver

**Business Goal:** Demonstrate architectural excellence in handling high-concurrency media uploads while maintaining exceptional user experience.

**Architectural Implication:**
- **Reactive, non-blocking architecture** (Spring WebFlux) is mandatory
- **Direct-to-cloud upload pattern** to bypass server bottleneck
- **Event-driven architecture** for real-time progress updates

### 1.2 Non-Negotiable Constraints

#### Performance Constraints
| Constraint | Value | Architectural Impact |
|------------|-------|---------------------|
| **Concurrent uploads** | 100 photos per session | Requires non-blocking I/O, connection pooling, WebSocket scalability |
| **Upload time** | <90 seconds for 200MB | Direct-to-S3 uploads, multipart for files >5MB, optimized networking |
| **UI responsiveness** | <100ms during uploads | Web Workers (web), async threads (React Native), async state management |

**Architecture Decision:** Spring WebFlux (reactive) over Spring MVC (thread-per-request)
- **Rationale:** WebFlux handles 100+ concurrent connections with ~10 threads vs MVC requiring 100+ threads
- **Trade-off:** Increased complexity (reactive programming) for 10× better concurrency

#### Mandatory Patterns
| Pattern | Business Justification | Architectural Benefit |
|---------|------------------------|----------------------|
| **Domain-Driven Design** | Demonstrates mastery of complex domain modeling | Encapsulates upload session logic, clear bounded contexts |
| **CQRS** | Separates high-throughput writes from query loads | Optimizes upload completion (write) vs gallery browsing (read) |
| **Vertical Slice Architecture** | Feature-based organization for rapid iteration | Each upload feature is self-contained, easy to test/deploy |

### 1.3 Business Capabilities Enabled by Architecture

```
Business Capability Map:

User Onboarding
├── [FR-001] JWT Authentication → Stateless, scalable auth
├── [FR-002] User Registration → Async email verification
└── [FR-021] Tutorial System → Client-side state management

High-Volume Upload
├── [FR-004] Batch Upload → Direct-to-S3 with pre-signed URLs
├── [FR-006] Real-Time Progress → WebSocket push architecture
├── [FR-018] Network Resilience → Multipart uploads, S3 resume
└── [FR-013] Cloud Storage → S3 event-driven processing

Photo Organization
├── [FR-011] Gallery Display → CDN-backed, paginated queries
├── [FR-019] Tagging System → Normalized database, indexed searches
└── [FR-012] Lightbox Viewer → Lazy loading, client-side caching
```

**Key Insight:** Each business capability maps to specific architectural patterns, ensuring technical decisions serve business needs.

---

## 2. Architecture Success Metrics

### 2.1 Technical KPIs (Aligned with Business Goals)

| Metric | Target | Measurement Method | Architecture Component |
|--------|--------|-------------------|----------------------|
| **Concurrent User Support** | 1000 simultaneous users | Load testing with Gatling | Spring WebFlux event loop, Redis session store |
| **Upload Success Rate** | >90% | CloudWatch metrics, application logs | S3 multipart uploads, exponential backoff retry |
| **P95 Upload Latency** | <90 seconds | CloudWatch custom metrics | Direct-to-S3, pre-signed URL generation time |
| **WebSocket Connection Stability** | >99% uptime | WebSocket health checks | STOMP/WebSocket broker, connection pooling |
| **Database Query Performance** | <500ms for gallery queries | PostgreSQL slow query log | Read replicas, database indexing strategy |
| **API Response Time (P95)** | <200ms | Application Performance Monitoring (APM) | Reactive handlers, R2DBC connection pool |

### 2.2 Architecture Quality Attributes

**ISO 25010 Quality Model:**

| Quality Attribute | Priority | Architectural Strategy |
|-------------------|----------|----------------------|
| **Performance** | CRITICAL | Reactive I/O, direct-to-cloud uploads, CDN delivery |
| **Reliability** | CRITICAL | Automatic retries, circuit breakers, multipart resume |
| **Scalability** | HIGH | Horizontal scaling (stateless services), database read replicas |
| **Maintainability** | HIGH | Vertical Slice Architecture, bounded contexts (DDD) |
| **Security** | CRITICAL | JWT tokens (RS256), pre-signed URLs (15-min expiry), encryption at rest |
| **Usability** | HIGH | <100ms UI response, real-time feedback (WebSocket) |

---

## 3. Business Capabilities Mapping

### 3.1 Capability-to-Component Matrix

```
Business Capability          | Domain Services          | Infrastructure        | Frontend Components
-----------------------------|--------------------------|----------------------|--------------------
User Authentication          | UserAuthService          | JWT, Redis sessions  | LoginForm, AuthGuard
Batch Upload Initiation      | UploadSessionService     | S3 pre-signed URLs   | UploadDropzone
Real-Time Progress Tracking  | UploadProgressService    | WebSocket broker     | ProgressBar, StatusBadge
Network Resilience           | UploadResumeService      | S3 multipart API     | NetworkIndicator
Photo Gallery Browsing       | PhotoQueryService        | PostgreSQL + CDN     | PhotoGrid, InfiniteScroll
Photo Tagging                | TaggingService           | PostgreSQL (tags)    | TagInput, TagChip
```

### 3.2 Critical User Journeys → Architecture Mapping

**Journey 1: First-Time Upload (Sarah - Professional Photographer)**

```
User Action                    | Architecture Layer              | Technology
-------------------------------|--------------------------------|---------------------------
Login → POST /auth/login       | AuthController (VSA)           | Spring WebFlux, JWT
Navigate to upload page        | React Router                   | Client-side routing
Select 100 photos              | Browser File API               | HTML5 File API
Initiate upload                | UploadCommandService (CQRS)    | Command handler
Get pre-signed URLs (100x)     | S3PresignedUrlGenerator        | AWS SDK S3Presigner
Upload to S3 (parallel)        | Browser XMLHttpRequest         | Direct to S3 (bypass server)
Track progress                 | WebSocket /ws/upload-progress  | STOMP protocol
Complete session               | UploadSessionCompletedEvent    | Domain event (DDD)
View gallery                   | PhotoQueryService (CQRS)       | Read-optimized query
```

**Architectural Insight:** User journey maps directly to architectural components, validating that every user action has a clear technical implementation.

---

## 4. Technical Risk & ROI

### 4.1 Architecture Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy | Architecture Component |
|------|-------------|--------|-------------------|----------------------|
| **Spring WebFlux learning curve** | MEDIUM | HIGH | 2-week team training, pair programming, code reviews | Backend |
| **100 concurrent uploads exceed server capacity** | MEDIUM | CRITICAL | Direct-to-S3 (no server proxy), reactive I/O | Upload architecture |
| **WebSocket connection drops** | HIGH | MEDIUM | Automatic reconnection, fallback to polling | Real-time updates |
| **S3 costs exceed budget** | LOW | MEDIUM | Lifecycle policies (Glacier after 90 days), cost alerts | Cloud storage |
| **Database bottleneck on gallery queries** | MEDIUM | HIGH | Read replicas, database indexing, CDN caching | Data architecture |

### 4.2 ROI of Architectural Decisions

**Decision 1: Spring WebFlux (Reactive)**
- **Cost:** 2 weeks team training, +30% code complexity
- **Benefit:** Handles 10× more concurrent requests with same infrastructure
- **ROI:** Saves ~$500/month in EC2 costs (fewer/smaller instances needed)

**Decision 2: Direct-to-S3 Uploads**
- **Cost:** Client-side SDK integration, pre-signed URL management
- **Benefit:** Zero server bandwidth for uploads, unlimited concurrent uploads
- **ROI:** Eliminates server bandwidth costs (~$200/month for 10TB transfers)

**Decision 3: PostgreSQL Read Replicas**
- **Cost:** +$150/month for read replica RDS instance
- **Benefit:** Offloads 80% of queries (gallery browsing) from primary DB
- **ROI:** Prevents primary DB bottleneck, enables 10× user growth without primary DB upgrade

---

# Part II: Market Architecture (M)

## 5. Competitive Technical Landscape

### 5.1 Competitor Architecture Analysis

| Competitor | Upload Architecture | Concurrency Handling | Real-Time Updates | Our Advantage |
|------------|-------------------|---------------------|------------------|---------------|
| **Google Photos** | Proprietary (likely GCS multipart) | Massive Google infrastructure | WebSocket or SSE | We match upload UX with 1/100th the infrastructure |
| **Dropbox** | Chunked uploads to Dropbox servers | Load balancers + CDN | Polling (slower) | We have WebSocket (faster real-time) |
| **Cloudinary** | Multipart to Cloudinary CDN | Cloudinary's EdgeWorkers | MediaFlows (async) | We have synchronous real-time progress |
| **Filestack** | Intelligent Ingestion (S3 multipart) | Direct-to-S3 | Polling | We match their upload pattern + WebSocket |

**Strategic Positioning:** We combine Filestack's direct-to-cloud efficiency with Google Photos' real-time UX, using open-source stack (Spring WebFlux, PostgreSQL, S3).

### 5.2 Technology Stack Positioning

**Decision Matrix:**

| Technology | Why Chosen | Competitive Parity |
|------------|-----------|-------------------|
| **Spring WebFlux** | Best-in-class reactive Java framework, Netflix/Amazon scale | Matches Google's gRPC streaming capabilities |
| **React Native for Web** | Single codebase for web + iOS + Android, 100% code reuse | Matches modern approach (Twitter/X, Uber Eats use same strategy) |
| **AWS S3** | 99.999999999% durability, built-in multipart | Same as Dropbox, Filestack, Cloudinary |
| **PostgreSQL** | Battle-tested, excellent JSON support (for metadata) | More flexible than Dropbox's MySQL, cheaper than Google Spanner |
| **WebSocket (STOMP)** | Real-time bi-directional, lower latency than polling | Better than Dropbox/Cloudinary polling |

---

## 6. Technology Positioning

### 6.1 Architectural Differentiation

**What Makes Our Architecture Unique:**

1. **Hybrid Upload Model**
   - **Direct-to-S3 for throughput** (like Filestack)
   - **+ WebSocket for real-time UX** (like Google Photos)
   - **+ Automatic resume** (like Dropbox, but faster with S3 multipart)

2. **Domain-Driven Upload Sessions**
   - Most competitors treat uploads as stateless transactions
   - We model UploadSession as DDD aggregate with domain events
   - **Benefit:** Richer business logic (pause all, retry failed subset, calculate session stats)

3. **CQRS for Upload vs Gallery**
   - Write-optimized path for uploads (fast ingestion)
   - Read-optimized path for gallery (fast queries, CDN caching)
   - **Benefit:** Independent scaling of upload and browse workloads

### 6.2 Future-Proof Architecture

**Extensibility Points for Post-MVP:**

| Future Feature | Architecture Support | Implementation |
|----------------|---------------------|----------------|
| **AI-powered auto-tagging** | Event-driven architecture | Subscribe to UploadCompletedEvent → Lambda → Rekognition |
| **Real-time collaboration** | WebSocket infrastructure | Extend STOMP topics to include photo.shared.{userId} |
| **Mobile offline upload** | S3 multipart resume | Already supported, just needs client-side queue persistence |
| **Video support** | S3 multipart (any file type) | No architecture change, just validation rules |
| **Cross-device sync** | Eventual consistency (CQRS) | Add sync service subscribing to domain events |

---

## 7. Scalability Roadmap

### 7.1 Current Architecture Scaling Limits

**MVP Architecture (Weeks 1-12):**

| Component | Scaling Limit | Bottleneck | Mitigation (Post-MVP) |
|-----------|--------------|------------|---------------------|
| **Spring Boot API** | ~5,000 concurrent users | CPU (JSON serialization) | Add API Gateway + multiple instances |
| **PostgreSQL Primary** | ~10,000 writes/sec | Disk I/O | Sharding by user_id, or Aurora Serverless |
| **WebSocket Broker** | ~50,000 connections | Memory (connection state) | Redis-backed session store, cluster STOMP |
| **Redis (Session Store)** | ~100,000 sessions | Memory | Redis Cluster (sharding) |

**Expected Load at Launch:**
- Beta: 50 users → 5,000 upload sessions/month → **Well below limits**

### 7.2 Scaling Strategy (Post-MVP)

**Phase 1: Vertical Scaling (Months 1-6)**
- Upgrade EC2 instances (t3.medium → t3.large)
- Upgrade RDS (db.t3.medium → db.r5.large)
- **Cost:** +$300/month, supports 500 users

**Phase 2: Horizontal Scaling (Months 6-12)**
- Add ALB → 3× Spring Boot instances (Auto Scaling)
- PostgreSQL read replicas (2×)
- Redis Cluster (3 nodes)
- **Cost:** +$800/month, supports 5,000 users

**Phase 3: Advanced Optimization (Year 2)**
- Migrate to Aurora Serverless (auto-scaling DB)
- Add CloudFront CDN for API responses (geo-distributed caching)
- Implement database sharding (if >1M users)
- **Cost:** Variable, supports 50,000+ users

---

# Part III: Architecture (A)

## 8. Architecture Principles & Patterns

### 8.1 Mandatory Architecture Patterns

The system MUST implement three architectural patterns as specified in the project brief:

#### 1. Domain-Driven Design (DDD)

**Core Principles:**
- **Ubiquitous Language:** Upload Session, Upload, Photo, User, Tag
- **Bounded Contexts:** Authentication Context, Upload Context, Photo Gallery Context
- **Aggregates:** UploadSession (aggregate root) contains Uploads
- **Domain Events:** UploadInitiated, UploadCompleted, SessionCompleted, UploadFailed

**Implementation:**
```java
// Aggregate Root
@Entity
@AggregateRoot
public class UploadSession {
    @Id
    private UUID sessionId;
    private UserId userId;
    private SessionStatus status;
    private int totalFiles;
    private int completedFiles;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<Upload> uploads;

    // Domain logic (business rules)
    public void completeUpload(UploadId uploadId) {
        validateSessionActive();
        Upload upload = findUpload(uploadId);
        upload.markCompleted();
        completedFiles++;

        if (allUploadsCompleted()) {
            this.status = SessionStatus.COMPLETED;
            DomainEvents.raise(new UploadSessionCompletedEvent(this));
        }
    }

    private boolean allUploadsCompleted() {
        return completedFiles == totalFiles;
    }

    private void validateSessionActive() {
        if (status == SessionStatus.CANCELLED) {
            throw new SessionCancelledException("Cannot complete upload in cancelled session");
        }
    }
}
```

**Bounded Contexts:**

```
┌──────────────────────┐     ┌─────────────────────┐     ┌──────────────────────┐
│  Authentication      │     │  Upload Context     │     │  Gallery Context     │
│  Context             │     │                     │     │                      │
│                      │     │  - UploadSession    │     │  - Photo             │
│  - User              │────▶│  - Upload           │────▶│  - Tag               │
│  - Session           │     │  - PresignedUrl     │     │  - PhotoMetadata     │
│  - Credentials       │     │  - ProgressTracker  │     │  - PhotoQuery        │
│                      │     │                     │     │                      │
│  Anti-Corruption     │     │  Anti-Corruption    │     │  Anti-Corruption     │
│  Layer: JWT Adapter  │     │  Layer: S3 Adapter  │     │  Layer: CDN Adapter  │
└──────────────────────┘     └─────────────────────┘     └──────────────────────┘
```

#### 2. CQRS (Command Query Responsibility Segregation)

**Principle:** Separate write operations (commands) from read operations (queries)

**Commands (Write Side):**
- InitiateUploadCommand
- CompleteUploadCommand
- FailUploadCommand
- CancelSessionCommand
- AddPhotoTagCommand

**Queries (Read Side):**
- GetSessionProgressQuery
- GetUserPhotosQuery
- GetPhotosByTagQuery
- GetUploadHistoryQuery

**Implementation:**

```java
// COMMAND SIDE (Write-Optimized)
@Service
@Transactional
public class UploadCommandService {
    private final UploadRepository uploadRepository;
    private final EventPublisher eventPublisher;

    public Mono<UploadId> initiateUpload(InitiateUploadCommand cmd) {
        // Domain logic
        UploadSession session = uploadRepository.findSession(cmd.getSessionId());
        Upload upload = session.addUpload(cmd.getFileName(), cmd.getFileSize());

        // Persist
        uploadRepository.save(upload);

        // Publish event
        eventPublisher.publish(new UploadInitiatedEvent(upload.getId()));

        return Mono.just(upload.getId());
    }
}

// QUERY SIDE (Read-Optimized)
@Service
@ReadOnly
public class UploadQueryService {
    @Qualifier("readReplica")
    private final UploadReadRepository readRepository;

    public Mono<SessionProgressDTO> getSessionProgress(UUID sessionId) {
        // Read from read replica (eventual consistency acceptable)
        return readRepository.findSessionProjection(sessionId)
            .map(this::toProgressDTO);
    }
}
```

**CQRS Benefits:**
- **Upload completion:** Writes to primary DB, optimized for throughput
- **Gallery browsing:** Reads from replica, optimized for low latency
- **Independent scaling:** Can add more read replicas without impacting writes

#### 3. Vertical Slice Architecture (VSA)

**Principle:** Organize code by feature (vertical slices) rather than technical layers (horizontal)

**Traditional Layered (NOT USED):**
```
src/
├── controllers/
│   ├── UploadController.java
│   ├── PhotoController.java
│   └── UserController.java
├── services/
│   ├── UploadService.java
│   ├── PhotoService.java
│   └── UserService.java
└── repositories/
    ├── UploadRepository.java
    └── PhotoRepository.java
```

**Vertical Slice (USED):**
```
src/main/java/com/rapidphoto/
├── features/
│   ├── initiateupload/                    ← Vertical Slice 1
│   │   ├── InitiateUploadCommand.java     (Request)
│   │   ├── InitiateUploadHandler.java     (Business logic)
│   │   ├── InitiateUploadController.java  (HTTP endpoint)
│   │   ├── UploadInitiatedEvent.java      (Domain event)
│   │   └── InitiateUploadValidator.java   (Validation)
│   │
│   ├── completeupload/                    ← Vertical Slice 2
│   │   ├── CompleteUploadCommand.java
│   │   ├── CompleteUploadHandler.java
│   │   ├── CompleteUploadController.java
│   │   └── UploadCompletedEvent.java
│   │
│   ├── trackprogress/                     ← Vertical Slice 3
│   │   ├── GetProgressQuery.java
│   │   ├── ProgressQueryHandler.java
│   │   ├── ProgressController.java
│   │   └── ProgressWebSocketHandler.java
│   │
│   └── addphototag/                       ← Vertical Slice 4
│       ├── AddPhotoTagCommand.java
│       ├── AddPhotoTagHandler.java
│       ├── TagController.java
│       └── TagAddedEvent.java
│
├── shared/
│   ├── domain/                            ← Shared domain models
│   │   ├── UploadSession.java (Aggregate root)
│   │   ├── Upload.java (Entity)
│   │   ├── Photo.java (Entity)
│   │   └── User.java (Entity)
│   │
│   ├── infrastructure/                    ← Shared infrastructure
│   │   ├── S3Service.java
│   │   ├── DatabaseConfig.java
│   │   ├── WebSocketConfig.java
│   │   └── SecurityConfig.java
│   │
│   └── common/                            ← Shared utilities
│       ├── Result.java (Result monad)
│       ├── ErrorCode.java (Error codes)
│       └── ValidationException.java
```

**VSA Benefits:**
- **Feature independence:** Can develop, test, deploy `initiateupload/` without touching other features
- **Easier testing:** Each slice has its own tests, no cross-feature dependencies
- **Team scalability:** Different developers work on different slices with minimal conflicts

---

### 8.2 Architecture Guiding Principles

| Principle | Description | Implementation |
|-----------|-------------|----------------|
| **Reactive-First** | Non-blocking I/O throughout the stack | Spring WebFlux, R2DBC, reactive Redis client |
| **Stateless Services** | No server-side session state (except WebSocket) | JWT tokens, client-side state, Redis for sessions |
| **Event-Driven** | Components communicate via domain events | Spring ApplicationEventPublisher, async handlers |
| **Fail-Fast** | Validate early, provide clear errors | Input validation at controller, business rules in domain |
| **Idempotent Operations** | Safe to retry any operation | Idempotency keys for S3 uploads, database upserts |
| **Defense in Depth** | Security at every layer | JWT + HTTPS + CORS + input validation + S3 encryption |

---

## 9. System Context & Boundaries

### 9.1 System Context Diagram (C4 Model - Level 1)

```
                                 ┌─────────────┐
                                 │   End User  │
                                 │ (Web/Mobile)│
                                 └──────┬──────┘
                                        │
                                        │ HTTPS
                                        ▼
┌──────────────────────────────────────────────────────────────┐
│                    RapidPhotoUpload System                   │
│                                                              │
│  ┌────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │   React    │  │   React Native    │  │   Spring WebFlux   │  │
│  │  Web App   │  │  Mobile App  │  │   Backend API      │  │
│  └────────────┘  └──────────────┘  └──────────┬─────────┘  │
│                                                │             │
└────────────────────────────────────────────────┼─────────────┘
                                                 │
                ┌────────────────────────────────┼────────────────────────┐
                │                                │                        │
                ▼                                ▼                        ▼
        ┌───────────────┐              ┌─────────────────┐      ┌──────────────┐
        │   AWS S3      │              │   PostgreSQL    │      │  AWS SES     │
        │ (Photo Store) │              │  (Metadata DB)  │      │ (Email)      │
        └───────────────┘              └─────────────────┘      └──────────────┘
```

### 9.2 System Boundaries

**In Scope:**
- ✅ User authentication and authorization
- ✅ Photo upload (single and batch, up to 100 photos)
- ✅ Real-time progress tracking via WebSocket
- ✅ Photo gallery with pagination
- ✅ Photo tagging and filtering
- ✅ Network resilience (auto-resume)
- ✅ Cloud storage (S3)

**Out of Scope (External Systems):**
- ❌ Payment processing (no monetization in MVP)
- ❌ Third-party social media integrations
- ❌ AI/ML services (deferred to Post-MVP)
- ❌ CDN configuration (uses default CloudFront)
- ❌ Mobile app distribution (assumes TestFlight/APK direct install)

**External Dependencies:**
| Dependency | Purpose | Failure Impact | Mitigation |
|------------|---------|---------------|------------|
| **AWS S3** | Photo storage | CRITICAL - no uploads work | Pre-signed URL expiry, retry logic, user-facing error |
| **AWS SES** | Email verification | MEDIUM - users can't verify email | Async queue, retry, manual verification option |
| **PostgreSQL** | Metadata storage | CRITICAL - app unusable | RDS Multi-AZ, automated backups, health checks |
| **Redis** | Session store, caching | MEDIUM - slower responses | In-memory fallback, session re-auth |

---

## 10. Domain-Driven Design (DDD)

### 10.1 Ubiquitous Language

**Core Domain Terms:**

| Term | Definition | Bounded Context |
|------|-----------|----------------|
| **Upload Session** | A batch upload initiated by a user, containing 1-100 uploads | Upload Context |
| **Upload** | A single file (photo) upload within a session | Upload Context |
| **Photo** | A successfully uploaded image with metadata | Gallery Context |
| **Tag** | A user-defined label attached to a photo | Gallery Context |
| **Progress** | Real-time state of an upload (bytes uploaded, speed, ETA) | Upload Context |
| **Pre-signed URL** | Time-limited S3 URL for direct upload | Upload Context |
| **User** | Authenticated person using the system | Auth Context |

### 10.2 Domain Model (Core Aggregates)

#### Aggregate 1: UploadSession (Aggregate Root)

```java
package com.rapidphoto.domain;

@Entity
@Table(name = "upload_sessions")
public class UploadSession {
    @Id
    private UUID sessionId;

    @Embedded
    private UserId userId;

    @Enumerated(EnumType.STRING)
    private SessionStatus status; // IN_PROGRESS, COMPLETED, CANCELLED, FAILED

    private int totalFiles;
    private int completedFiles;
    private int failedFiles;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Upload> uploads = new ArrayList<>();

    private Instant createdAt;
    private Instant completedAt;

    // Business Rules (Domain Logic)

    public Upload initiateUpload(String fileName, long fileSize) {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new DomainException("Cannot add upload to non-active session");
        }

        if (uploads.size() >= 100) {
            throw new DomainException("Maximum 100 uploads per session");
        }

        Upload upload = Upload.create(this, fileName, fileSize);
        uploads.add(upload);

        DomainEvents.raise(new UploadInitiatedEvent(upload.getUploadId()));
        return upload;
    }

    public void completeUpload(UploadId uploadId) {
        Upload upload = findUpload(uploadId);
        upload.markCompleted();
        completedFiles++;

        checkSessionCompletion();
    }

    public void failUpload(UploadId uploadId, String errorMessage) {
        Upload upload = findUpload(uploadId);
        upload.markFailed(errorMessage);
        failedFiles++;

        checkSessionCompletion();
    }

    private void checkSessionCompletion() {
        if (completedFiles + failedFiles == totalFiles) {
            this.status = SessionStatus.COMPLETED;
            this.completedAt = Instant.now();

            DomainEvents.raise(new UploadSessionCompletedEvent(
                sessionId,
                completedFiles,
                failedFiles
            ));
        }
    }

    public void cancel() {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new DomainException("Cannot cancel non-active session");
        }

        this.status = SessionStatus.CANCELLED;

        // Cancel all in-progress uploads
        uploads.stream()
            .filter(u -> u.getStatus() == UploadStatus.UPLOADING)
            .forEach(Upload::cancel);

        DomainEvents.raise(new UploadSessionCancelledEvent(sessionId));
    }

    // Invariant enforcement
    @PrePersist
    @PreUpdate
    private void validateInvariants() {
        if (totalFiles < 1 || totalFiles > 100) {
            throw new DomainException("Total files must be between 1 and 100");
        }

        if (completedFiles + failedFiles > totalFiles) {
            throw new DomainException("Completed + failed cannot exceed total");
        }
    }
}
```

#### Aggregate 2: Photo (Separate Aggregate Root)

```java
package com.rapidphoto.domain;

@Entity
@Table(name = "photos")
public class Photo {
    @Id
    private UUID photoId;

    @Embedded
    private UserId userId;

    @Embedded
    private UploadId uploadId; // Reference to Upload (different aggregate)

    private String fileName;
    private long fileSize;

    @Embedded
    private S3Location s3Location; // Value object

    @Embedded
    private PhotoMetadata metadata; // Value object (EXIF data)

    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL)
    private Set<PhotoTag> tags = new HashSet<>();

    private Instant createdAt;
    private Instant deletedAt; // Soft delete

    // Business Rules

    public void addTag(String tagName) {
        if (tags.size() >= 10) {
            throw new DomainException("Maximum 10 tags per photo");
        }

        if (!TagName.isValid(tagName)) {
            throw new DomainException("Invalid tag name: " + tagName);
        }

        TagName tag = new TagName(tagName);
        if (tags.stream().anyMatch(t -> t.getName().equals(tag))) {
            return; // Idempotent - tag already exists
        }

        tags.add(new PhotoTag(this, tag));
        DomainEvents.raise(new PhotoTagAddedEvent(photoId, tag));
    }

    public void removeTag(String tagName) {
        TagName tag = new TagName(tagName);
        tags.removeIf(t -> t.getName().equals(tag));

        DomainEvents.raise(new PhotoTagRemovedEvent(photoId, tag));
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        DomainEvents.raise(new PhotoDeletedEvent(photoId));
    }
}
```

### 10.3 Value Objects

```java
// S3Location value object
@Embeddable
public class S3Location {
    private String bucket;
    private String key;
    private String versionId;

    public String getStorageUrl() {
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
    }

    // Value objects are immutable
    private S3Location() {}

    public S3Location(String bucket, String key, String versionId) {
        this.bucket = Objects.requireNonNull(bucket);
        this.key = Objects.requireNonNull(key);
        this.versionId = versionId;
    }

    // Equals and hashCode based on all fields
}

// TagName value object
public class TagName {
    private final String value;

    public TagName(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid tag: " + value);
        }
        this.value = value.toLowerCase().trim();
    }

    public static boolean isValid(String tag) {
        return tag != null
            && tag.length() >= 1
            && tag.length() <= 30
            && tag.matches("^[a-zA-Z0-9\\s]+$");
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagName)) return false;
        TagName tagName = (TagName) o;
        return value.equals(tagName.value);
    }
}
```

### 10.4 Domain Events

```java
// Domain event base class
public abstract class DomainEvent {
    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredAt = Instant.now();

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
}

// Upload domain events
public class UploadInitiatedEvent extends DomainEvent {
    private final UUID uploadId;
    private final UUID sessionId;

    public UploadInitiatedEvent(UUID uploadId, UUID sessionId) {
        this.uploadId = uploadId;
        this.sessionId = sessionId;
    }

    // Getters
}

public class UploadCompletedEvent extends DomainEvent {
    private final UUID uploadId;
    private final UUID sessionId;
    private final String fileName;
    private final S3Location s3Location;

    // Constructor, getters
}

public class UploadSessionCompletedEvent extends DomainEvent {
    private final UUID sessionId;
    private final int completedFiles;
    private final int failedFiles;

    // Constructor, getters
}

// Event publisher
@Component
public class DomainEventPublisher {
    private final ApplicationEventPublisher publisher;

    public DomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}

// Event handler (async)
@Component
public class UploadEventHandlers {
    @Async
    @EventListener
    public void handleUploadCompleted(UploadCompletedEvent event) {
        // Create Photo entity from completed Upload
        // Send notification
        // Update analytics
    }

    @Async
    @EventListener
    public void handleSessionCompleted(UploadSessionCompletedEvent event) {
        // Send completion email
        // Update user stats
        // Trigger post-processing (thumbnail generation)
    }
}
```

---

## 11. CQRS Implementation

### 11.1 Command Side (Write Model)

**Commands are intentions to change state:**

```java
// Command DTO
public record InitiateUploadCommand(
    UUID sessionId,
    String fileName,
    long fileSize,
    String contentType
) {
    // Validation in constructor
    public InitiateUploadCommand {
        Objects.requireNonNull(sessionId, "Session ID required");
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name required");
        }
        if (fileSize <= 0 || fileSize > 5 * 1024 * 1024) { // 5MB limit
            throw new IllegalArgumentException("File size must be 1B-5MB");
        }
    }
}

// Command Handler
@Service
@Transactional
public class UploadCommandHandler {
    private final UploadSessionRepository sessionRepository;
    private final S3PresignedUrlService s3Service;
    private final DomainEventPublisher eventPublisher;

    public Mono<InitiateUploadResponse> handle(InitiateUploadCommand command) {
        return sessionRepository.findById(command.sessionId())
            .switchIfEmpty(Mono.error(new SessionNotFoundException()))
            .flatMap(session -> {
                // Domain logic
                Upload upload = session.initiateUpload(
                    command.fileName(),
                    command.fileSize()
                );

                // Persist
                return sessionRepository.save(session)
                    .then(generatePresignedUrl(upload, command.contentType()))
                    .map(presignedUrl -> new InitiateUploadResponse(
                        upload.getUploadId(),
                        presignedUrl,
                        Duration.ofMinutes(15)
                    ));
            });
    }

    private Mono<String> generatePresignedUrl(Upload upload, String contentType) {
        return s3Service.generatePutUrl(
            upload.getUploadId().toString(),
            contentType,
            Duration.ofMinutes(15)
        );
    }
}

// Controller (Command endpoint)
@RestController
@RequestMapping("/api/v1/uploads")
public class UploadCommandController {
    private final UploadCommandHandler commandHandler;

    @PostMapping("/initiate")
    public Mono<ResponseEntity<InitiateUploadResponse>> initiateUpload(
        @RequestBody @Valid InitiateUploadRequest request
    ) {
        InitiateUploadCommand command = request.toCommand();

        return commandHandler.handle(command)
            .map(response -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response)
            )
            .onErrorResume(SessionNotFoundException.class, e ->
                Mono.just(ResponseEntity.notFound().build())
            );
    }
}
```

### 11.2 Query Side (Read Model)

**Queries are requests for data:**

```java
// Query DTO
public record GetSessionProgressQuery(UUID sessionId) {}

// Query Handler
@Service
@Transactional(readOnly = true)
public class UploadQueryHandler {
    @Qualifier("readReplica") // Read from replica
    private final UploadReadRepository readRepository;

    public Mono<SessionProgressDTO> handle(GetSessionProgressQuery query) {
        return readRepository.findSessionProgress(query.sessionId())
            .map(this::toDTO);
    }

    private SessionProgressDTO toDTO(SessionProgressProjection projection) {
        return new SessionProgressDTO(
            projection.getSessionId(),
            projection.getTotalFiles(),
            projection.getCompletedFiles(),
            projection.getFailedFiles(),
            projection.getStatus(),
            projection.getUploads().stream()
                .map(this::toUploadProgressDTO)
                .toList()
        );
    }
}

// Read-optimized repository (uses projections)
@Repository
public interface UploadReadRepository extends R2dbcRepository<UploadSession, UUID> {

    @Query("""
        SELECT
            s.session_id, s.total_files, s.completed_files, s.failed_files, s.status,
            u.upload_id, u.file_name, u.file_size, u.upload_status, u.bytes_uploaded
        FROM upload_sessions s
        LEFT JOIN uploads u ON s.session_id = u.session_id
        WHERE s.session_id = :sessionId
    """)
    Mono<SessionProgressProjection> findSessionProgress(UUID sessionId);
}

// Controller (Query endpoint)
@RestController
@RequestMapping("/api/v1/uploads/sessions")
public class UploadQueryController {
    private final UploadQueryHandler queryHandler;

    @GetMapping("/{sessionId}/progress")
    public Mono<ResponseEntity<SessionProgressDTO>> getProgress(
        @PathVariable UUID sessionId
    ) {
        GetSessionProgressQuery query = new GetSessionProgressQuery(sessionId);

        return queryHandler.handle(query)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
```

### 11.3 CQRS Benefits in Our System

| Aspect | Command Side | Query Side | Benefit |
|--------|-------------|------------|---------|
| **Database** | Primary (master) | Read replica | Writes don't block reads |
| **Optimization** | Write throughput | Read latency | Independent tuning |
| **Caching** | No caching (stale writes dangerous) | Aggressive caching | Faster reads without stale write risk |
| **Scaling** | 1 primary DB | N read replicas | Scale reads independently |
| **Complexity** | Full domain model | DTOs/projections | Simpler queries |

---

## 12. Vertical Slice Architecture (VSA)

### 12.1 Feature Slice Structure

Each feature is a self-contained vertical slice with all layers:

```
features/initiateupload/
├── InitiateUploadCommand.java       # Request model
├── InitiateUploadHandler.java       # Business logic
├── InitiateUploadController.java    # HTTP endpoint
├── InitiateUploadValidator.java     # Input validation
├── InitiateUploadResponse.java      # Response model
├── UploadInitiatedEvent.java        # Domain event
└── InitiateUploadTests.java         # Feature tests
```

**Complete Example: "Initiate Upload" Slice**

```java
// 1. Command (Request)
package com.rapidphoto.features.initiateupload;

public record InitiateUploadCommand(
    UUID sessionId,
    String fileName,
    long fileSize,
    String contentType
) implements Command {}

// 2. Handler (Business Logic)
@Service
public class InitiateUploadHandler implements CommandHandler<InitiateUploadCommand, InitiateUploadResponse> {
    private final UploadSessionRepository sessionRepository;
    private final S3PresignedUrlService s3Service;

    @Override
    public Mono<InitiateUploadResponse> handle(InitiateUploadCommand command) {
        return sessionRepository.findById(command.sessionId())
            .switchIfEmpty(Mono.error(new SessionNotFoundException()))
            .flatMap(session -> processUpload(session, command));
    }

    private Mono<InitiateUploadResponse> processUpload(
        UploadSession session,
        InitiateUploadCommand command
    ) {
        // Domain logic
        Upload upload = session.initiateUpload(command.fileName(), command.fileSize());

        // Generate pre-signed URL
        return s3Service.generatePutUrl(upload)
            .flatMap(presignedUrl ->
                sessionRepository.save(session)
                    .thenReturn(new InitiateUploadResponse(
                        upload.getUploadId(),
                        presignedUrl,
                        Duration.ofMinutes(15)
                    ))
            );
    }
}

// 3. Controller (HTTP Endpoint)
@RestController
@RequestMapping("/api/v1/uploads")
public class InitiateUploadController {
    private final InitiateUploadHandler handler;

    @PostMapping("/initiate")
    public Mono<ResponseEntity<InitiateUploadResponse>> initiateUpload(
        @RequestBody @Valid InitiateUploadRequest request,
        @AuthenticationPrincipal Mono<UserPrincipal> principal
    ) {
        return principal
            .map(user -> request.toCommand(user.getUserId()))
            .flatMap(handler::handle)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
            .onErrorResume(this::handleError);
    }

    private Mono<ResponseEntity<InitiateUploadResponse>> handleError(Throwable error) {
        if (error instanceof SessionNotFoundException) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        if (error instanceof DomainException) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return Mono.error(error); // Let global handler catch
    }
}

// 4. Tests (Feature-level)
@SpringBootTest
@AutoConfigureWebTestClient
class InitiateUploadTests {
    @Autowired
    WebTestClient webClient;

    @Test
    void shouldInitiateUploadSuccessfully() {
        // Given
        UUID sessionId = createTestSession();
        InitiateUploadRequest request = new InitiateUploadRequest(
            sessionId,
            "test.jpg",
            1024L,
            "image/jpeg"
        );

        // When
        webClient.post()
            .uri("/api/v1/uploads/initiate")
            .bodyValue(request)
            .exchange()

        // Then
            .expectStatus().isCreated()
            .expectBody(InitiateUploadResponse.class)
            .value(response -> {
                assertThat(response.uploadId()).isNotNull();
                assertThat(response.presignedUrl()).contains("s3.amazonaws.com");
                assertThat(response.expiresIn()).isEqualTo(Duration.ofMinutes(15));
            });
    }
}
```

### 12.2 Slice Independence

**Key Principle:** Each slice can be developed, tested, and deployed independently.

**Example: Adding a new feature "Batch Retry Failed Uploads"**

```
features/batchretryfailed/      ← NEW SLICE
├── BatchRetryFailedCommand.java
├── BatchRetryFailedHandler.java
├── BatchRetryFailedController.java
└── BatchRetryFailedTests.java

# No changes needed to existing slices!
```

**Shared Domain Models:** Slices reference shared domain models but don't depend on each other:

```java
// features/initiateupload/Handler references shared domain
import com.rapidphoto.shared.domain.UploadSession;
import com.rapidphoto.shared.domain.Upload;

// features/completeupload/Handler also references same domain
import com.rapidphoto.shared.domain.UploadSession;
import com.rapidphoto.shared.domain.Upload;

// But initiateupload/ doesn't import anything from completeupload/
// And vice versa - slices are independent!
```

---

## 13. Component Architecture

### 13.1 Backend Components (Spring Boot)

```
┌─────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                     │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    HTTP Layer                             │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │ Auth         │  │ Upload       │  │ Photo        │   │  │
│  │  │ Controller   │  │ Controller   │  │ Controller   │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │  │
│  └───────────────────────────┬──────────────────────────────┘  │
│                              │                                  │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │                  Application Services                     │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │ Command      │  │ Query        │  │ Event        │   │  │
│  │  │ Handlers     │  │ Handlers     │  │ Handlers     │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │  │
│  └───────────────────────────┬──────────────────────────────┘  │
│                              │                                  │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │                    Domain Layer                           │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │ UploadSession│  │ Photo        │  │ User         │   │  │
│  │  │ (Aggregate)  │  │ (Aggregate)  │  │ (Aggregate)  │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │  │
│  └───────────────────────────┬──────────────────────────────┘  │
│                              │                                  │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │                  Infrastructure Layer                     │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │ R2DBC        │  │ S3 Client    │  │ WebSocket    │   │  │
│  │  │ Repository   │  │ (AWS SDK)    │  │ (STOMP)      │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

**Key Components:**

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **HTTP Controllers** | Spring WebFlux @RestController | Handle HTTP requests, validation, auth |
| **Command Handlers** | Custom handlers (VSA) | Execute write operations (CQRS commands) |
| **Query Handlers** | Custom handlers (VSA) | Execute read operations (CQRS queries) |
| **Event Handlers** | @EventListener (async) | React to domain events |
| **Domain Models** | JPA entities with business logic | Encapsulate business rules (DDD) |
| **Repositories** | R2DBC repositories | Reactive database access |
| **S3 Service** | AWS SDK S3AsyncClient | Generate pre-signed URLs, manage uploads |
| **WebSocket Handler** | Spring WebSocket STOMP | Real-time progress updates |

### 13.2 Frontend Components (React Web)

```
src/
├── features/
│   ├── auth/
│   │   ├── LoginForm.tsx
│   │   ├── RegistrationForm.tsx
│   │   ├── useAuth.ts (hook)
│   │   └── authSlice.ts (Redux)
│   │
│   ├── upload/
│   │   ├── UploadDropzone.tsx
│   │   ├── UploadProgress.tsx
│   │   ├── UploadManager.ts (handles S3 upload)
│   │   ├── useUploadSession.ts (hook)
│   │   └── uploadSlice.ts (Redux)
│   │
│   └── gallery/
│       ├── PhotoGrid.tsx
│       ├── Lightbox.tsx
│       ├── TagInput.tsx
│       ├── usePhotos.ts (hook)
│       └── gallerySlice.ts (Redux)
│
├── shared/
│   ├── components/
│   │   ├── Button.tsx
│   │   ├── ProgressBar.tsx
│   │   └── Modal.tsx
│   │
│   ├── hooks/
│   │   ├── useWebSocket.ts
│   │   └── useInfiniteScroll.ts
│   │
│   └── api/
│       ├── apiClient.ts
│       └── uploadApi.ts
│
└── app/
    ├── store.ts (Redux store)
    └── App.tsx
```

**Key Frontend Patterns:**

1. **Feature-based organization** (mirrors backend VSA)
2. **Custom hooks** for business logic
3. **Redux slices** for state management
4. **Separation of concerns:** Components (UI) + Hooks (logic) + API (data)

### 13.3 Cross-Platform Components (React Native for Web)

**Single Codebase for Web + iOS + Android:**

```
src/
├── components/               ← Cross-platform UI components
│   ├── Button.tsx           ← Uses <Pressable> (works on all platforms)
│   ├── UploadProgress.tsx
│   ├── PhotoCard.tsx
│   └── Lightbox.tsx
│
├── screens/                 ← Feature screens (all platforms)
│   ├── auth/
│   │   ├── LoginScreen.tsx
│   │   └── RegistrationScreen.tsx
│   ├── upload/
│   │   └── UploadScreen.tsx
│   └── gallery/
│       ├── GalleryScreen.tsx
│       └── PhotoViewScreen.tsx
│
├── hooks/                   ← Shared business logic
│   ├── useAuth.ts
│   ├── useUpload.ts
│   └── usePhotoGallery.ts
│
├── services/                ← API clients and platform services
│   ├── api/
│   │   ├── authApi.ts
│   │   ├── uploadApi.ts
│   │   └── photoApi.ts
│   ├── s3/
│   │   └── S3UploadService.ts
│   └── websocket/
│       └── WebSocketClient.ts
│
├── store/                   ← Redux Toolkit (shared across all platforms)
│   ├── authSlice.ts
│   ├── uploadSlice.ts
│   └── photoSlice.ts
│
├── types/                   ← TypeScript types (shared)
│   ├── User.ts
│   ├── Photo.ts
│   └── UploadSession.ts
│
├── navigation/              ← React Navigation (web + mobile)
│   └── RootNavigator.tsx
│
└── App.tsx                  ← Single entry point for all platforms
```

**React Native for Web Architecture:**
- **Components:** Use React Native primitives (`View`, `Text`, `Pressable`) - compile to DOM on web, native on mobile
- **State:** Redux Toolkit shared across all platforms
- **Navigation:** React Navigation (works on web + mobile)
- **Styling:** StyleSheet API (compiles to CSS on web)
- **Platform-Specific:** Use `Platform.select()` when needed

**Build Targets:**
```
npm run web      → Web build (react-native-web)
npm run ios      → iOS build (React Native)
npm run android  → Android build (React Native)
```

---

## 14. Data Architecture

### 14.1 Database Schema (PostgreSQL)

**Full Schema with Indexes:**

```sql
-- Users table
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    INDEX idx_email (email)
);

-- Upload sessions table
CREATE TABLE upload_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    total_files INTEGER NOT NULL,
    completed_files INTEGER DEFAULT 0,
    failed_files INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL, -- IN_PROGRESS, COMPLETED, CANCELLED, FAILED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,

    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_status (status) WHERE status = 'IN_PROGRESS',

    CONSTRAINT chk_total_files CHECK (total_files >= 1 AND total_files <= 100),
    CONSTRAINT chk_completed_failed CHECK (completed_files + failed_files <= total_files)
);

-- Uploads table
CREATE TABLE uploads (
    upload_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES upload_sessions(session_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    s3_bucket VARCHAR(255) NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    s3_version_id VARCHAR(255),
    upload_status VARCHAR(20) NOT NULL, -- PENDING, UPLOADING, COMPLETED, FAILED, CANCELLED
    bytes_uploaded BIGINT DEFAULT 0,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    presigned_url_generated_at TIMESTAMP WITH TIME ZONE,
    upload_started_at TIMESTAMP WITH TIME ZONE,
    upload_completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    INDEX idx_session_status (session_id, upload_status),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_s3_key (s3_bucket, s3_key),

    CONSTRAINT chk_file_size CHECK (file_size > 0 AND file_size <= 5242880), -- 5MB
    CONSTRAINT chk_bytes_uploaded CHECK (bytes_uploaded >= 0 AND bytes_uploaded <= file_size)
);

-- Photos table (created after successful upload)
CREATE TABLE photos (
    photo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id UUID NOT NULL REFERENCES uploads(upload_id),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    s3_bucket VARCHAR(255) NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    storage_url TEXT NOT NULL, -- CloudFront CDN URL
    thumbnail_url TEXT, -- Generated thumbnail URL
    metadata JSONB, -- EXIF data, camera info, etc.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE, -- Soft delete

    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_deleted (deleted_at) WHERE deleted_at IS NULL,
    INDEX idx_metadata USING GIN (metadata)
);

-- Photo tags table (normalized many-to-many)
CREATE TABLE photo_tags (
    photo_id UUID NOT NULL REFERENCES photos(photo_id) ON DELETE CASCADE,
    tag_name VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    PRIMARY KEY (photo_id, tag_name),

    INDEX idx_tag_search (tag_name, photo_id),
    INDEX idx_tag_name (tag_name)
);

-- User settings table
CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    notifications_enabled BOOLEAN DEFAULT TRUE,
    desktop_notifications BOOLEAN DEFAULT TRUE,
    push_notifications BOOLEAN DEFAULT TRUE,
    success_animations BOOLEAN DEFAULT TRUE,
    success_sounds BOOLEAN DEFAULT TRUE,
    theme VARCHAR(10) DEFAULT 'AUTO', -- LIGHT, DARK, AUTO
    concurrent_upload_limit INTEGER DEFAULT 10,
    auto_retry_failed BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_concurrent_limit CHECK (concurrent_upload_limit >= 1 AND concurrent_upload_limit <= 10)
);
```

### 14.2 Database Indexing Strategy

**Query-Driven Indexing:**

| Query | Index | Justification |
|-------|-------|---------------|
| Get user's photos | `idx_user_created (user_id, created_at DESC)` | User browses their gallery chronologically |
| Filter photos by tag | `idx_tag_search (tag_name, photo_id)` | Tag filter queries (WHERE tag_name = ?) |
| Track upload progress | `idx_session_status (session_id, upload_status)` | WebSocket progress updates (WHERE session_id AND status) |
| Find in-progress sessions | `idx_status (status) WHERE status = 'IN_PROGRESS'` | Partial index for active sessions cleanup |
| Search metadata (future) | `idx_metadata USING GIN (metadata)` | JSONB search for EXIF data queries |

**Index Maintenance:**
- `ANALYZE` tables weekly (automated via cron)
- `REINDEX` monthly to reduce bloat
- Monitor index size with `pg_indexes` view

### 14.3 Data Retention & Archival

**Retention Policy:**

| Data Type | Retention | Archive Strategy |
|-----------|-----------|-----------------|
| **Upload sessions (completed)** | 30 days | Soft delete (deleted_at), purge after 90 days |
| **Photos (active)** | Indefinite | User-controlled deletion |
| **Photos (soft-deleted)** | 30 days | Permanent delete after 30 days |
| **Upload logs** | 90 days | Export to S3 (CloudWatch Logs) for audit |

**Archival Script (Scheduled Job):**

```sql
-- Soft delete completed upload sessions older than 30 days
UPDATE upload_sessions
SET status = 'ARCHIVED'
WHERE status = 'COMPLETED'
  AND completed_at < NOW() - INTERVAL '30 days';

-- Permanently delete soft-deleted photos after 30 days
DELETE FROM photos
WHERE deleted_at IS NOT NULL
  AND deleted_at < NOW() - INTERVAL '30 days';
```

---

## 15. Integration Architecture

### 15.1 External System Integrations

```
┌────────────────────────────────────────────────────────────────┐
│              RapidPhotoUpload System                           │
│                                                                  │
│  ┌────────────┐     ┌────────────┐     ┌────────────┐         │
│  │  React Web │     │  React Native   │     │  Spring    │         │
│  │    App     │────▶│   Mobile   │────▶│  WebFlux   │         │
│  └────────────┘     └────────────┘     └─────┬──────┘         │
│                                               │                 │
└───────────────────────────────────────────────┼─────────────────┘
                                                │
                   ┌────────────────────────────┼───────────────────────────┐
                   │                            │                           │
                   ▼                            ▼                           ▼
          ┌────────────────┐          ┌────────────────┐          ┌────────────────┐
          │   AWS S3       │          │  PostgreSQL    │          │   AWS SES      │
          │                │          │   (RDS)        │          │  (Email)       │
          │ • Photo Storage│          │ • Metadata DB  │          │                │
          │ • Multipart    │          │ • Transactions │          │ • Verification │
          │ • Pre-signed   │          │ • Read Replica │          │ • Notifications│
          │   URLs         │          │                │          │                │
          └────────────────┘          └────────────────┘          └────────────────┘
                   │
                   │ (Event Notification)
                   ▼
          ┌────────────────┐
          │  AWS Lambda    │
          │  (Future)      │
          │                │
          │ • Thumbnail    │
          │   generation   │
          │ • AI tagging   │
          └────────────────┘
```

### 15.2 AWS S3 Integration

**Pre-Signed URL Pattern:**

```java
@Service
public class S3PresignedUrlService {
    private final S3Presigner presigner;
    private final String bucketName;

    public Mono<PresignedUrlResponse> generatePutUrl(
        UUID uploadId,
        String fileName,
        String contentType,
        Duration expiration
    ) {
        String s3Key = buildS3Key(uploadId, fileName);

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(contentType)
            .metadata(Map.of(
                "upload-id", uploadId.toString(),
                "original-filename", fileName
            ))
            .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .putObjectRequest(putRequest)
                .signatureDuration(expiration) // 15 minutes
                .build()
        );

        return Mono.just(new PresignedUrlResponse(
            uploadId,
            presignedRequest.url().toString(),
            s3Key,
            expiration
        ));
    }

    private String buildS3Key(UUID uploadId, String fileName) {
        // uploads/{date}/{uploadId}/{sanitized-filename}
        String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String sanitizedName = sanitizeFileName(fileName);
        return String.format("uploads/%s/%s/%s", date, uploadId, sanitizedName);
    }
}
```

**S3 Event Notification (Future - Post-MVP):**

```yaml
# S3 Bucket notification configuration
NotificationConfiguration:
  LambdaFunctionConfigurations:
    - Event: s3:ObjectCreated:*
      Filter:
        Key:
          FilterRules:
            - Name: prefix
              Value: uploads/
      LambdaFunctionArn: arn:aws:lambda:us-east-1:123456789:function:ThumbnailGenerator
```

### 15.3 WebSocket Integration (Real-Time Progress)

**STOMP Protocol over WebSocket:**

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages FROM client TO server
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/upload-progress")
            .setAllowedOrigins("https://rapidphotoupload.com")
            .withSockJS(); // Fallback to polling if WebSocket unavailable
    }
}

// WebSocket Controller
@Controller
public class UploadProgressWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleUploadProgress(UploadProgressUpdatedEvent event) {
        ProgressDTO progress = ProgressDTO.from(event);

        // Send to specific session topic
        messagingTemplate.convertAndSend(
            "/topic/upload-progress/" + event.getSessionId(),
            progress
        );
    }
}
```

**Client-Side WebSocket (React):**

```typescript
import { Client, StompSubscription } from '@stomp/stompjs';

export function useUploadProgress(sessionId: string) {
  const [progress, setProgress] = useState<UploadProgress[]>([]);

  useEffect(() => {
    const client = new Client({
      brokerURL: 'wss://api.rapidphotoupload.com/ws/upload-progress',
      onConnect: () => {
        client.subscribe(
          `/topic/upload-progress/${sessionId}`,
          (message) => {
            const update = JSON.parse(message.body);
            setProgress(prev => updateProgress(prev, update));
          }
        );
      },
    });

    client.activate();

    return () => client.deactivate();
  }, [sessionId]);

  return progress;
}
```

---

## 16. Security Architecture

### 16.1 Authentication & Authorization

**JWT-Based Stateless Authentication:**

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf().disable() // Using JWT, CSRF not needed
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/v1/auth/**").permitAll()
                .pathMatchers("/api/v1/uploads/**").authenticated()
                .pathMatchers("/api/v1/photos/**").authenticated()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter()))
            )
            .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // RS256 asymmetric keys (public key for validation)
        return ReactiveJwtDecoders.fromOidcIssuerLocation(issuerUri);
    }
}
```

**JWT Token Structure:**

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "550e8400-e29b-41d4-a716-446655440000",  // user_id
    "email": "user@example.com",
    "iat": 1699564800,  // Issued at
    "exp": 1699565700,  // Expires in 15 minutes (access token)
    "type": "access"
  },
  "signature": "..."
}
```

**Token Refresh Flow:**

```
Client                  Backend                 Redis
  │                        │                       │
  │  POST /auth/login      │                       │
  ├──────────────────────▶ │                       │
  │                        │                       │
  │  ◀──────────────────────┤                       │
  │  { accessToken,        │                       │
  │    refreshToken }      │   SETEX refresh:uuid  │
  │                        ├──────────────────────▶│
  │                        │   (7 days TTL)        │
  │                        │                       │
  │  ... 14 minutes later ...                      │
  │                        │                       │
  │  POST /auth/refresh    │                       │
  │  { refreshToken }      │                       │
  ├──────────────────────▶ │   GET refresh:uuid   │
  │                        ├──────────────────────▶│
  │                        │                       │
  │  ◀──────────────────────┤                       │
  │  { newAccessToken }    │                       │
```

### 16.2 Data Security

**Encryption at Rest:**

| Data | Encryption Method | Key Management |
|------|------------------|---------------|
| **S3 Photos** | AES-256 (SSE-S3) | AWS-managed keys |
| **Database** | AES-256 (RDS encryption) | AWS KMS |
| **Passwords** | bcrypt (cost factor 12) | N/A (one-way hash) |
| **JWT Signing** | RS256 private key | AWS Secrets Manager |

**Encryption in Transit:**

| Connection | Protocol | Enforcement |
|------------|----------|------------|
| **Client → Backend** | TLS 1.3 | Mandatory (HTTPS only) |
| **Backend → Database** | TLS 1.2+ | RDS force_ssl=1 |
| **Backend → S3** | TLS 1.2+ | AWS SDK default |
| **WebSocket** | WSS (TLS) | Mandatory |

### 16.3 Input Validation & Sanitization

**Multi-Layer Validation:**

```java
// Layer 1: Controller (HTTP validation)
@RestController
public class UploadController {
    @PostMapping("/uploads/initiate")
    public Mono<ResponseEntity<InitiateUploadResponse>> initiateUpload(
        @RequestBody @Valid InitiateUploadRequest request  // ← @Valid triggers validation
    ) {
        // ...
    }
}

// Layer 2: DTO Validation (Bean Validation)
public class InitiateUploadRequest {
    @NotNull(message = "Session ID required")
    private UUID sessionId;

    @NotBlank(message = "File name required")
    @Size(max = 255, message = "File name max 255 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+\\.(jpg|jpeg|png|heic|webp)$",
        flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "Invalid file name or unsupported format"
    )
    private String fileName;

    @Min(value = 1, message = "File size must be at least 1 byte")
    @Max(value = 5242880, message = "File size max 5MB")
    private long fileSize;

    @Pattern(regexp = "^image/(jpeg|png|heic|webp)$")
    private String contentType;
}

// Layer 3: Domain Validation (Business Rules)
@Entity
public class UploadSession {
    public Upload initiateUpload(String fileName, long fileSize) {
        if (uploads.size() >= 100) {
            throw new DomainException("Maximum 100 uploads per session");
        }

        if (status != SessionStatus.IN_PROGRESS) {
            throw new DomainException("Cannot add upload to non-active session");
        }

        // Domain logic continues...
    }
}
```

**SQL Injection Prevention:**

- **R2DBC Parameterized Queries:** All database queries use parameter binding
- **JPA/Hibernate:** No raw SQL, all queries type-safe
- **Example:**

```java
@Query("SELECT p FROM Photo p WHERE p.userId = :userId AND p.deletedAt IS NULL")
Flux<Photo> findActivePhotosByUser(@Param("userId") UUID userId);
// ✅ Safe: :userId is parameter-bound, not string-concatenated
```

---

## 17. Infrastructure Architecture

### 17.1 AWS Infrastructure (Production)

```
┌──────────────────────────────────────────────────────────────────────┐
│                           AWS Cloud (us-east-1)                      │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Route 53                                                     │   │
│  │  • rapidphotoupload.com → CloudFront                         │   │
│  │  • api.rapidphotoupload.com → ALB                            │   │
│  └───────────────────┬─────────────────────────────────────────┘   │
│                      │                                               │
│  ┌───────────────────▼──────────┐  ┌───────────────────────────┐  │
│  │  CloudFront (CDN)            │  │  Application Load Balancer │  │
│  │  • Static assets (React)     │  │  • SSL termination         │  │
│  │  • Photo thumbnails          │  │  • Health checks           │  │
│  └──────────────────────────────┘  │  • Auto-scaling            │  │
│                                     └───────────┬───────────────┘  │
│                                                 │                   │
│  ┌──────────────────────────────────────────────▼────────────────┐ │
│  │  Auto Scaling Group (2-10 instances)                          │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │ │
│  │  │   EC2        │  │   EC2        │  │   EC2        │       │ │
│  │  │ Spring Boot  │  │ Spring Boot  │  │ Spring Boot  │       │ │
│  │  │  (WebFlux)   │  │  (WebFlux)   │  │  (WebFlux)   │       │ │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │ │
│  └─────────┼──────────────────┼──────────────────┼──────────────┘ │
│            │                  │                  │                 │
│  ┌─────────▼──────────────────▼──────────────────▼──────────────┐ │
│  │  VPC (10.0.0.0/16)                                            │ │
│  │  ┌────────────────────┐  ┌────────────────────┐              │ │
│  │  │  Private Subnet 1  │  │  Private Subnet 2  │              │ │
│  │  │  (10.0.1.0/24)     │  │  (10.0.2.0/24)     │              │ │
│  │  │                    │  │                    │              │ │
│  │  │  ┌──────────────┐  │  │  ┌──────────────┐  │              │ │
│  │  │  │ RDS Primary  │◀─┼──┼─▶│RDS Read      │  │              │ │
│  │  │  │ PostgreSQL   │  │  │  │Replica       │  │              │ │
│  │  │  └──────────────┘  │  │  └──────────────┘  │              │ │
│  │  │                    │  │                    │              │ │
│  │  │  ┌──────────────┐  │  │  ┌──────────────┐  │              │ │
│  │  │  │ElastiCache   │  │  │  │ElastiCache   │  │              │ │
│  │  │  │Redis Primary │◀─┼──┼─▶│Redis Replica │  │              │ │
│  │  │  └──────────────┘  │  │  └──────────────┘  │              │ │
│  │  └────────────────────┘  └────────────────────┘              │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │  S3 Buckets                                                    │ │
│  │  • rapidphoto-uploads (photos)                                │ │
│  │  • rapidphoto-static (web app assets)                         │ │
│  │  • rapidphoto-backups (database backups)                      │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 17.2 Infrastructure as Code (Terraform)

```hcl
# terraform/main.tf

# VPC Configuration
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "rapidphoto-vpc"
  }
}

# RDS PostgreSQL
resource "aws_db_instance" "primary" {
  identifier             = "rapidphoto-db"
  engine                = "postgres"
  engine_version        = "15.3"
  instance_class        = "db.t3.medium"
  allocated_storage     = 100
  storage_type          = "gp3"

  db_name               = "rapidphoto"
  username              = var.db_username
  password              = var.db_password

  multi_az              = true  # High availability
  backup_retention_period = 7
  backup_window         = "03:00-04:00"

  vpc_security_group_ids = [aws_security_group.database.id]
  db_subnet_group_name  = aws_db_subnet_group.main.name

  storage_encrypted     = true

  tags = {
    Name = "rapidphoto-primary-db"
  }
}

# RDS Read Replica
resource "aws_db_instance" "read_replica" {
  identifier             = "rapidphoto-db-replica"
  replicate_source_db    = aws_db_instance.primary.identifier
  instance_class        = "db.t3.medium"

  vpc_security_group_ids = [aws_security_group.database.id]

  tags = {
    Name = "rapidphoto-read-replica"
  }
}

# ElastiCache Redis
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "rapidphoto-redis"
  replication_group_description = "Redis for session storage"

  engine                     = "redis"
  engine_version            = "7.0"
  node_type                 = "cache.t3.micro"
  num_cache_clusters        = 2  # Primary + 1 replica

  automatic_failover_enabled = true

  subnet_group_name         = aws_elasticache_subnet_group.main.name
  security_group_ids        = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
}

# S3 Bucket for Uploads
resource "aws_s3_bucket" "uploads" {
  bucket = "rapidphoto-uploads-${var.environment}"

  tags = {
    Name = "rapidphoto-uploads"
  }
}

resource "aws_s3_bucket_versioning" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Auto Scaling Group for Spring Boot
resource "aws_autoscaling_group" "app" {
  name                 = "rapidphoto-app-asg"
  vpc_zone_identifier  = aws_subnet.private[*].id
  target_group_arns    = [aws_lb_target_group.app.arn]
  health_check_type    = "ELB"
  health_check_grace_period = 300

  min_size             = 2
  max_size             = 10
  desired_capacity     = 2

  launch_template {
    id      = aws_launch_template.app.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "rapidphoto-app-instance"
    propagate_at_launch = true
  }
}
```

### 17.3 Deployment Architecture

**Blue-Green Deployment Strategy:**

```
┌────────────────────────────────────────────────────────┐
│  Application Load Balancer                             │
│  • Target Group: Blue (100% traffic)                   │
│  • Target Group: Green (0% traffic)                    │
└────────┬──────────────────────┬────────────────────────┘
         │                      │
         ▼                      ▼
┌────────────────┐    ┌────────────────┐
│  Blue ASG      │    │  Green ASG     │
│  v1.2.0        │    │  v1.3.0        │
│  (Active)      │    │  (Standby)     │
│                │    │                │
│  2 instances   │    │  2 instances   │
└────────────────┘    └────────────────┘

Deployment Steps:
1. Deploy v1.3.0 to Green ASG
2. Run health checks on Green
3. Shift 10% traffic to Green (canary)
4. Monitor metrics for 10 minutes
5. If OK, shift 100% to Green
6. Terminate Blue ASG
7. Green becomes new Blue
```

---

# Part IV: Development (D)

## 18. Development Workflow

### 18.1 Git Workflow (GitFlow)

```
main (production-ready)
  │
  ├─ release/v1.2.0
  │   │
  │   ├─ hotfix/fix-upload-bug
  │   │
  │   └─ (merge) → main (v1.2.1)
  │
  └─ develop (integration)
      │
      ├─ feature/upload-resume
      │   ├─ Commits: Implement resume logic
      │   └─ PR → develop
      │
      ├─ feature/photo-tagging
      │   ├─ Commits: Add tag CRUD
      │   └─ PR → develop
      │
      └─ (after testing) → release/v1.3.0 → main
```

**Branch Naming Convention:**
- `feature/description` - New features (e.g., `feature/photo-tagging`)
- `bugfix/description` - Non-critical bug fixes (e.g., `bugfix/ui-alignment`)
- `hotfix/description` - Critical production fixes (e.g., `hotfix/s3-upload-timeout`)
- `release/vX.Y.Z` - Release preparation branches

### 18.2 Pull Request Process

**PR Template:**

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Feature (new functionality)
- [ ] Bug fix (non-breaking fix)
- [ ] Breaking change (fix or feature that breaks existing functionality)
- [ ] Documentation update

## Checklist
- [ ] Code follows VSA pattern
- [ ] Domain logic is in aggregate/entity
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] API documentation updated
- [ ] No commented-out code
- [ ] No TODOs left in code

## Testing
- How was this tested?
- [ ] Local testing
- [ ] Integration tests pass
- [ ] Manual testing on staging

## Screenshots (if UI changes)
```

**Review Requirements:**
- **2 approvals** required for merge to `develop`
- **All CI checks pass** (build, tests, linting)
- **Code coverage** maintained or improved
- **No merge conflicts**

---

## 19. Code Organization

### 19.1 Backend Project Structure (Complete)

```
rapidphoto-backend/
├── src/
│   ├── main/
│   │   ├── java/com/rapidphoto/
│   │   │   ├── RapidPhotoApplication.java
│   │   │   │
│   │   │   ├── features/              ← VERTICAL SLICES
│   │   │   │   ├── initiateupload/
│   │   │   │   │   ├── InitiateUploadCommand.java
│   │   │   │   │   ├── InitiateUploadHandler.java
│   │   │   │   │   ├── InitiateUploadController.java
│   │   │   │   │   └── InitiateUploadResponse.java
│   │   │   │   │
│   │   │   │   ├── completeupload/
│   │   │   │   ├── trackprogress/
│   │   │   │   ├── addphototag/
│   │   │   │   └── ... (other slices)
│   │   │   │
│   │   │   ├── shared/
│   │   │   │   ├── domain/            ← SHARED DOMAIN MODELS
│   │   │   │   │   ├── UploadSession.java (Aggregate Root)
│   │   │   │   │   ├── Upload.java (Entity)
│   │   │   │   │   ├── Photo.java (Aggregate Root)
│   │   │   │   │   ├── User.java (Aggregate Root)
│   │   │   │   │   ├── S3Location.java (Value Object)
│   │   │   │   │   └── events/
│   │   │   │   │       ├── DomainEvent.java
│   │   │   │   │       ├── UploadCompletedEvent.java
│   │   │   │   │       └── SessionCompletedEvent.java
│   │   │   │   │
│   │   │   │   ├── infrastructure/     ← SHARED INFRASTRUCTURE
│   │   │   │   │   ├── config/
│   │   │   │   │   │   ├── DatabaseConfig.java
│   │   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   │   ├── WebSocketConfig.java
│   │   │   │   │   │   └── S3Config.java
│   │   │   │   │   ├── persistence/
│   │   │   │   │   │   ├── UploadRepository.java
│   │   │   │   │   │   ├── PhotoRepository.java
│   │   │   │   │   │   └── UserRepository.java
│   │   │   │   │   ├── s3/
│   │   │   │   │   │   └── S3PresignedUrlService.java
│   │   │   │   │   └── websocket/
│   │   │   │   │       └── WebSocketHandler.java
│   │   │   │   │
│   │   │   │   └── common/            ← SHARED UTILITIES
│   │   │   │       ├── Result.java
│   │   │   │       ├── ErrorCode.java
│   │   │   │       └── exceptions/
│   │   │   │           ├── DomainException.java
│   │   │   │           └── NotFoundException.java
│   │   │   │
│   │   │   └── RapidPhotoApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/           ← FLYWAY MIGRATIONS
│   │           ├── V1__create_users_table.sql
│   │           ├── V2__create_upload_sessions_table.sql
│   │           └── V3__create_photos_table.sql
│   │
│   └── test/
│       └── java/com/rapidphoto/
│           ├── features/
│           │   ├── initiateupload/
│           │   │   ├── InitiateUploadHandlerTest.java
│           │   │   └── InitiateUploadIntegrationTest.java
│           │   └── ... (other slices)
│           │
│           └── shared/
│               ├── domain/
│               │   └── UploadSessionTest.java
│               └── testcontainers/
│                   └── PostgresTestContainer.java
│
├── build.gradle
├── settings.gradle
└── README.md
```

### 19.2 Frontend Project Structure (React)

```
rapidphoto-web/
├── src/
│   ├── features/              ← FEATURE-BASED (matches backend VSA)
│   │   ├── auth/
│   │   │   ├── LoginForm.tsx
│   │   │   ├── RegistrationForm.tsx
│   │   │   ├── useAuth.ts
│   │   │   ├── authSlice.ts (Redux)
│   │   │   └── auth.test.tsx
│   │   │
│   │   ├── upload/
│   │   │   ├── UploadDropzone.tsx
│   │   │   ├── UploadProgress.tsx
│   │   │   ├── UploadManager.ts
│   │   │   ├── useUploadSession.ts
│   │   │   ├── uploadSlice.ts
│   │   │   └── upload.test.tsx
│   │   │
│   │   └── gallery/
│   │       ├── PhotoGrid.tsx
│   │       ├── Lightbox.tsx
│   │       ├── TagInput.tsx
│   │       ├── usePhotos.ts
│   │       ├── gallerySlice.ts
│   │       └── gallery.test.tsx
│   │
│   ├── shared/
│   │   ├── components/
│   │   │   ├── Button.tsx
│   │   │   ├── ProgressBar.tsx
│   │   │   ├── Modal.tsx
│   │   │   └── Spinner.tsx
│   │   ├── hooks/
│   │   │   ├── useWebSocket.ts
│   │   │   ├── useInfiniteScroll.ts
│   │   │   └── useDebounce.ts
│   │   ├── api/
│   │   │   ├── apiClient.ts
│   │   │   ├── uploadApi.ts
│   │   │   └── photoApi.ts
│   │   └── types/
│   │       ├── upload.types.ts
│   │       └── photo.types.ts
│   │
│   ├── app/
│   │   ├── store.ts (Redux store)
│   │   ├── App.tsx
│   │   ├── routes.tsx
│   │   └── theme.ts (Tailwind config)
│   │
│   ├── index.tsx
│   └── index.css
│
├── public/
├── package.json
├── tsconfig.json
└── tailwind.config.js
```

---

## 20. Testing Strategy

### 20.1 Test Pyramid

```
                    ▲
                   ╱ ╲
                  ╱   ╲
                 ╱  E2E ╲              ← 5% of tests (slow, brittle)
                ╱───────╲
               ╱         ╲
              ╱Integration╲             ← 20% of tests (medium speed)
             ╱─────────────╲
            ╱               ╲
           ╱   Unit Tests    ╲          ← 75% of tests (fast, isolated)
          ╱───────────────────╲
         ╱─────────────────────╲
```

### 20.2 Backend Testing

**Unit Tests (75%):**

```java
@ExtendWith(MockitoExtension.class)
class UploadSessionTest {

    @Test
    void shouldCompleteUploadAndRaiseEventWhenLastUploadCompletes() {
        // Given
        UploadSession session = createSession(totalFiles = 2);
        Upload upload1 = session.initiateUpload("file1.jpg", 1024);
        Upload upload2 = session.initiateUpload("file2.jpg", 2048);

        DomainEvents.clearEvents(); // Reset

        // When
        session.completeUpload(upload1.getId());
        session.completeUpload(upload2.getId());

        // Then
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getCompletedFiles()).isEqualTo(2);

        List<DomainEvent> events = DomainEvents.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(UploadSessionCompletedEvent.class);
    }

    @Test
    void shouldThrowExceptionWhenExceeding100Uploads() {
        // Given
        UploadSession session = createSession(totalFiles = 100);
        for (int i = 0; i < 100; i++) {
            session.initiateUpload("file" + i + ".jpg", 1024);
        }

        // When/Then
        assertThatThrownBy(() -> session.initiateUpload("file101.jpg", 1024))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Maximum 100 uploads");
    }
}
```

**Integration Tests (20%):**

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class InitiateUploadIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    WebTestClient webClient;

    @Autowired
    UploadSessionRepository sessionRepository;

    @Test
    void shouldInitiateUploadAndReturnPresignedUrl() {
        // Given
        UUID sessionId = createTestSession();
        InitiateUploadRequest request = new InitiateUploadRequest(
            sessionId,
            "test.jpg",
            1024L,
            "image/jpeg"
        );

        // When
        webClient.post()
            .uri("/api/v1/uploads/initiate")
            .headers(auth -> auth.setBearerAuth(getTestJwt()))
            .bodyValue(request)
            .exchange()

        // Then
            .expectStatus().isCreated()
            .expectBody(InitiateUploadResponse.class)
            .value(response -> {
                assertThat(response.uploadId()).isNotNull();
                assertThat(response.presignedUrl()).startsWith("https://");
                assertThat(response.presignedUrl()).contains("s3.amazonaws.com");
                assertThat(response.expiresIn()).isEqualTo(Duration.ofMinutes(15));
            });

        // Verify database state
        UploadSession session = sessionRepository.findById(sessionId).block();
        assertThat(session.getUploads()).hasSize(1);
    }
}
```

**End-to-End Tests (5%):**

```java
@SpringBootTest
@Testcontainers
class UploadE2ETest {

    @Test
    void shouldCompleteFullUploadWorkflow() {
        // Given: User with active session
        UUID userId = createTestUser();
        UUID sessionId = createUploadSession(userId, totalFiles = 1);

        // When: Initiate upload
        InitiateUploadResponse initResponse = initiateUpload(sessionId, "test.jpg");

        // And: Upload to S3 using pre-signed URL
        uploadToS3(initResponse.presignedUrl(), testImageBytes);

        // And: Complete upload
        completeUpload(initResponse.uploadId());

        // Then: Photo should be in gallery
        List<Photo> photos = getPhotos(userId);
        assertThat(photos).hasSize(1);
        assertThat(photos.get(0).getFileName()).isEqualTo("test.jpg");
    }
}
```

### 20.3 Frontend Testing

**Component Tests (Jest + React Testing Library):**

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { UploadDropzone } from './UploadDropzone';

describe('UploadDropzone', () => {
  it('should accept file drop and show preview', async () => {
    // Given
    const onFilesSelected = jest.fn();
    render(<UploadDropzone onFilesSelected={onFilesSelected} />);

    // When
    const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
    const dropzone = screen.getByTestId('dropzone');

    fireEvent.drop(dropzone, {
      dataTransfer: { files: [file] },
    });

    // Then
    expect(onFilesSelected).toHaveBeenCalledWith([file]);
    expect(screen.getByText('test.jpg')).toBeInTheDocument();
  });

  it('should reject files exceeding 100 limit', () => {
    // Given
    const onError = jest.fn();
    render(<UploadDropzone onError={onError} />);

    // When
    const files = Array.from({ length: 101 }, (_, i) =>
      new File(['test'], `file${i}.jpg`, { type: 'image/jpeg' })
    );
    const dropzone = screen.getByTestId('dropzone');
    fireEvent.drop(dropzone, { dataTransfer: { files } });

    // Then
    expect(onError).toHaveBeenCalledWith('Maximum 100 files per upload');
  });
});
```

---

## 21. Deployment Pipeline

### 21.1 CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/ci-cd.yml

name: CI/CD Pipeline

on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop]

jobs:
  backend-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run unit tests
        run: ./gradlew test

      - name: Run integration tests
        run: ./gradlew integrationTest

      - name: Code coverage
        run: ./gradlew jacocoTestReport

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

  frontend-test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Install dependencies
        run: npm ci

      - name: Run tests
        run: npm test

      - name: Build
        run: npm run build

  deploy-staging:
    needs: [backend-test, frontend-test]
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest

    steps:
      - name: Deploy to staging
        run: |
          # Deploy backend to staging EC2
          # Deploy frontend to staging S3 + CloudFront

  deploy-production:
    needs: [backend-test, frontend-test]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest

    steps:
      - name: Deploy to production (Blue-Green)
        run: |
          # Deploy to Green ASG
          # Run health checks
          # Shift traffic from Blue to Green
          # Terminate Blue ASG
```

---

## 22. Monitoring & Observability

### 22.1 Logging Strategy

**Structured Logging (Logback with JSON):**

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>sessionId</includeMdcKeyName>
            <includeMdcKeyName>uploadId</includeMdcKeyName>
        </encoder>
    </appender>

    <logger name="com.rapidphoto" level="INFO"/>
    <logger name="com.rapidphoto.features" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

**Application Code:**

```java
@Slf4j
@Service
public class UploadCommandHandler {
    public Mono<InitiateUploadResponse> handle(InitiateUploadCommand command) {
        MDC.put("sessionId", command.sessionId().toString());

        log.info("Initiating upload: fileName={}, fileSize={}",
            command.fileName(), command.fileSize());

        return sessionRepository.findById(command.sessionId())
            .switchIfEmpty(Mono.error(new SessionNotFoundException()))
            .flatMap(session -> {
                log.debug("Session found: status={}, totalFiles={}",
                    session.getStatus(), session.getTotalFiles());
                return processUpload(session, command);
            })
            .doOnSuccess(response ->
                log.info("Upload initiated successfully: uploadId={}", response.uploadId())
            )
            .doOnError(error ->
                log.error("Failed to initiate upload", error)
            )
            .doFinally(signal -> MDC.clear());
    }
}
```

### 22.2 Metrics (Micrometer + CloudWatch)

```java
@Service
public class UploadMetricsService {
    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter uploadInitiatedCounter;
    private final Counter uploadCompletedCounter;
    private final Counter uploadFailedCounter;

    // Timers
    private final Timer uploadDurationTimer;

    // Gauges
    private final AtomicInteger activeUploads = new AtomicInteger(0);

    public UploadMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.uploadInitiatedCounter = Counter.builder("uploads.initiated")
            .description("Total uploads initiated")
            .register(meterRegistry);

        this.uploadCompletedCounter = Counter.builder("uploads.completed")
            .description("Total uploads completed")
            .register(meterRegistry);

        this.uploadFailedCounter = Counter.builder("uploads.failed")
            .tag("reason", "unknown")
            .description("Total uploads failed")
            .register(meterRegistry);

        this.uploadDurationTimer = Timer.builder("uploads.duration")
            .description("Upload duration in seconds")
            .register(meterRegistry);

        Gauge.builder("uploads.active", activeUploads, AtomicInteger::get)
            .description("Currently active uploads")
            .register(meterRegistry);
    }

    public void recordUploadInitiated() {
        uploadInitiatedCounter.increment();
        activeUploads.incrementAndGet();
    }

    public void recordUploadCompleted(Duration duration) {
        uploadCompletedCounter.increment();
        uploadDurationTimer.record(duration);
        activeUploads.decrementAndGet();
    }
}
```

### 22.3 Distributed Tracing (AWS X-Ray)

```java
@Configuration
public class XRayConfig {

    @Bean
    public Filter TracingFilter() {
        return AWSXRayServletFilter.defaultFilter("RapidPhotoUpload");
    }
}

// Automatic instrumentation for:
// - HTTP requests
// - Database queries (R2DBC)
// - S3 SDK calls
// - WebSocket messages
```

---

## 23. Operational Excellence

### 23.1 Health Checks

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @GetMapping("/health")
    public Mono<ResponseEntity<HealthResponse>> health() {
        return Mono.just(ResponseEntity.ok(new HealthResponse("UP")));
    }

    @GetMapping("/health/ready")
    public Mono<ResponseEntity<HealthResponse>> readiness(
        @Autowired DataSource dataSource,
        @Autowired RedisConnectionFactory redis
    ) {
        // Check database connectivity
        boolean dbHealthy = checkDatabase(dataSource);

        // Check Redis connectivity
        boolean redisHealthy = checkRedis(redis);

        if (dbHealthy && redisHealthy) {
            return Mono.just(ResponseEntity.ok(new HealthResponse("READY")));
        } else {
            return Mono.just(ResponseEntity.status(503)
                .body(new HealthResponse("NOT_READY")));
        }
    }
}
```

### 23.2 Graceful Shutdown

```yaml
# application.yml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### 23.3 Database Migrations (Flyway)

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- V2__add_email_verification.sql
ALTER TABLE users
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN verification_token VARCHAR(255);
```

---

## Appendices

### Appendix A: Architecture Decision Records (ADRs)

**ADR-001: Use Spring WebFlux for Backend**

- **Status:** Accepted
- **Context:** Need to handle 100 concurrent uploads with non-blocking I/O
- **Decision:** Use Spring WebFlux (reactive) instead of Spring MVC
- **Consequences:**
  - ✅ Better concurrency with fewer threads
  - ❌ Steeper learning curve for team
  - ❌ More complex debugging

**ADR-002: Use React Native for Web (Unified Codebase)**

- **Status:** Accepted
- **Context:** Need to support web, iOS, and Android with maximum development efficiency and AI-assisted development
- **Decision:** Use React Native with react-native-web to create a single codebase for all platforms
- **Consequences:**
  - ✅ 100% code reuse across web, iOS, and Android (single component library)
  - ✅ Same language (TypeScript) across entire stack (frontend + backend)
  - ✅ AI development efficiency: Claude generates once, deploys everywhere
  - ✅ Single team, single codebase, single state management system
  - ✅ Proven at scale: Twitter/X, Uber Eats, Major League Soccer
  - ⚠️ Larger web bundle (~200KB overhead, acceptable for authenticated app)
  - ⚠️ No semantic HTML (acceptable - no SEO requirements)
- **Performance Validation:** React Native for Web can handle 100 concurrent file operations using background tasks and efficient state management
- **AI Development Impact:** Single codebase dramatically improves Claude's ability to generate consistent, cross-platform code

**ADR-003: Direct-to-S3 Uploads with Pre-signed URLs**

- **Status:** Accepted
- **Context:** Server bandwidth would be bottleneck for 100 concurrent uploads
- **Decision:** Use pre-signed URLs for direct client-to-S3 uploads
- **Consequences:**
  - ✅ Zero server bandwidth for uploads
  - ✅ Unlimited concurrent uploads
  - ❌ More complex client-side logic

---

### Appendix B: Glossary

| Term | Definition |
|------|-----------|
| **Aggregate** | DDD pattern: cluster of domain objects treated as a single unit |
| **CQRS** | Command Query Responsibility Segregation |
| **DDD** | Domain-Driven Design |
| **Pre-signed URL** | Time-limited S3 URL for direct upload/download |
| **R2DBC** | Reactive Relational Database Connectivity |
| **VSA** | Vertical Slice Architecture |
| **WebFlux** | Spring's reactive web framework |

---

### Appendix C: References

1. Domain-Driven Design: Tackling Complexity in the Heart of Software (Eric Evans)
2. Implementing Domain-Driven Design (Vaughn Vernon)
3. Building Microservices (Sam Newman)
4. Spring WebFlux Documentation: https://docs.spring.io/spring-framework/reference/web/webflux.html
5. AWS Well-Architected Framework: https://aws.amazon.com/architecture/well-architected/

---

**END OF ARCHITECTURE DOCUMENT**

---

## Approval Signatures

| Role | Name | Date | Signature |
|------|------|------|-----------|
| **Solution Architect** | _________ | ______ | _________ |
| **Technical Lead** | _________ | ______ | _________ |
| **Backend Lead** | _________ | ______ | _________ |
| **Frontend Lead** | _________ | ______ | _________ |
| **DevOps Lead** | _________ | ______ | _________ |

**Document Status:** DRAFT FOR ARCHITECTURE REVIEW
**Next Review Date:** [Date]
