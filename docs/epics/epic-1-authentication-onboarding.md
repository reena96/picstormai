# Epic 1: Authentication & Onboarding

**Goal**: Users can securely access the system and understand key features

**Duration**: 2-3 weeks
**Dependencies**: Epic 0 completed
**Scope**: JWT authentication, registration, login, user settings, onboarding tutorial

---

## Story 1.1: JWT Authentication Backend

**As a** backend developer
**I want to** implement JWT-based authentication with access and refresh tokens
**So that** users can securely authenticate and maintain sessions

### Acceptance Criteria

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

## Story 1.2: User Registration & Email Verification

**As a** new user
**I want to** register and verify my email address
**So that** I can create a secure account

### Acceptance Criteria

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

## Story 1.3: Login/Logout UI (Web + Mobile)

**As a** user
**I want to** log in and log out from a beautiful UI
**So that** I can access my account on any device

### Acceptance Criteria

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

## Story 1.4: User Settings Panel

**As a** user
**I want to** customize my app preferences
**So that** the app behaves according to my preferences

### Acceptance Criteria

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

## Story 1.5: Onboarding Tutorial (First-Time Users)

**As a** first-time user
**I want to** see an interactive tutorial
**So that** I understand how to use the app

### Acceptance Criteria

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

## Story 1.6: Authentication Integration Tests

**As a** QA engineer
**I want to** validate complete authentication flows
**So that** all auth scenarios are tested end-to-end

### Acceptance Criteria

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
