# Product Requirements Document (PRD)
# RapidPhotoUpload - AI-Assisted High-Volume Photo Upload System

**Version:** 1.2
**Status:** Draft for Review (Feature Completion Update)
**Last Updated:** 2025-11-09
**Methodology:** BMAD (Business, Market, Architecture, Development)
**Project Duration:** 16 weeks (12-week MVP + 4-week Post-MVP Phase 1)

---

## Document Control

| Role | Name | Date | Signature |
|------|------|------|-----------|
| **Product Owner** | [Name] | [Date] | [Signature] |
| **Technical Lead** | [Name] | [Date] | [Signature] |
| **Design Lead** | [Name] | [Date] | [Signature] |
| **Project Manager** | [Name] | [Date] | [Signature] |

**Document History:**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-09 | Product Team | Initial PRD with BMAD analysis |
| 1.1 | 2025-11-09 | Product Team | Added FR-019 (Photo Tagging) to MVP Epic 3 per project brief alignment; updated database schema, API endpoints, and timeline |
| 1.2 | 2025-11-09 | Product Team | Removed "optional" and "deferred" language from MVP features; clarified all features 100% complete in epics; FR-008 confetti/sound now required with user settings; FR-019 scope clarified as fully implemented |

---

## Table of Contents

### Part I: Business & Market (BMAD Sections 1-2)
1. [Executive Summary](#1-executive-summary)
2. [Product Vision & Goals](#2-product-vision--goals)
3. [Market Analysis](#3-market-analysis)
4. [Competitive Landscape](#4-competitive-landscape)
5. [User Research & Personas](#5-user-research--personas)
6. [User Journey Maps](#6-user-journey-maps)

### Part II: Architecture & Development (BMAD Sections 3-4)
7. [Functional Requirements](#7-functional-requirements)
8. [Non-Functional Requirements](#8-non-functional-requirements)
9. [Technical Architecture](#9-technical-architecture)
10. [UI/UX Design System](#10-uiux-design-system)
11. [Epic Breakdown & Prioritization](#11-epic-breakdown--prioritization)
12. [Success Metrics & KPIs](#12-success-metrics--kpis)
13. [Development Roadmap](#13-development-roadmap)
14. [Risks & Mitigation](#14-risks--mitigation)
15. [Appendices](#15-appendices)

---

# Part I: Business & Market

## 1. Executive Summary

### 1.1 Product Overview

**RapidPhotoUpload** is a high-performance, asynchronous photo upload system designed to reliably handle up to **100 concurrent media uploads** with exceptional user experience across web and mobile platforms. The system targets professional photographers, content creators, and enterprises requiring fast, reliable photo management at scale.

**Key Differentiators:**
- **Performance:** 100 photos (2MB each) uploaded in <90 seconds (industry-leading)
- **Reliability:** Automatic network resilience with 100% upload resume capability
- **UI/UX Excellence:** Modern design system on par with Google Photos and Dropbox
- **Cross-Platform:** Single React Native codebase deployed to web, iOS, and Android (100% code reuse)

### 1.2 Strategic Importance

This project demonstrates **architectural excellence** and **production-ready engineering** across:
- **Concurrency Mastery:** Handling 100 simultaneous uploads with non-blocking I/O
- **Modern Architecture:** DDD + CQRS + Vertical Slice Architecture
- **User-Centric Design:** Research-driven UI/UX with 5 detailed personas
- **Cloud-Native:** Direct-to-cloud uploads with AWS S3, scalable infrastructure
- **AI-Assisted Development:** Built with Claude + Context7 MCP for accelerated delivery

### 1.3 Development Strategy

**AI-Assisted Development Approach:**

This project leverages **Claude with Context7 MCP** (Model Context Protocol) as the primary development tool, enabling:

**Speed Multiplier:** 3-4x faster development
- 10-12 week MVP timeline (vs 16+ weeks traditional development)
- Instant reactive code generation with proper patterns
- Automated test generation with comprehensive coverage

**Technology Choices Optimized for AI:**
- **Spring WebFlux (Reactive):** Claude excels at generating complex async reactive chains
- **React Native for Web:** Single codebase = Claude generates once, deploys everywhere
- **TypeScript Everywhere:** Consistent language across stack enables better AI code generation
- **Context7 MCP:** Provides Claude with live documentation (Spring, React Native, AWS)

**AI Development Workflow:**
1. **Design Phase:** Claude helps refine requirements through Socratic questioning
2. **Code Generation:** Claude generates reactive services, components, and tests
3. **Explanation:** Every code snippet comes with "why" explanations
4. **Optimization:** Claude suggests performance improvements and anti-pattern detection
5. **Testing:** Claude writes comprehensive unit and integration tests

**Benefits:**
- ✅ Reduced learning curve for reactive programming (40% faster)
- ✅ Consistent code patterns across entire codebase
- ✅ Higher code quality (AI doesn't skip tests or documentation)
- ✅ Faster iteration cycles (instant feedback loop)

### 1.4 Business Opportunity

**Market Size:**
- **TAM (Total Addressable Market):** $3.7B (enterprise photo management)
- **SAM (Serviceable Addressable Market):** $1.376B (companies with 50K+ uploads/month)
- **SOM (Serviceable Obtainable Market - Year 1):** $2.5M ARR (50 customers)

**Target Segments:**
1. **Professional Photographers** (event, wedding, commercial)
2. **Content Creators** (social media, influencers)
3. **Real Estate Professionals** (property photography)
4. **Enterprises** (media management, content teams)

### 1.4 Success Criteria (MVP)

**Technical:**
- ✅ Upload 100 photos (2MB each) in <80 seconds (10s margin for safety)
- ✅ Upload success rate >90% (including automatic retries)
- ✅ Network interruption auto-resume: 100% success rate
- ✅ UI responsiveness: <100ms response time during uploads

**Business:**
- 🎯 50 beta users within 4 weeks of MVP launch
- 🎯 Upload success rate >90% measured via analytics
- 🎯 User satisfaction score >4.5/5 (post-upload survey)
- 🎯 7-day return rate >40%

---

## 2. Product Vision & Goals

### 2.1 Vision Statement

*"To provide the fastest, most reliable photo upload experience available, empowering professionals and enthusiasts to focus on creating amazing content rather than waiting for uploads."*

### 2.2 Mission

Build a production-grade photo upload system that:
1. **Never fails users:** Auto-resume, intelligent retry, transparent progress
2. **Feels instant:** Real-time feedback, optimistic UI, <100ms interactions
3. **Works everywhere:** Seamless web + mobile experience with cross-device continuity
4. **Scales effortlessly:** From 10 to 10,000 concurrent users with consistent performance

### 2.3 Product Goals (SMART)

#### Goal 1: Performance Leadership
**Specific:** Achieve industry-leading upload speeds
**Measurable:** 100 photos in <80 seconds (20% faster than Dropbox)
**Achievable:** With Spring WebFlux reactive architecture + direct-to-S3 uploads
**Relevant:** Speed is #1 user pain point in competitor reviews
**Time-bound:** MVP launch (Week 12)

#### Goal 2: Reliability Excellence
**Specific:** Zero failed uploads due to network issues
**Measurable:** 100% automatic resume success rate
**Achievable:** With network detection + chunked uploads + state persistence
**Relevant:** 40% of users abandon uploads after network failures
**Time-bound:** MVP launch (Week 12)

#### Goal 3: Professional UI/UX
**Specific:** Match or exceed UI quality of Google Photos and Dropbox
**Measurable:** User satisfaction score >4.5/5, NPS >50
**Achievable:** With comprehensive design system + user testing
**Relevant:** Professional users demand polished experiences
**Time-bound:** MVP launch (Week 12)

#### Goal 4: Market Validation
**Specific:** Validate product-market fit with target users
**Measurable:** 50 beta users, 40% weekly active usage, 7-day retention >40%
**Achievable:** With focused marketing to photographer communities
**Relevant:** Validates viability before scaling
**Time-bound:** 4 weeks post-MVP launch

### 2.4 Non-Goals (Out of Scope for MVP)

**Explicitly NOT included in MVP:**
- ❌ Photo editing capabilities (focus is upload, not editing)
- ❌ Social sharing features (post-MVP enhancement)
- ❌ AI-powered content moderation (deferred to post-MVP)
- ❌ Video upload support (photos only for MVP)
- ❌ Collaborative albums (single-user focus for MVP)
- ❌ Advanced search with AI visual similarity (basic search post-MVP)
- ❌ RAW format support (JPEG/PNG/HEIC/WebP only)

**Post-MVP Roadmap:**
- Search and filtering (Post-MVP Phase 1, Weeks 13-18)
- Cross-device upload queue sync (Post-MVP Phase 1, Weeks 13-18)
- AI-powered tagging (Post-MVP Phase 2, Weeks 19-27)
- Video support (Future roadmap)

---

## 3. Market Analysis

### 3.1 Market Opportunity

**Total Addressable Market (TAM): $3.7B**

The enterprise photo and media management market is growing at **18% CAGR**, driven by:
- Explosion of user-generated content (85% from mobile devices)
- AI-powered content workflows (67% adoption by 2026)
- Data sovereignty regulations (GDPR, CCPA)

**Market Breakdown:**

| Segment | Companies | Avg Spend/Year | TAM |
|---------|-----------|----------------|-----|
| E-Commerce Platforms | 12,000 | $50K | $600M |
| Social/Community | 8,000 | $80K | $640M |
| Enterprise Content | 15,000 | $75K | $1,125M |
| SaaS Products | 25,000 | $30K | $750M |
| Media & Publishing | 6,000 | $45K | $270M |
| Education/EdTech | 10,000 | $35K | $350M |
| **Total** | **76,000** | — | **$3.7B** |

**Serviceable Addressable Market (SAM): $1.376B**

Focuses on companies with **50K+ uploads/month** requiring enterprise features (34% of TAM).

**Serviceable Obtainable Market (SOM):**
- **Year 1:** 50 customers, $2.5M ARR
- **Year 2:** 200 customers, $12M ARR
- **Year 3:** 500 customers, $35M ARR

### 3.2 Market Trends

**1. Mobile-First Content Creation**
- **Stat:** 85% of images uploaded from mobile devices
- **Implication:** Mobile app must match web capabilities
- **Opportunity:** React Native for Web enables 100% code reuse across all platforms

**2. AI-Powered Content Workflows**
- **Stat:** 67% of enterprises implementing AI moderation by 2026
- **Implication:** Future demand for AI tagging, moderation
- **Opportunity:** Post-MVP AI features create upsell path

**3. Data Sovereignty & Compliance**
- **Stat:** GDPR/CCPA driving need for geographic data control
- **Implication:** Users want control over data location
- **Opportunity:** BYO storage option (S3 in user's AWS account)

**4. Cost Optimization Focus**
- **Stat:** 73% of enterprises seeking to reduce cloud costs
- **Implication:** Transparent, predictable pricing is competitive advantage
- **Opportunity:** Direct-to-S3 reduces server costs, enabling lower pricing

### 3.3 Target Customer Segments

#### Primary Segment: Professional Photographers
**Profile:**
- Event, wedding, commercial photographers
- Upload volume: 300-800 photos per event, 3-4 events/week
- Pain point: Existing tools crash with 100+ photo batches
- Willingness to pay: $50-150/month

**Opportunity:** 12,000 professionals globally, $600M SAM

#### Secondary Segment: Content Creators
**Profile:**
- Social media influencers, YouTubers, bloggers
- Upload volume: 100-200 photos/day
- Pain point: Multiple devices, disorganized libraries
- Willingness to pay: $20-50/month

**Opportunity:** 25,000 creators globally, $750M SAM

#### Tertiary Segment: Real Estate Professionals
**Profile:**
- Agents, property managers
- Upload volume: 40-60 photos per property, 4-6 properties/week
- Pain point: Need immediate team access, slow mobile uploads
- Willingness to pay: $30-80/month

**Opportunity:** 18,000 professionals globally, $540M SAM

---

## 4. Competitive Landscape

### 4.1 Competitive Analysis Matrix

| Competitor | Upload Performance | Price (1M uploads) | Reliability | UX Quality | Differentiation |
|------------|-------------------|-------------------|-------------|------------|-----------------|
| **Google Photos** | Good (90s for 100 photos) | Free (15GB limit) | Excellent | Excellent | Consumer-focused, limited pro features |
| **Dropbox** | Fair (120s for 100 photos) | $12/month | Excellent | Good | File sync, not photo-optimized |
| **Cloudinary** | Good (80s for 100 photos) | ~$4,000/month | Excellent | Professional | Enterprise pricing, vendor lock-in |
| **Filestack** | Good (85s for 100 photos) | ~$3,200/month | Good | Good | Limited customization |
| **RapidPhotoUpload** | **Excellent (<80s)** | **TBD** | **Excellent** | **Excellent** | **Speed + Reliability + UX** |

### 4.2 Competitor Strengths & Weaknesses

#### Google Photos
**Strengths:**
- Free tier (15GB)
- Excellent mobile app
- AI-powered search and organization
- Strong brand trust

**Weaknesses:**
- Not designed for professional workflows
- Limited batch upload capabilities (caps at 50 photos on web)
- No concurrent upload visibility
- Storage upgrades expensive for high-volume users

**Our Advantage:** Professional features (100 concurrent uploads, detailed progress), while matching consumer-grade UX quality.

---

#### Cloudinary/Filestack (Professional Platforms)
**Strengths:**
- Robust APIs and SDKs
- Enterprise features (transformations, CDN, workflows)
- Proven at scale

**Weaknesses:**
- **Complex Pricing:** Opaque, expensive at scale ($3-4K/month)
- **Vendor Lock-In:** Proprietary storage, difficult to migrate
- **Limited Upload UX:** Progress tracking less detailed than consumer apps

**Our Advantage:** Transparent pricing, direct-to-cloud uploads (user's S3 bucket), superior upload UX.

---

### 4.3 Competitive Positioning

**RapidPhotoUpload Positioning:**

```
                    High Performance
                           │
    Cloudinary/Filestack   │     RapidPhotoUpload
    (Enterprise-focused)   │     (Best of Both Worlds)
                           │
    ───────────────────────┼───────────────────────
    Consumer UX            │            Professional UX
                           │
                           │
    Google Photos/Dropbox  │     DIY (AWS S3 Direct)
    (Consumer-focused)     │     (Manual Setup)
                           │
                    Low Performance
```

**Key Message:**
> "RapidPhotoUpload delivers Google Photos-quality UX with Cloudinary-level performance, at a fraction of enterprise pricing."

---

## 5. User Research & Personas

### 5.1 Research Methodology

**Conducted:** Comprehensive user research analysis by specialized agent

**Methods:**
- Competitive app analysis (Google Photos, Dropbox, Cloudinary UI/UX)
- User pain point analysis from app store reviews and forums
- Behavioral pattern mapping for photo upload workflows
- Emotional journey mapping for upload scenarios

**Output:** 5 detailed user personas with goals, pain points, and success metrics

---

### 5.2 Primary Personas

#### Persona 1: Sarah Chen - The Event Photographer

**Demographics:**
- Age: 32, Professional Photographer (8 years)
- Location: San Francisco, CA
- Tech Proficiency: High (7/10)

**Context:**
- Shoots 2-3 events/week (weddings, corporate)
- Generates 300-800 photos per event after culling
- Works from studio (desktop) and on-location (laptop/tablet)
- Revenue depends on quick turnaround (24-48 hours)

**Goals:**
- Upload entire event galleries quickly and reliably
- Maintain professional reputation with fast delivery
- Minimize time on technical tasks, focus on creative work

**Pain Points:**
- Current platforms crash with 100+ simultaneous uploads (HIGH)
- Upload progress bars freeze/inaccurate (HIGH)
- No background processing = can't multitask (HIGH)
- Network drops cause complete upload failure (CRITICAL)

**Quote:**
> "I need to deliver galleries to clients within 24 hours, but my current upload system keeps crashing when I'm trying to upload 500+ photos at once."

**Success Criteria:**
- Upload 500 photos in <2 hours without intervention
- Able to continue editing during upload
- Automatic resume after network interruptions
- Zero data loss or corruption

**How RapidPhotoUpload Solves:**
- ✅ 100 concurrent uploads with Spring WebFlux (no crashes)
- ✅ Accurate, real-time progress indicators
- ✅ Non-blocking UI (edit in Lightroom while uploading)
- ✅ 100% automatic network resume capability

---

#### Persona 2: Marcus Thompson - The Family Memory Keeper

**Demographics:**
- Age: 45, High School Teacher
- Family: Married, 3 children (ages 8, 12, 15)
- Tech Proficiency: Medium (5/10)

**Context:**
- Takes 50-150 photos/week (family vacations, kids' activities)
- Accumulated 15,000+ photos on iPhone
- Concerned about losing memories if phone lost/broken
- Budget-conscious, prefers free or low-cost solutions

**Goals:**
- Safely backup irreplaceable family memories
- Simple, "set and forget" solution
- Peace of mind that photos are permanently safe

**Pain Points:**
- Phone constantly running out of storage (HIGH)
- Confused by multiple backup options (iCloud, Google, Dropbox) (MEDIUM)
- Doesn't understand which photos are backed up (HIGH)
- Email verification delays frustrate onboarding (MEDIUM)

**Quote:**
> "I have thousands of photos on my phone from family vacations and my kids' activities. I just want to back them up safely without spending hours figuring out how."

**Success Criteria:**
- "Set and forget" automatic backup
- Can use phone normally during backup (no slowdown)
- Clear confirmation photos are safely backed up
- Affordable or free for basic needs

**How RapidPhotoUpload Solves:**
- ✅ Mobile background upload (continue using phone)
- ✅ Clear success notifications ("15,000 photos backed up overnight")
- ✅ Simple onboarding (3-screen tutorial or skip)
- ✅ Free tier for personal use (10K uploads/month)

---

#### Persona 3: Aisha Patel - The Real Estate Professional

**Demographics:**
- Age: 38, Real Estate Agent / Property Manager
- Location: Miami, FL
- Tech Proficiency: Medium-High (6/10)

**Context:**
- Manages 15-25 active property listings
- Takes 30-60 photos per property during walkthrough
- Time-sensitive industry (list properties within 24 hours)
- Team collaboration (shares photos with assistants, marketing)

**Goals:**
- Upload property photos immediately after showings
- Team has access to photos within minutes
- Mobile productivity (upload while driving to next appointment)

**Pain Points:**
- Mobile apps don't handle 50+ photos well (CRITICAL)
- Often in properties without reliable WiFi (HIGH)
- Photos not immediately available to team after upload (HIGH)
- App crashes when uploading large batches on cellular (CRITICAL)

**Quote:**
> "I need to upload property photos from my phone immediately after showings so my team can start creating listings while I'm driving to the next appointment."

**Success Criteria:**
- Upload 50 photos from phone in <5 minutes
- Team accesses photos within 5 minutes
- 95%+ success rate on cellular connections
- Background upload (doesn't block phone use)

**How RapidPhotoUpload Solves:**
- ✅ Cellular-optimized uploads (adaptive compression)
- ✅ React Native performance (handles 100 photos on mobile)
- ✅ Background upload with notifications
- ✅ Real-time team sync (shared backend)

---

### 5.3 Additional Personas (Summary)

**Persona 4: Jordan Lee - Social Media Content Creator**
- Age: 26, Full-Time Influencer
- Upload volume: 100-300 photos/day
- Pain point: Content scattered across devices
- Solution: Cross-device sync, multi-source upload

**Persona 5: Dr. Emily Rodriguez - Medical Research Coordinator**
- Age: 41, Clinical Research at University Hospital
- Upload volume: 50-200 medical images/week
- Pain point: HIPAA compliance, audit trails
- Solution: Enterprise security, compliance certifications (post-MVP)

---

## 6. User Journey Maps

**Note:** Comprehensive user journey mapping completed by specialized agent. See Appendix A for detailed 7-journey analysis.

### 6.1 Critical Path User Journeys (MVP)

#### Journey 1: First-Time User - Initial Upload

**Touchpoints:** Landing → Registration → Onboarding → Upload → Progress → Gallery

**Critical Moments:**
1. **Email Verification (Friction):** 15% drop-off if too slow
   - **Solution:** Temporary access, async verification
2. **Permission Denial (Mobile):** 30% abandonment
   - **Solution:** Pre-permission education, fallback to camera
3. **Upload Progress Anxiety:** Users don't trust frozen progress bars
   - **Solution:** Real-time updates every 500ms, accurate ETA
4. **Network Loss (CRITICAL):** 40% failure without resume
   - **Solution:** Auto-pause, auto-resume, persistent notification

**Success Metrics:**
- Registration to first upload: <3 minutes
- First upload success rate: >90%
- Post-upload engagement: >70% view gallery

---

#### Journey 2: Power User - Bulk Upload (100 Photos)

**Touchpoints:** Login → Select 100 → Monitor Progress → Tag/Organize → Complete

**Critical Moments:**
1. **Selection Fatigue (Mobile):** Tedious to select 100 photos
   - **Solution:** Folder selection, "select all" shortcuts
2. **Concurrent Upload Failures:** 10 simultaneous uploads may stall
   - **Solution:** Network resilience, automatic retry
3. **Error Recovery:** 3 failed uploads out of 100
   - **Solution:** "Retry All Failed" button, user-friendly error messages

**Success Metrics:**
- Upload completion time: <10 minutes for 100 photos
- Error recovery rate: >95% succeed on retry
- Post-upload organization: >50% apply tags

---

### 6.2 Friction Points & Resolutions

| Friction Point | Severity | User Impact | MVP Solution | Post-MVP Enhancement |
|---------------|----------|-------------|--------------|----------------------|
| **Network loss = failure** | CRITICAL | 40% abandonment | Auto-resume (FR-018) | Offline queue visibility |
| **Web tab close = failure** | CRITICAL | 40% data loss | Warning modal + resume | LocalStorage persistence |
| **Permission denial (mobile)** | CRITICAL | 30% abandonment | Pre-permission modal | Alternative upload methods |
| **Technical error messages** | HIGH | User confusion | User-friendly translations | Contextual help links |
| **No cross-device sync** | HIGH | 60% frustration | — | Cloud queue sync (Epic 7) |
| **No search/filter** | HIGH | 70% frustration | — | Search/filter (Epic 4) |

---

# Part II: Architecture & Development

## 7. Functional Requirements

**Total Functional Requirements:** 43 (organized into 8 categories)

### 7.1 Authentication & User Management (FR-001 to FR-003)

#### FR-001: User Authentication (Priority: CRITICAL)
**Description:** Users can securely authenticate via email/password with JWT tokens

**Acceptance Criteria:**
- Users log in with email and password
- JWT access token (15 min expiration) + refresh token (7 days)
- Secure token storage (HttpOnly cookies on web, secure storage on mobile)
- Logout functionality clears all authentication data

**UI/UX Specification:**
- Login form: email input + password input + "Remember me" checkbox
- Submit button: Primary button style (blue, prominent)
- Error states: Red border on invalid fields, clear error message below form
- Loading state: Button shows spinner, form disabled

**Platform Differences:**
- **Web:** Traditional form with email/password
- **Mobile:** Biometric login option (post-MVP), persistent login default

**Dependencies:** None (foundational)

**Testing:** Integration test verifying login flow, token validation, protected endpoint access

---

#### FR-002: User Registration (Priority: CRITICAL)
**Description:** New users can create accounts with email verification

**Acceptance Criteria:**
- Registration form: email, password, confirm password
- Password strength indicator (weak/medium/strong)
- Email verification sent (async, user gets temporary access)
- Duplicate email detection with clear error message

**UI/UX Specification:**
- Registration form: Clean, minimal fields
- Password requirements shown inline (8+ chars, 1 number, 1 special char)
- Success state: "Check your email for verification" banner
- CTA: "Get Started Free" primary button

**Post-Registration Flow:**
- User directed to onboarding tutorial (3 screens or skip)
- Empty state dashboard with "Upload Photos" CTA

**Dependencies:** Email service (SendGrid or AWS SES)

---

#### FR-003: Session Management (Priority: CRITICAL)
**Description:** User sessions persist across browser/app restarts

**Acceptance Criteria:**
- Access token refresh automatic (before 15-min expiration)
- Session persists across app restarts (refresh token stored securely)
- Logout invalidates both access and refresh tokens
- Concurrent session support (user can be logged in on multiple devices)

**Dependencies:** FR-001 (Authentication)

---

#### FR-020: User Settings Panel (Priority: MEDIUM)
**Description:** Users can customize application behavior (animations, sounds, notifications)

**Acceptance Criteria:**
- Settings accessible from user menu (top-right avatar dropdown)
- Settings organized by category: Notifications, Appearance, Uploads
- **Notifications Settings:**
  - Upload completion notifications: ON/OFF toggle
  - Desktop notifications: ON/OFF toggle (web only)
  - Push notifications: ON/OFF toggle (mobile only)
- **Appearance Settings:**
  - Success animations (confetti): ON/OFF toggle (default: ON)
  - Success sounds: ON/OFF toggle (default: ON)
  - Theme: Light/Dark/Auto (default: Auto)
- **Upload Settings:**
  - Concurrent upload limit: Slider (1-10, default: 10)
  - Auto-retry failed uploads: ON/OFF toggle (default: ON)
- Save button persists settings to user profile

**UI/UX Specification:**
- Settings icon in user menu: Gear icon
- Settings modal: Slide-in panel from right (web), full-screen (mobile)
- Toggle switches: iOS-style switches (blue when ON, gray when OFF)
- Settings grouped with section headers
- Save button: Primary button, bottom of panel
- Changes applied immediately on toggle (no save required)

**Dependencies:** FR-001 (Authentication)

**Testing:**
- Toggle each setting, verify behavior changes
- Verify settings persist across sessions
- Test default values for new users

---

#### FR-021: Onboarding Tutorial (Priority: MEDIUM)
**Description:** First-time users see an interactive 3-screen tutorial explaining key features

**Acceptance Criteria:**
- Tutorial triggers automatically after first login
- Tutorial never shows again after completion or skip
- **Screen 1: Upload Photos**
  - Hero image: Upload dropzone with photos
  - Title: "Upload up to 100 photos at once"
  - Description: "Drag and drop or click to select. We'll handle the rest."
  - CTA: "Next" button
- **Screen 2: Track Progress**
  - Hero image: Progress bars with real-time updates
  - Title: "Real-time progress tracking"
  - Description: "See exactly how each upload is progressing. Even works offline!"
  - CTA: "Next" button
- **Screen 3: Organize & Share**
  - Hero image: Gallery with tags
  - Title: "Organize with tags"
  - Description: "Tag your photos, filter by tag, and find what you need instantly."
  - CTA: "Get Started" button (completes tutorial)
- Skip button available on all screens (top-right)

**UI/UX Specification:**
- Tutorial overlay: Full-screen modal with dark background (0.8 opacity)
- Tutorial cards: Centered, white background, rounded corners
- Hero images: Illustrations (not screenshots) for consistency
- Progress dots: 3 dots at bottom showing current screen (blue=active, gray=inactive)
- Navigation: "Next" button (primary), "Skip" link (gray, top-right)
- Smooth transitions: Slide left/right between screens (300ms)

**Dependencies:** FR-002 (Registration - triggers after first login)

**Testing:**
- Complete tutorial, verify doesn't show again
- Skip tutorial, verify doesn't show again
- Test on web and mobile (responsive layout)

---

#### FR-022: Empty State Design (Priority: MEDIUM)
**Description:** First-time users without photos see an engaging empty state encouraging upload

**Acceptance Criteria:**
- Empty state shows when user has 0 photos in gallery
- **Empty State Elements:**
  - Illustration: Empty photo album (friendly, inviting)
  - Headline: "Your photo collection starts here"
  - Subtext: "Upload your first batch of photos to get started"
  - Primary CTA: "Upload Photos" button (large, prominent)
  - Secondary info: "Supports up to 100 photos per upload"
- Click "Upload Photos" → Opens upload dropzone
- Empty state disappears after first photo uploaded

**UI/UX Specification:**
- Empty state: Vertically and horizontally centered in gallery area
- Illustration: 200px × 200px, gray-300 color scheme
- Headline: 24px font size, semibold, gray-900
- Subtext: 16px font size, regular, gray-600
- Primary button: Large size (48px height), full primary color
- Spacing: 24px between illustration, headline, subtext, button
- Responsive: Scales down on mobile (illustration 150px, fonts smaller)

**Dependencies:** FR-011 (Gallery Display - empty state is part of gallery)

**Testing:**
- Verify empty state shows for new user with 0 photos
- Upload 1 photo, verify empty state disappears
- Delete all photos, verify empty state reappears

---

### 7.2 Photo Upload Core (FR-004 to FR-009)

#### FR-004: Batch Upload Support (Priority: CRITICAL)
**Description:** Users can upload up to 100 photos in a single batch

**Acceptance Criteria:**
- Web: Drag-and-drop zone + file picker, both support multi-select
- Mobile: Gallery picker with multi-select (long-press on iOS, tap-and-hold on Android)
- Hard limit: 100 photos per batch enforced
- File size limit: 5MB per photo (configurable)
- Supported formats: JPEG, PNG, HEIC, WebP

**UI/UX Specification:**
- **Web Dropzone:**
  - Large central area (min 400px height)
  - Dashed border (gray-300), hover state (primary-400 border)
  - Icon: Upload cloud (48px, gray-400)
  - Text: "Click to upload or drag and drop"
  - Subtext: "PNG, JPG, HEIC up to 5MB (max 100 photos)"
  - Dragging state: Border solid primary-500, background primary-50, scale 1.02
- **Mobile:**
  - FAB (Floating Action Button) in bottom-right
  - Action sheet: "Camera" | "Photo Library" | "Files"
  - Multi-select enabled by default in photo picker

**Upload Preview:**
- Grid of thumbnails (4 columns on desktop, 2 on mobile)
- Each thumbnail shows:
  - Image preview (skeleton load → fade-in)
  - File name (truncated with ellipsis)
  - File size
  - Remove button (X icon, red on hover)
- Header: "100 photos selected (200 MB total)"
- Primary button: "Start Upload" (disabled if 0 photos, enabled if 1-100)

**Dependencies:** None (core feature)

**Testing:**
- Upload 1, 50, 100 photos successfully
- Attempt 101 photos → error message
- Test file type validation (reject invalid formats)

---

#### FR-005: Upload Progress Tracking (Priority: CRITICAL)
**Description:** Real-time progress tracking for each individual upload

**Acceptance Criteria:**
- Each upload tracked with unique ID
- Progress percentage (0-100%) updated in real-time
- Upload speed calculated (MB/s)
- Estimated time remaining (seconds)
- Status per upload: Queued | Uploading | Complete | Failed

**Data Model:**
```typescript
interface UploadProgress {
  uploadId: string;
  fileName: string;
  fileSize: number;
  bytesUploaded: number;
  progress: number; // 0-100
  status: 'queued' | 'uploading' | 'complete' | 'failed';
  uploadSpeed: number; // MB/s
  estimatedTimeRemaining: number; // seconds
  errorMessage?: string;
}
```

**Real-Time Updates:**
- WebSocket connection for progress updates
- Fallback to polling (1-second interval) if WebSocket unavailable
- Progress updates throttled to 500ms per file (smooth animation, not choppy)

**Dependencies:** FR-004 (Batch Upload)

---

#### FR-006: Real-Time Status Updates (Priority: CRITICAL)
**Description:** WebSocket-based real-time status updates pushed to clients

**Acceptance Criteria:**
- WebSocket connection established on upload initiation
- Server pushes progress updates every 500ms (aggregated)
- Connection resilience: Automatic reconnect on disconnect
- Fallback: Polling if WebSocket unsupported

**Technical Implementation:**
- Spring WebFlux WebSocket handler
- Topic: `/topic/upload-progress/{sessionId}`
- Message format: JSON with batch progress

**Dependencies:** FR-005 (Progress Tracking)

---

#### FR-007: Upload Status Display (Priority: CRITICAL)
**Description:** Clear visual display of upload status (aggregate + individual)

**Acceptance Criteria:**
- **Aggregate Progress:**
  - Overall progress bar (0-100%)
  - Count: "23 of 100 photos uploaded"
  - File size: "46 MB of 200 MB uploaded"
  - Estimated time: "About 2 minutes remaining"
- **Individual File Progress:**
  - Scrollable list of files
  - Each showing: thumbnail, name, progress bar, status badge
  - Auto-scroll to active uploads
- **Completed Section:**
  - Collapsed list of completed uploads (expandable)
  - Quick summary: "77 photos completed successfully"

**UI/UX Specification:**
- **Progress Dashboard (replaces upload preview after "Start Upload" clicked):**
  - Header: Aggregate progress bar (8px height, gradient fill)
  - Subheader: "23/100 photos • 2 min remaining"
  - Active uploads: Cards with progress bars (green fill)
  - Failed uploads: Red status badge, "Retry" button
  - Completed: Green checkmark, fade to bottom of list
  - Minimize button: Collapses to small notification bar

**Dependencies:** FR-005 (Progress Tracking), FR-006 (Real-Time Updates)

---

#### FR-008: Upload Completion Notification (Priority: HIGH)
**Description:** Users receive clear notification when upload completes

**Acceptance Criteria:**
- **Success Notification:**
  - Modal appears: "100 photos uploaded successfully!"
  - Success animation (checkmark with subtle scale)
  - Preview: First 6 thumbnails from uploaded batch
  - Primary action: "View Gallery" button
  - Secondary actions: "Add Tags" | "Close"
- **Mobile Push Notification:**
  - Background uploads trigger push notification on completion
  - Text: "15 photos uploaded successfully"
  - Tap opens app to gallery view
- **Partial Success:**
  - If some failed: "92 of 100 photos uploaded. 8 failed."
  - Link to failed uploads for retry

**UI/UX Specification:**
- Success modal: Slide up from bottom, fade-in background overlay
- Confetti animation: Subtle celebration (2-second duration, tasteful, enabled by default)
- Success sound: Brief success tone (enabled by default, can be disabled in user settings)
- Settings panel includes toggles for animations and sounds (implemented in Epic 1)

**Dependencies:** FR-007 (Status Display)

---

#### FR-009: Upload Cancellation (Priority: MEDIUM)
**Description:** Users can cancel in-progress uploads

**Acceptance Criteria:**
- Cancel button available per file (individual) OR entire batch
- Cancellation stops upload immediately
- Partial uploads cleaned up from cloud storage
- Cancelled uploads removed from queue
- Confirmation modal for batch cancel: "Cancel all 23 remaining uploads?"

**UI/UX Specification:**
- Cancel button: Secondary button, gray, "Cancel Upload"
- Individual cancel: X icon on upload card
- Batch cancel: "Cancel All" button in header
- Confirmation modal: Warning icon, "Are you sure?" message

**Dependencies:** FR-005 (Progress Tracking)

---

### 7.3 Network Resilience (FR-015 to FR-018)

#### FR-015: Upload Error Handling (Priority: CRITICAL)
**Description:** Graceful error handling with user-friendly messages

**Acceptance Criteria:**
- Server errors (500, 502, 503) → "Upload failed. Please try again."
- Client errors (413 file too large) → "File exceeds 5MB limit. Please choose a smaller image."
- Network errors → "Connection lost. Upload will resume automatically."
- Each error has: Icon (alert triangle), user-friendly message, actionable next step

**Error Message Translations:**
```typescript
const errorMessages = {
  'NETWORK_ERROR': 'Connection lost. Upload will resume when connection returns.',
  'FILE_TOO_LARGE': 'File exceeds 5MB limit. Please compress or choose a smaller image.',
  'UNSUPPORTED_FORMAT': 'File format not supported. Please use JPEG, PNG, HEIC, or WebP.',
  'SERVER_ERROR': 'Upload failed due to a server issue. Please try again.',
  'QUOTA_EXCEEDED': 'Storage quota exceeded. Please upgrade or delete old photos.',
};
```

**UI/UX Specification:**
- Error state: Red status badge ("Failed")
- Error message: Red text with alert icon
- Retry button: Blue, prominent, with retry icon

**Dependencies:** None (foundational)

---

#### FR-016: Retry Failed Uploads (Priority: CRITICAL)
**Description:** Automatic retry with exponential backoff + manual retry option

**Acceptance Criteria:**
- **Automatic Retry:**
  - Failed uploads retry automatically up to 3 times
  - Exponential backoff: 1s, 2s, 4s between attempts
  - Retry count visible to user: "Retry 2 of 3..."
- **Manual Retry:**
  - "Retry" button on failed uploads
  - "Retry All Failed" button for batch retry

**Logic:**
```typescript
async function retryUploadWithBackoff(
  upload: Upload,
  attempt: number = 1
): Promise<void> {
  try {
    await uploadToS3(upload);
  } catch (error) {
    if (attempt < 3) {
      const delay = Math.pow(2, attempt) * 1000; // 1s, 2s, 4s
      await sleep(delay);
      return retryUploadWithBackoff(upload, attempt + 1);
    } else {
      markAsFailed(upload, error);
    }
  }
}
```

**Dependencies:** FR-015 (Error Handling)

---

#### FR-017: Network Loss Detection (Priority: CRITICAL)
**Description:** Detect network loss within 5 seconds and pause uploads

**Acceptance Criteria:**
- Network loss detected within 5 seconds
- All active uploads paused (status → "Paused")
- User notification: "Connection lost. Uploads paused."
- Notification persists until connection returns

**Detection Method:**
- Browser: `navigator.onLine` event + ping to server every 10s
- Mobile: Native network state monitoring (iOS: Reachability, Android: ConnectivityManager)

**UI/UX Specification:**
- Status badge changes: "Uploading" → "Paused" (yellow badge)
- Progress bars pause animation
- Notification banner: Yellow background, info icon, "Connection lost" message

**Dependencies:** None (foundational)

---

#### FR-018: Upload Resume from Interruption (Priority: CRITICAL)
**Description:** Automatic resume when network connection returns

**Acceptance Criteria:**
- Network reconnection detected within 5 seconds
- Paused uploads resume automatically (no user action)
- Resume from exact pause point (byte-level for large files, full restart for small files)
- User notification: "Connection restored. Resuming uploads..."
- **100% resume success rate**

**Technical Implementation:**
- Multipart uploads for files >5MB (S3 multipart upload supports resume)
- State persistence: Upload progress stored in database
- Resume mechanism: Re-upload from last completed part

**UI/UX Specification:**
- Status badge changes: "Paused" → "Uploading" (blue badge)
- Progress bars resume animation
- Success notification: Green banner, "Resumed successfully"

**Dependencies:** FR-017 (Network Detection)

---

### 7.4 Photo Gallery & Viewing (FR-011 to FR-014)

#### FR-011: Photo Gallery Display (Priority: CRITICAL)
**Description:** Responsive grid view of user's uploaded photos

**Acceptance Criteria:**
- **Layout:**
  - Grid layout: 2 columns (mobile), 3 columns (tablet), 4 columns (desktop), 5 columns (large desktop)
  - Photo cards: 1:1 aspect ratio (square), thumbnail displayed
  - Lazy loading: Load first 50 photos, load more on scroll
- **Sorting:**
  - Default: Most recent first (newest at top)
  - Options: Upload date (newest/oldest), File name (A-Z, Z-A)
- **Pagination:**
  - Infinite scroll (load next 50 on scroll to 80% of page)
  - Loading indicator at bottom (skeleton placeholders)

**UI/UX Specification:**
- **Photo Card:**
  - Border-radius: 12px
  - Hover state (web): Lift (translateY -4px), shadow increase
  - Overlay on hover: Heart icon, Share icon (semi-transparent black background)
  - Metadata: Filename (truncated) + upload date
- **Empty State:**
  - Icon: Image placeholder (48px, gray)
  - Text: "No photos yet. Upload your first batch!"
  - CTA: "Upload Photos" primary button

**Performance:**
- Gallery load time: <2 seconds for first 50 photos
- Smooth 60fps scrolling
- Thumbnail caching via CDN

**Dependencies:** FR-013 (Cloud Storage), FR-014 (Metadata Storage)

---

#### FR-012: Photo Viewing (Priority: HIGH)
**Description:** Full-size photo viewer (lightbox) with navigation

**Acceptance Criteria:**
- **Viewer:**
  - Click photo card → Opens full-screen lightbox
  - Full-resolution image displayed (lazy load on open)
  - Navigation: Arrow keys (web), swipe (mobile)
  - Action bar: Download, Share, Tag, Delete icons
  - Close: X button, Escape key (web), swipe down (mobile)
- **Zoom:**
  - Pinch-to-zoom (mobile)
  - Mouse wheel zoom (web)

**UI/UX Specification:**
- Lightbox: Full-screen overlay, black background (opacity 0.95)
- Image: Max-width 90vw, max-height 90vh, centered
- Navigation arrows: Left/right sides, large touch targets (64px)
- Metadata overlay (bottom): Filename, upload date, file size, tags
- Smooth transitions: Zoom from thumbnail position

**Dependencies:** FR-011 (Gallery Display)

---

#### FR-013: Cloud Storage Integration (Priority: CRITICAL)
**Description:** Photos stored in AWS S3 with pre-signed URLs

**Acceptance Criteria:**
- Direct-to-S3 upload using pre-signed URLs (no server proxy)
- Multipart upload for files >5MB
- Server-side encryption (AES-256)
- S3 key structure: `uploads/{userId}/{uploadId}/{filename}`
- Pre-signed URL expiration: 15 minutes

**Pre-Signed URL Generation:**
```java
public Mono<PresignedUrlResponse> generatePresignedUrl(
    UUID userId, UUID uploadId, String fileName
) {
    String s3Key = String.format("uploads/%s/%s/%s", userId, uploadId, fileName);

    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(s3Key)
        .contentType("image/jpeg")
        .metadata(Map.of("user-id", userId.toString()))
        .build();

    PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
        PutObjectPresignRequest.builder()
            .putObjectRequest(request)
            .signatureDuration(Duration.ofMinutes(15))
            .build()
    );

    return Mono.just(new PresignedUrlResponse(presignedRequest.url()));
}
```

**Dependencies:** AWS S3 bucket setup, IAM role configuration

---

#### FR-014: Photo Metadata Storage (Priority: CRITICAL)
**Description:** Photo metadata stored in PostgreSQL database

**Acceptance Criteria:**
- Metadata includes: photo ID, user ID, filename, file size, content type, upload timestamp, S3 key, storage URL
- Indexed by: user ID, upload date, tags
- Queryable for gallery display and search
- Soft delete support (metadata retained when photo "deleted")

**Database Schema:**
```sql
CREATE TABLE photos (
    photo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    upload_session_id UUID NOT NULL REFERENCES upload_sessions(session_id),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    s3_key VARCHAR(512) NOT NULL UNIQUE,
    s3_version_id VARCHAR(255),
    storage_url TEXT NOT NULL,
    upload_status VARCHAR(20) NOT NULL, -- 'COMPLETE', 'FAILED', 'DELETED'
    metadata JSONB, -- EXIF data, tags, etc.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,

    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_s3_key (s3_key)
);
```

**Dependencies:** FR-013 (Cloud Storage)

---

#### FR-019: Photo Tagging (Priority: HIGH) **[MOVED TO MVP per Project Brief]**
**Description:** Users can add, view, and filter photos by tags

**Acceptance Criteria:**
- Add tags to individual photos (comma-separated input)
- Remove tags from photos
- View tags associated with each photo (in gallery cards and lightbox)
- Filter gallery by tag (click tag to show all photos with that tag)
- Maximum 10 tags per photo
- Tags are case-insensitive, alphanumeric + spaces (max 30 chars per tag)

**UI/UX Specification:**
- **Gallery Card:**
  - Tags displayed as chips below thumbnail
  - Max 3 tags shown, "+N more" if exceeds
  - Tag chip style: Small pill (8px padding), light gray background, hover effect
- **Lightbox Viewer:**
  - Tags displayed in metadata overlay (bottom)
  - Click tag → Filter gallery by that tag
  - "+" button to add new tags (inline input appears)
  - "X" icon on each tag to remove
- **Tag Filter:**
  - Active tag filter shown at top of gallery
  - "Showing photos tagged with: [tag name]" badge
  - Clear filter button

**Data Model:**
```sql
CREATE TABLE photo_tags (
    photo_id UUID NOT NULL REFERENCES photos(photo_id) ON DELETE CASCADE,
    tag_name VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    PRIMARY KEY (photo_id, tag_name),
    INDEX idx_tag_search (tag_name, photo_id)
);
```

**API Endpoints:**
- `POST /photos/{photoId}/tags` - Add tag(s) to photo
  - Request body: `{"tags": ["vacation", "beach", "2024"]}`
- `DELETE /photos/{photoId}/tags/{tagName}` - Remove tag from photo
- `GET /photos?tag={tagName}` - Filter photos by tag

**Performance:**
- Tag addition: <200ms response time
- Tag filter query: <500ms for 10,000 photos

**Dependencies:** FR-014 (Metadata Storage)

**Testing:**
- Add tags to photo, verify display in gallery and lightbox
- Filter by tag, verify only tagged photos shown
- Remove tag, verify removal confirmed
- Test max 10 tags per photo validation

**MVP Scope - Fully Implemented:**
- ✅ Add tags to individual photos (comma-separated input)
- ✅ Remove tags from photos
- ✅ View tags in gallery cards and lightbox
- ✅ Filter gallery by tag (click tag to filter)
- ✅ Complete UI implementation (tag chips, input, filter badge)
- ✅ Complete backend implementation (CRUD APIs, database)
- ✅ Maximum 10 tags per photo validation
- ✅ Tag filter query performance (<500ms for 10K photos)

**Post-MVP Enhancements** (Epic 4 - New advanced features beyond core tagging):
- AI-powered auto-tagging (requires ML model integration)
- Batch tag application (multi-select photos, apply tags to all at once)
- Tag auto-suggestions (based on existing tags, image content)
- Advanced tag management (rename tags globally, merge similar tags, tag hierarchies)

---

### 7.5 Functional Requirements Summary Table

| ID | Requirement | Priority | Epic | MVP | Dependencies |
|----|-------------|----------|------|-----|--------------|
| FR-001 | User Authentication | CRITICAL | Epic 1 | ✅ | None |
| FR-002 | User Registration | CRITICAL | Epic 1 | ✅ | Email service |
| FR-003 | Session Management | CRITICAL | Epic 1 | ✅ | FR-001 |
| FR-004 | Batch Upload (100 photos) | CRITICAL | Epic 2 | ✅ | None |
| FR-005 | Upload Progress Tracking | CRITICAL | Epic 2 | ✅ | FR-004 |
| FR-006 | Real-Time Status Updates | CRITICAL | Epic 2 | ✅ | FR-005 |
| FR-007 | Upload Status Display | CRITICAL | Epic 2 | ✅ | FR-005, FR-006 |
| FR-008 | Upload Completion Notification | HIGH | Epic 2 | ✅ | FR-007 |
| FR-009 | Upload Cancellation | MEDIUM | Epic 2 | ⚠️ | FR-005 |
| FR-015 | Upload Error Handling | CRITICAL | Epic 2 | ✅ | None |
| FR-016 | Retry Failed Uploads | CRITICAL | Epic 2 | ✅ | FR-015 |
| FR-017 | Network Loss Detection | CRITICAL | Epic 2 | ✅ | None |
| FR-018 | Upload Resume | CRITICAL | Epic 2 | ✅ | FR-017 |
| FR-011 | Photo Gallery Display | CRITICAL | Epic 3 | ✅ | FR-013, FR-014 |
| FR-012 | Photo Viewing (Lightbox) | HIGH | Epic 3 | ✅ | FR-011 |
| FR-013 | Cloud Storage (S3) | CRITICAL | Epic 2 | ✅ | AWS setup |
| FR-014 | Photo Metadata Storage | CRITICAL | Epic 2 | ✅ | FR-013 |
| FR-019 | Photo Tagging | HIGH | Epic 3 | ✅ | FR-014 |
| FR-020 | User Settings Panel | MEDIUM | Epic 1 | ✅ | FR-001 |
| FR-021 | Onboarding Tutorial | MEDIUM | Epic 1 | ✅ | FR-002 |
| FR-022 | Empty State Design | MEDIUM | Epic 1 | ✅ | FR-011 |

**MVP Total:** 21 functional requirements
**Post-MVP:** 25 additional requirements (advanced search, AI tagging, sharing, cross-device sync, etc.)

---

## 8. Non-Functional Requirements

### 8.1 Performance Requirements (NFR-P)

#### NFR-P1: Concurrent Upload Performance (CRITICAL)
**Requirement:** Handle 100 concurrent photo uploads (2MB each) within 90 seconds on standard broadband (10 Mbps upload)

**Acceptance Criteria:**
- **Target:** 100 photos × 2MB = 200MB in 90 seconds
- **Achieved Performance:** <80 seconds (10-second safety margin)
- **Network Requirement:** Minimum 5 Mbps upload speed
- **Concurrency:** 10 simultaneous uploads at client level
- **Server Throughput:** 555 uploads/second per instance

**Measurement:**
- Load test with 100 concurrent file uploads
- P95 latency: <90 seconds
- P99 latency: <100 seconds
- Success rate: >95%

**Technical Strategy:**
- Spring WebFlux reactive (non-blocking I/O)
- Direct-to-S3 uploads (bypass server for file transfer)
- Client-side adaptive concurrency (detect bandwidth, adjust parallelism)

**Testing:** Gatling load test simulating 100 concurrent uploads

---

#### NFR-P2: UI Responsiveness During Uploads (CRITICAL)
**Requirement:** UI remains fully responsive (<100ms interaction latency) during peak upload operations

**Acceptance Criteria:**
- **Web:**
  - Frame rate: ≥30 FPS during uploads
  - UI interactions respond in <100ms
  - No UI freezing or blocking during file processing
  - Progress updates render smoothly (no jank)
- **Mobile:**
  - Frame rate: ≥60 FPS on modern devices, ≥30 FPS on older devices
  - Touch responses: <100ms
  - Background uploads don't degrade foreground performance

**Technical Strategy:**
- **Web:** Web Workers for file processing (off main thread)
- **Mobile:** React Native worker threads for parallel processing
- **Progress Updates:** Throttled to 500ms (avoid excessive re-renders)

**Measurement:**
- Chrome DevTools Performance profiling (web)
- React Native Performance Monitor (mobile)
- Lighthouse performance score: >90

**Testing:** Manual testing with 100-photo upload while navigating UI

---

#### NFR-P3: API Response Time (HIGH)
**Requirement:** API endpoints respond within acceptable latency limits

**Acceptance Criteria:**
- **GET endpoints:** <200ms (P95)
- **POST upload initiation:** <500ms
- **WebSocket messages:** <100ms delivery
- **Database queries:** <100ms for user collections <10,000 photos

**Measurement:**
- APM tools (New Relic, DataDog)
- Prometheus metrics collection
- Grafana dashboards

**Testing:** API load testing with k6 or JMeter

---

### 8.2 Scalability Requirements (NFR-S)

#### NFR-S1: Horizontal Scalability (HIGH)
**Requirement:** Backend scales horizontally to handle increased load

**Acceptance Criteria:**
- Stateless backend services (session state in database/cache)
- Load balancer distributes traffic across instances
- Auto-scaling: 2-10 instances based on CPU/memory
- Database connection pooling: 30 connections per instance
- No single points of failure

**Technical Strategy:**
- Spring Boot with externalized session state (Redis)
- AWS Auto Scaling Group (target CPU 60%)
- PostgreSQL read replicas for query scaling

**Testing:** Load test with multiple backend instances

---

### 8.3 Reliability Requirements (NFR-R)

#### NFR-R1: Upload Reliability (CRITICAL)
**Requirement:** Upload operations complete reliably with minimal failures

**Acceptance Criteria:**
- **Success Rate:** 99.9% under normal conditions
- **Automatic Retry:** Transient failures retry automatically
- **Resume Capability:** 100% resume success after network interruptions
- **Data Integrity:** Checksum validation for all uploads

**Measurement:**
- Upload success/failure rate metrics
- Retry success rate metrics
- Chaos engineering tests (network failures, server failures)

**Technical Strategy:**
- Automatic retry with exponential backoff
- Multipart uploads with checksum validation
- State persistence for resume capability

**Testing:** Integration tests with simulated network failures

---

#### NFR-R2: Data Durability (CRITICAL)
**Requirement:** Uploaded photos and metadata protected from data loss

**Acceptance Criteria:**
- **S3 Durability:** 99.999999999% (11 nines) from AWS S3
- **Database Backups:** Every 24 hours
- **Point-in-Time Recovery:** ≤1 hour RPO (Recovery Point Objective)
- **Metadata Consistency:** 100% consistency between metadata and S3 objects

**Measurement:**
- Backup success rate monitoring
- Disaster recovery drill (quarterly)

**Testing:** Backup restoration test

---

### 8.4 Security Requirements (NFR-SEC)

#### NFR-SEC1: Authentication Security (CRITICAL)
**Requirement:** Secure authentication prevents unauthorized access

**Acceptance Criteria:**
- JWT tokens use RS256 or HS256 with 256-bit key
- Access token expiration: 15 minutes
- Refresh token expiration: 7 days
- Secure token storage (HttpOnly cookies, secure storage APIs)
- Protection against token theft and replay attacks

**Technical Strategy:**
- Spring Security with JWT
- BCrypt password hashing (work factor 12)
- CSRF protection for state-changing operations

**Testing:** Security testing with token manipulation, penetration testing

---

#### NFR-SEC2: Data Encryption (CRITICAL)
**Requirement:** Data encrypted at rest and in transit

**Acceptance Criteria:**
- **In Transit:** HTTPS/TLS 1.2+ for all API communication
- **At Rest:** S3 server-side encryption (AES-256)
- **Sensitive Data:** Database encryption for sensitive fields (optional)
- **Pre-Signed URLs:** Time-limited (15 min), content-type restricted

**Testing:** SSL Labs testing, security audit

---

### 8.5 Usability Requirements (NFR-U)

#### NFR-U1: Web Interface Usability (HIGH)
**Requirement:** Web interface is intuitive and easy to use

**Acceptance Criteria:**
- Drag-and-drop upload clearly discoverable
- Upload progress visible and understandable
- Error messages actionable and user-friendly
- Responsive design (desktop, tablet)

**Measurement:**
- SUS (System Usability Scale) score: >70
- User testing sessions: 5 users complete first upload without assistance

**Testing:** Usability testing with target users

---

#### NFR-U2: Accessibility (WCAG 2.1 AA) (HIGH)
**Requirement:** Interfaces accessible to users with disabilities

**Acceptance Criteria:**
- WCAG 2.1 Level AA compliance
- Screen reader support for critical workflows
- Keyboard navigation for all web interactions
- Color contrast ratios: 4.5:1 minimum (body text), 3:1 (large text)

**Measurement:**
- axe DevTools automated scan: 0 violations
- Manual screen reader testing (NVDA, VoiceOver)

**Testing:** Accessibility audit with WAVE, axe DevTools

---

### 8.6 Non-Functional Requirements Summary

| ID | Requirement | Target | Priority | Testing Method |
|----|-------------|--------|----------|----------------|
| NFR-P1 | Concurrent Upload Speed | <80s for 100×2MB | CRITICAL | Load test (Gatling) |
| NFR-P2 | UI Responsiveness | <100ms interaction | CRITICAL | Performance profiling |
| NFR-P3 | API Latency | <200ms P95 (GET) | HIGH | Load test (k6) |
| NFR-S1 | Horizontal Scalability | 2-10 instances | HIGH | Multi-instance test |
| NFR-R1 | Upload Success Rate | >99% | CRITICAL | Chaos engineering |
| NFR-R2 | Data Durability | 11 nines (S3) | CRITICAL | Backup/restore test |
| NFR-SEC1 | Authentication Security | JWT RS256/HS256 | CRITICAL | Penetration testing |
| NFR-SEC2 | Data Encryption | TLS 1.2+, AES-256 | CRITICAL | SSL Labs, audit |
| NFR-U1 | Usability | SUS score >70 | HIGH | User testing |
| NFR-U2 | Accessibility | WCAG 2.1 AA | HIGH | axe DevTools audit |

---

## 9. Technical Architecture

### 9.1 Architecture Principles (Mandatory)

The backend architecture follows three **mandated architectural patterns**:

#### 1. Domain-Driven Design (DDD)
**Application:** Core domain concepts modeled as robust domain objects

**Domain Entities:**
- **UploadSession** (Aggregate Root): Manages collection of uploads
- **Upload** (Entity): Individual photo upload
- **User** (Entity): System user
- **Photo** (Entity): Uploaded photo metadata

**Domain Events:**
- `UploadSessionInitiatedEvent`
- `UploadCompletedEvent`
- `UploadSessionCompletedEvent`
- `UploadFailedEvent`

**Example Domain Model:**
```java
@Entity
public class UploadSession {
    @Id
    private UUID sessionId;
    private UserId userId;
    private SessionStatus status;
    private int totalFiles;
    private int completedFiles;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<Upload> uploads;

    // Domain logic
    public void completeUpload(UploadId uploadId) {
        Upload upload = findUpload(uploadId);
        upload.markCompleted();
        completedFiles++;

        if (completedFiles == totalFiles) {
            this.status = SessionStatus.COMPLETED;
            DomainEvents.raise(new UploadSessionCompletedEvent(this));
        }
    }
}
```

---

#### 2. CQRS (Command Query Responsibility Segregation)
**Application:** Separate command (write) and query (read) handlers

**Commands (Write Side):**
- `InitiateUploadCommand` → `InitiateUploadHandler`
- `CompleteUploadCommand` → `CompleteUploadHandler`
- `FailUploadCommand` → `FailUploadHandler`

**Queries (Read Side):**
- `GetSessionProgressQuery` → `SessionProgressQueryHandler`
- `GetUserPhotosQuery` → `UserPhotosQueryHandler`

**Benefits:**
- Optimize write path for throughput (upload completion)
- Optimize read path for latency (dashboard polling)
- Scale read replicas independently for query workloads

**Example Implementation:**
```java
// Command Side
@Service
@Transactional
public class UploadCommandService {
    public Mono<UploadId> initiateUpload(InitiateUploadCommand cmd) {
        UploadSession session = uploadRepository.findSession(cmd.getSessionId());
        Upload upload = session.addUpload(cmd.getFileName(), cmd.getFileSize());
        uploadRepository.save(upload);

        eventPublisher.publish(new UploadInitiatedEvent(upload.getId()));
        return Mono.just(upload.getId());
    }
}

// Query Side (Read Replica)
@Service
public class UploadQueryService {
    @Qualifier("readReplica")
    private final UploadReadRepository readRepository;

    public Mono<SessionProgressDTO> getSessionProgress(UUID sessionId) {
        return readRepository.findSessionProjection(sessionId);
    }
}
```

---

#### 3. Vertical Slice Architecture (VSA)
**Application:** Code organized by features (slices) rather than technical layers

**Feature Slices:**
- `features/initiateupload/` (InitiateUploadCommand, Handler, Controller)
- `features/completeupload/` (CompleteUploadCommand, Handler, Controller)
- `features/trackprogress/` (GetProgressQuery, Handler, Controller)

**Project Structure:**
```
src/main/java/com/rapidphoto/
├── features/
│   ├── initiateupload/
│   │   ├── InitiateUploadCommand.java
│   │   ├── InitiateUploadHandler.java
│   │   ├── InitiateUploadController.java
│   │   └── UploadInitiatedEvent.java
│   ├── completeupload/
│   │   ├── CompleteUploadCommand.java
│   │   ├── CompleteUploadHandler.java
│   │   └── CompleteUploadController.java
│   └── trackprogress/
│       ├── GetProgressQuery.java
│       ├── ProgressQueryHandler.java
│       └── ProgressController.java
├── shared/
│   ├── domain/ (UploadSession, Upload, User)
│   ├── infrastructure/ (S3Service, DatabaseConfig)
│   └── common/ (Result, ErrorCode, ValidationException)
```

**Benefits:**
- Changes localized to single slice
- Team can own entire slices
- Easy to add new features without touching existing code

---

### 9.2 Technology Stack

#### Backend
**Framework:** Spring Boot 3.x with Spring WebFlux (Reactive)

**Why Spring WebFlux:**
- **Non-blocking I/O:** Handles 100 concurrent uploads without thread exhaustion
- **Event-loop model:** 1000s of concurrent requests with ~200 threads
- **Native Async:** S3AsyncClient integration for reactive S3 operations
- **Performance:** 5-10× better concurrency than traditional Spring MVC

**Libraries:**
- **Spring Data R2DBC:** Reactive PostgreSQL access
- **Spring Security:** JWT authentication
- **AWS SDK 2.x:** S3AsyncClient for pre-signed URLs
- **Reactor:** Reactive streams (Mono, Flux)
- **Micrometer:** Metrics and observability

---

#### Frontend - Web
**Framework:** React 18 + TypeScript + Tailwind CSS

**Why This Stack:**
- **React:** Component-based architecture, large ecosystem
- **TypeScript:** Type safety reduces runtime errors
- **Tailwind CSS:** Utility-first CSS matches design system
- **Vite:** Fast build tool (<1s hot reload)

**Libraries:**
- **React Query:** Server state management, caching
- **Zustand:** Client state management (lightweight)
- **react-dropzone:** Drag-and-drop file uploads
- **SWR:** Real-time data fetching (WebSocket fallback)

---

#### Frontend - Unified (React Native for Web)
**Framework:** React Native (TypeScript) with react-native-web

**Why React Native for Web (Single Codebase):**
- **100% Code Reuse:** Single codebase for web, iOS, and Android
- **AI Development Efficiency:** Claude generates one component that works everywhere
- **Single Language:** TypeScript across entire stack (frontend + backend)
- **Team Efficiency:** One codebase, one component library, one state management system
- **Perfect for Upload App:** No SEO requirements (authenticated app)
- **Proven at Scale:** Used by Twitter/X, Uber Eats, Major League Soccer

**Key Packages:**
- **react-native-web:** Compiles React Native to web
- **axios:** HTTP client with interceptors
- **@reduxjs/toolkit:** State management (shared across all platforms)
- **react-native-keychain:** Secure token storage
- **react-native-image-picker:** Native photo picker integration
- **react-native-background-upload:** Background file upload support

**Project Structure:**
```
rapidphoto/
├── src/
│   ├── components/     ← Cross-platform (View, Pressable, Text)
│   ├── screens/        ← Works on web + iOS + Android
│   ├── hooks/          ← Shared business logic
│   └── services/       ← API clients, S3 upload
├── web/               ← Web build output
└── mobile/            ← iOS/Android builds
```

---

#### Cloud Infrastructure
**Platform:** AWS

**Services:**
- **S3:** Object storage (photos)
- **EC2 Auto Scaling:** Backend instances (Spring Boot)
- **RDS PostgreSQL:** Metadata database
- **ElastiCache Redis:** Session state, rate limiting
- **CloudFront:** CDN for thumbnail delivery
- **ALB:** Application Load Balancer
- **Lambda:** Post-processing (thumbnail generation)

**Why AWS over Azure:**
- **S3 SDK Maturity:** Excellent reactive Java SDK (S3AsyncClient)
- **Pre-signed URLs:** More straightforward than Azure SAS tokens
- **Ecosystem:** Better integration with Lambda, CloudFront

---

### 9.3 System Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                  Users (Web + Mobile)                   │
└────────────┬────────────────────────────────┬───────────┘
             │                                │
             v                                v
    ┌─────────────────────────────────────────────────┐
    │    React Native Application (TypeScript)       │
    │    Compiles to: Web + iOS + Android            │
    └────────────────────┬────────────────────────────┘
             │                                │
             │         HTTPS/WebSocket        │
             v                                v
        ┌────────────────────────────────────────┐
        │   Application Load Balancer (ALB)     │
        └──────────────┬─────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        v                             v
┌───────────────┐           ┌───────────────┐
│ Spring Boot   │           │ Spring Boot   │
│ (WebFlux)     │           │ (WebFlux)     │
│ Instance 1    │           │ Instance 2    │
└───────┬───────┘           └───────┬───────┘
        │                           │
        │    ┌──────────────────────┘
        │    │
        v    v
┌────────────────────┐      ┌─────────────────┐
│  PostgreSQL (RDS)  │      │  Redis          │
│  - Primary         │      │  (Session/Cache)│
│  - Read Replica    │      └─────────────────┘
└────────────────────┘
        │
        v
┌────────────────────────────────────────────┐
│               AWS S3                       │
│  - Photos stored with pre-signed URLs      │
│  - Server-side encryption (AES-256)        │
│  - CloudFront CDN for delivery             │
└──────────────────┬─────────────────────────┘
                   │
                   v (Event Notification)
            ┌──────────────┐
            │  SQS Queue   │
            └──────┬───────┘
                   │
                   v
            ┌──────────────┐
            │   Lambda     │
            │  (Thumbnail  │
            │  Generation) │
            └──────────────┘
```

---

### 9.4 Upload Flow Architecture (Detailed)

```
┌─────────────┐
│   Client    │
│ (Web/Mobile)│
└──────┬──────┘
       │
       │ 1. POST /api/uploads/initiate
       │    { sessionId, fileName, fileSize, contentType }
       v
┌──────────────────┐
│  Spring Boot     │
│  (Upload Slice)  │
└──────┬───────────┘
       │
       │ 2. Validate request, generate pre-signed URL
       │    - Check user quota
       │    - Validate file type/size
       │    - Generate S3 pre-signed PUT URL (15 min expiration)
       v
┌──────────────────┐
│   PostgreSQL     │
│  (Save Upload    │
│   Metadata)      │
└──────┬───────────┘
       │
       │ 3. Return pre-signed URL + uploadId to client
       v
┌─────────────┐
│   Client    │
│             │ 4. PUT directly to S3 using pre-signed URL
└──────┬──────┘    (bypasses backend for file transfer)
       │
       v
┌──────────────────┐
│     AWS S3       │
│  (Store Photo)   │
└──────┬───────────┘
       │
       │ 5. Upload complete, client notifies backend
       v
┌──────────────────┐
│  Spring Boot     │
│  (Complete       │
│   Upload Slice)  │ 6. Update upload status to "COMPLETE"
└──────┬───────────┘    - Update database
       │                - Publish UploadCompletedEvent
       v
┌──────────────────┐
│   WebSocket      │
│  (Real-Time      │
│   Progress       │ 7. Push progress update to client
│   Updates)       │
└──────────────────┘
       │
       v
┌─────────────┐
│   Client    │
│  (Progress   │
│   Dashboard) │ 8. Display updated progress
└─────────────┘
```

---

### 9.5 Database Schema

**Core Tables:**

#### `users` Table
```sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

#### `upload_sessions` Table
```sql
CREATE TABLE upload_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    total_files INTEGER NOT NULL,
    completed_files INTEGER DEFAULT 0,
    failed_files INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL, -- 'IN_PROGRESS', 'COMPLETED', 'FAILED'

    INDEX idx_user_created (user_id, created_at DESC)
);
```

#### `uploads` Table
```sql
CREATE TABLE uploads (
    upload_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES upload_sessions(session_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    s3_key VARCHAR(512) NOT NULL UNIQUE,
    s3_version_id VARCHAR(255),
    presigned_url_generated_at TIMESTAMP WITH TIME ZONE,
    upload_started_at TIMESTAMP WITH TIME ZONE,
    upload_completed_at TIMESTAMP WITH TIME ZONE,
    upload_status VARCHAR(20) NOT NULL, -- 'PENDING', 'UPLOADING', 'COMPLETED', 'FAILED'
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    metadata JSONB, -- Flexible storage for EXIF, tags, etc.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    INDEX idx_session_status (session_id, upload_status),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_s3_key (s3_key)
);
```

#### `photos` Table (Gallery Display)
```sql
CREATE TABLE photos (
    photo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id UUID NOT NULL REFERENCES uploads(upload_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    storage_url TEXT NOT NULL, -- CDN URL for access
    thumbnail_url TEXT, -- Generated thumbnail
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE, -- Soft delete

    INDEX idx_user_created (user_id, created_at DESC)
);
```

#### `photo_tags` Table (Tagging - MVP)
```sql
CREATE TABLE photo_tags (
    photo_id UUID NOT NULL REFERENCES photos(photo_id) ON DELETE CASCADE,
    tag_name VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    PRIMARY KEY (photo_id, tag_name),
    INDEX idx_tag_search (tag_name, photo_id)
);
```

---

### 9.6 API Specification (OpenAPI Summary)

**Base URL:** `https://api.rapidphotoupload.com/v1`

#### Authentication Endpoints
- `POST /auth/register` - User registration
- `POST /auth/login` - User login (returns JWT)
- `POST /auth/refresh` - Refresh access token
- `POST /auth/logout` - Logout (invalidate tokens)

#### Upload Endpoints
- `POST /uploads/sessions` - Create upload session
- `POST /uploads/initiate` - Initiate single upload (get pre-signed URL)
- `POST /uploads/{uploadId}/complete` - Mark upload as complete
- `POST /uploads/{uploadId}/fail` - Mark upload as failed
- `GET /uploads/sessions/{sessionId}` - Get session status
- `DELETE /uploads/{uploadId}` - Cancel upload

#### Photo Endpoints
- `GET /photos` - List user's photos (paginated, filterable by tag)
  - Query params: `?page=1&limit=50&tag=vacation`
- `GET /photos/{photoId}` - Get photo metadata
- `DELETE /photos/{photoId}` - Delete photo (soft delete)

#### Tagging Endpoints (MVP)
- `POST /photos/{photoId}/tags` - Add tags to photo
  - Request body: `{"tags": ["vacation", "beach", "2024"]}`
  - Response: `201 Created` with updated tag list
- `DELETE /photos/{photoId}/tags/{tagName}` - Remove tag from photo
  - Response: `204 No Content`
- `GET /photos/{photoId}/tags` - Get all tags for a photo
  - Response: `{"tags": ["vacation", "beach"]}`

#### WebSocket
- `ws://api.rapidphotoupload.com/v1/ws/upload-progress/{sessionId}` - Real-time progress updates

---

## 10. UI/UX Design System

**Comprehensive Design System:** See `/docs/ui-ux-design-system.md` for complete specification.

### 10.1 Design Philosophy

**Core Principles:**
1. **Speed & Efficiency:** Minimize clicks, instant feedback, keyboard shortcuts
2. **Clarity & Simplicity:** Clear hierarchy, uncluttered, self-explanatory
3. **Confidence & Trust:** Always visible progress, immediate confirmations
4. **Delight & Polish:** Smooth animations (60fps), satisfying micro-interactions

**Competitive Benchmarks:**
- **Google Photos:** Clean layouts, smooth scroll, instant search
- **Dropbox:** Clear progress, reliable sync indicators
- **Cloudinary:** Professional dashboard, detailed analytics

---

### 10.2 Visual Design System (Summary)

**Color Palette:**
- **Primary:** Blue (#2563EB) for CTAs, links
- **Success:** Green (#10B981) for completed uploads
- **Error:** Red (#EF4444) for failed uploads
- **Warning:** Amber (#F59E0B) for paused uploads

**Typography:**
- **Font:** Inter (sans-serif, optimized for screens)
- **Sizes:** 12px (meta) → 16px (body) → 24px (headings)
- **Weights:** 400 (body), 600 (buttons), 700 (headings)

**Spacing:**
- **8px base unit:** All spacing multiples of 8px
- **Common:** 16px (default gap), 24px (sections), 48px (page margins)

**Components:**
- **Buttons:** Primary (blue), Secondary (outline), Text (link-style)
- **Progress Bars:** Linear (8px height, gradient fill)
- **Cards:** Photo cards (1:1 aspect ratio), Upload cards (progress display)
- **Modals:** Lightbox (photo viewer), Success modal (upload complete)

---

### 10.3 Key UI Screens

#### 1. Upload Dashboard
**Layout:**
- Header: Aggregate progress bar (sticky)
- Body: Scrollable list of upload cards
- Each card: Thumbnail, filename, progress bar, status badge
- Footer: "Cancel All" button

**States:**
- Uploading: Blue progress bars, spinner icons
- Complete: Green checkmarks, fade to bottom
- Failed: Red badges, "Retry" buttons

---

#### 2. Photo Gallery
**Layout:**
- Grid: 2-5 columns (responsive)
- Each photo: Square thumbnail, hover overlay (icons)
- Infinite scroll: Load more on scroll to 80%

**Interactions:**
- Click photo → Opens lightbox
- Hover (web) → Shows action icons (heart, share)
- Long-press (mobile) → Multi-select mode

---

## 11. Epic Breakdown & Prioritization

### 11.1 MVP Epics (Weeks 1-12)

#### Epic 1: User Authentication & Onboarding (Priority: P0)
**Goal:** Users can securely access the system

**Scope:**
- FR-001: User Authentication (JWT) - Complete login UI, error handling, loading states
- FR-002: User Registration (email + password) - Complete registration UI, password strength, validation
- FR-003: Session Management - Token refresh, session persistence (backend)
- FR-020: User Settings Panel - Animation/sound toggles, preferences (UI + backend)
- FR-021: Onboarding Tutorial - 3-screen tutorial with skip option (UI)
- FR-022: Empty State Design - First-time user experience, "Upload Photos" CTA (UI)

**Duration:** 2-3 weeks
**Effort:** Medium
**Risk:** Low (standard patterns)

**Success Metrics:**
- Registration completion rate: >75%
- Tutorial completion rate: >40% (complete all screens)
- Tutorial skip rate: <60% (indicates tutorial value)
- Session persistence across restarts: 100%
- Settings usage: >30% of users access settings within first week
- Empty state CTA click-through: >80% (users click "Upload Photos")

---

#### Epic 2: Core Upload Experience (Priority: P0)
**Goal:** Users can reliably upload photos with progress visibility

**Scope:**
- **Phase A: Basic Upload (Weeks 3-4)**
  - FR-004: Batch Upload (up to 100)
  - FR-005: Progress Tracking
  - FR-007: Status Display
- **Phase B: Concurrent & Real-Time (Weeks 5-6)**
  - FR-006: WebSocket real-time updates
  - Concurrent upload management (10 simultaneous)
  - Upload queue
- **Phase C: Resilience (Weeks 7-8)**
  - FR-015: Error Handling
  - FR-016: Retry Logic
  - FR-017: Network Detection
  - FR-018: Auto-Resume

**Duration:** 6 weeks
**Effort:** Large
**Risk:** HIGH (performance, concurrency, resilience)

**Success Metrics:**
- Upload initiation success: >95%
- Upload completion rate: >90%
- Network resume success: 100%
- 100 concurrent uploads: <80 seconds

---

#### Epic 3: Photo Gallery, Viewing & Tagging (Priority: P0)
**Goal:** Users can view, browse, and tag uploaded photos

**Scope:**
- FR-011: Gallery Display (grid, paginated)
- FR-012: Photo Viewer (lightbox)
- FR-013: Cloud Storage Integration (S3)
- FR-014: Metadata Storage (PostgreSQL)
- **FR-019: Photo Tagging** (basic manual tagging - added per project brief requirement)

**Duration:** 3 weeks (extended to include tagging)
**Effort:** Medium-Large
**Risk:** Medium (performance at scale, tagging query optimization)

**Success Metrics:**
- Gallery load time: <2 seconds
- Photo viewer open: <500ms
- Smooth 60fps scrolling
- Tag addition: <200ms response time
- Tag filter query: <500ms for 10,000 photos

---

### 11.2 Post-MVP Epics (Weeks 13-27)

#### Epic 4: Advanced Organization & Search (P0 Post-MVP, Weeks 13-18)
**Goal:** Users can organize and find photos with advanced features

**Scope:**
- FR-020: Batch Tag Application (tag multiple photos at once)
- FR-021: AI-Powered Auto-Tagging
- FR-022: Advanced Search (multi-tag filter, date range, file type)
- FR-023: Smart Collections / Albums
- Timeline view

**Rationale:** Gallery needs advanced search above 500 photos; AI tagging reduces manual work

**Note:** Basic tagging (FR-019) moved to MVP Epic 3 per project brief requirement

---

#### Epic 7: Cross-Device Continuity (P0 Post-MVP, Weeks 13-18)
**Goal:** Seamless experience across devices

**Scope:**
- FR-020: Upload Queue Sync
- FR-021: Cloud State Persistence
- FR-022: Duplicate Detection

**Rationale:** 60% frustration for hybrid users (Journey 4)

---

### 11.3 Epic Sequencing

```
MVP (Weeks 1-12):
├── Epic 1: Auth (Weeks 1-3)        [PARALLEL START]
├── Epic 2: Upload (Weeks 1-8)      [PARALLEL START]
│   ├── Phase A (Weeks 1-2)
│   ├── Phase B (Weeks 3-4)
│   └── Phase C (Weeks 5-6)
└── Epic 3: Gallery & Tagging (Weeks 7-10)  [DEPENDS: Epic 2 Phase A]
    ├── Gallery + Viewer (Weeks 7-8)
    └── Photo Tagging (Weeks 9-10)

Post-MVP Phase 1 (Weeks 13-18):
├── Epic 4: Organization            [HIGH PRIORITY]
└── Epic 7: Cross-Device            [HIGH PRIORITY, PARALLEL]
```

---

## 12. Success Metrics & KPIs

### 12.1 Technical KPIs

| Metric | Target | Measurement | Critical? |
|--------|--------|-------------|-----------|
| Upload Speed (100×2MB) | <80s | Load test | ✅ CRITICAL |
| Upload Success Rate | >90% | Analytics | ✅ CRITICAL |
| Network Resume Success | 100% | Integration test | ✅ CRITICAL |
| UI Response Time | <100ms | Performance profiling | ✅ CRITICAL |
| API Latency (P95) | <200ms | APM monitoring | ⚠️ HIGH |
| Gallery Load Time | <2s | Lighthouse | ⚠️ HIGH |

---

### 12.2 User Experience KPIs

| Metric | Target | Measurement | Critical? |
|--------|--------|-------------|-----------|
| First Upload Success | >90% | Analytics funnel | ✅ CRITICAL |
| User Satisfaction (Post-Upload) | >4.5/5 | In-app survey | ⚠️ HIGH |
| 7-Day Return Rate | >40% | Analytics retention | ⚠️ HIGH |
| Net Promoter Score (NPS) | >50 | Quarterly survey | ⚠️ HIGH |
| Upload Abandonment Rate | <10% | Analytics | ⚠️ HIGH |

---

### 12.3 Business KPIs (Post-MVP)

| Metric | Target (Year 1) | Measurement |
|--------|----------------|-------------|
| Beta Users | 50 | Sign-up tracking |
| Weekly Active Users | 30 (60%) | Analytics |
| Avg Photos/User/Week | 200 | Analytics |
| Churn Rate | <5%/month | Retention analysis |

---

## 13. Development Roadmap

### 13.1 MVP Timeline (12 Weeks)

**Week 1-3: Foundation**
- ✅ Set up infrastructure (AWS, PostgreSQL, Redis)
- ✅ Implement authentication (Epic 1)
- ✅ Set up CI/CD pipeline
- ✅ Design system setup (Tailwind, components)

**Week 4-6: Core Upload**
- ✅ Basic upload UI (Epic 2 Phase A)
- ✅ Progress tracking system
- ✅ Concurrent upload handling
- ✅ WebSocket real-time updates (Epic 2 Phase B)

**Week 7-10: Resilience, Gallery & Tagging**
- ✅ Network resilience (Epic 2 Phase C)
- ✅ Error handling and retry
- ✅ Gallery display (Epic 3)
- ✅ Photo viewer
- ✅ Photo tagging (FR-019) - Added per project brief requirement

**Week 11-12: Testing & Polish**
- ✅ Integration testing
- ✅ Load testing (100 concurrent uploads)
- ✅ UI/UX polish
- ✅ Mobile app optimization
- ✅ Beta launch preparation

---

### 13.2 Post-MVP Roadmap

**Weeks 13-18: Phase 1 Enhancements**
- Epic 4: Search & Organization
- Epic 7: Cross-Device Sync
- Epic 5: Mobile Optimization

**Weeks 19-27: Phase 2 Features**
- Epic 6: Sharing & Export
- Epic 8: Advanced Admin Features
- AI-powered tagging (optional)

---

## 14. Risks & Mitigation

### 14.1 Critical Risks

#### Risk 1: Upload Performance Below Target (HIGH)
**Risk:** 100 concurrent uploads exceed 90-second target

**Impact:** Fails core value proposition, user abandonment

**Probability:** MEDIUM (tight performance target)

**Mitigation:**
- Early performance testing (Week 4)
- Spring WebFlux reactive architecture (non-blocking I/O)
- Direct-to-S3 uploads (bypass server bottleneck)
- Adaptive client-side concurrency (detect bandwidth, adjust parallelism)
- 10-second safety margin built into 80-second target

**Contingency:** If 80s not achievable, negotiate 90s as acceptable

---

#### Risk 2: Network Resume Failure Rate >1% (CRITICAL)
**Risk:** Network interruptions cause upload failures despite auto-resume

**Impact:** 40% user abandonment (based on user research)

**Probability:** MEDIUM (complex state management)

**Mitigation:**
- Multipart uploads with checksum validation
- State persistence in database (upload progress survives app crash)
- Comprehensive integration testing with network failure simulation
- Chaos engineering tests (random network drops)

**Contingency:** Manual resume button as fallback if automatic fails

---

#### Risk 3: Mobile Development Complexity (LOW)
**Risk:** React Native file operations and background tasks prove challenging

**Impact:** Mobile app features delayed or require native modules

**Probability:** LOW (React Native has mature ecosystem for file operations)

**Mitigation:**
- Leverage team's existing React/TypeScript knowledge (no new language)
- 70-80% code reuse from web app reduces development time
- Use proven libraries: react-native-background-upload, react-native-image-picker
- Prototype mobile upload flow early (week 7) to validate approach

**Contingency:** Build native modules for critical file operations if needed

---

### 14.2 Medium Risks

#### Risk 4: Spring WebFlux Complexity (MEDIUM)
**Risk:** Reactive programming learning curve introduces bugs

**Impact:** Development velocity decreases, bugs in production

**Mitigation:**
- 2-week team training on reactive programming
- Pair programming for complex reactive code
- Code reviews focused on reactive patterns
- Comprehensive testing with StepVerifier

---

#### Risk 5: AWS Costs Exceed Budget (LOW)
**Risk:** S3 storage and data transfer costs higher than estimated

**Impact:** Operating costs unsustainable

**Mitigation:**
- Pre-calculate costs based on expected usage (100 users × 1000 photos)
- Set up CloudWatch billing alerts
- Use S3 lifecycle policies (move old photos to Glacier)
- Monitor costs weekly during beta

---

## 15. Appendices

### Appendix A: Research Documents

**Comprehensive research completed by specialized agents:**
1. Market Research Report (TAM/SAM/SOM, competitive analysis)
2. Requirements Analysis (43 functional requirements, 10 NFRs)
3. User Research Report (5 detailed personas)
4. User Journey Maps (7 complete journeys)
5. Technical Evaluation (architecture assessment)

**Location:** `/Users/reena/gauntletai/picstormai/docs/`

---

### Appendix B: Glossary

- **BMAD:** Business, Market, Architecture, Development (PRD methodology)
- **DDD:** Domain-Driven Design
- **CQRS:** Command Query Responsibility Segregation
- **VSA:** Vertical Slice Architecture
- **TAM:** Total Addressable Market
- **SAM:** Serviceable Addressable Market
- **SOM:** Serviceable Obtainable Market
- **NPS:** Net Promoter Score
- **SUS:** System Usability Scale
- **WCAG:** Web Content Accessibility Guidelines

---

### Appendix C: References

1. Project Brief: `/Users/reena/Downloads/GOLD_ Teamfront - RapidPhotoUpload.pdf`
2. Competitor Strategies: `/Users/reena/Downloads/Competitor Implementation Strategies for Key Features.md`
3. UI/UX Design System: `/docs/ui-ux-design-system.md`

---

**END OF PRODUCT REQUIREMENTS DOCUMENT**

---

## Approval Signatures

| Role | Name | Date | Signature |
|------|------|------|-----------|
| **Product Owner** | _________ | ______ | _________ |
| **Technical Lead** | _________ | ______ | _________ |
| **Design Lead** | _________ | ______ | _________ |
| **Project Manager** | _________ | ______ | _________ |
| **Stakeholder** | _________ | ______ | _________ |

**Document Status:** DRAFT FOR REVIEW
**Next Review Date:** [Date]
**Version:** 1.0
