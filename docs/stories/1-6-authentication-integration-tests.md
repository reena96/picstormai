# Story 1.6: Authentication Integration Tests

Status: ready-for-dev

## Story

As a QA engineer,
I want to validate complete authentication flows,
so that all auth scenarios are tested end-to-end.

## Acceptance Criteria

**Given** authentication system is complete
**When** I run integration tests
**Then** all auth flows are validated

### Test Coverage Requirements:

1. **Registration → Email verification → Login** - Full user journey
2. **JWT token generation and validation** - Token structure and claims
3. **Refresh token rotation** - Token rotation on refresh
4. **Login with unverified email (rejected)** - Email verification enforcement
5. **Duplicate email registration (rejected)** - Email uniqueness
6. **Weak password validation** - Password strength requirements
7. **Logout revokes tokens** - Token cleanup (future: backend revocation)
8. **Expired token returns 401** - Token expiry handling
9. **Auto-refresh on 401 response** - Automatic token refresh

## Tasks / Subtasks

- [ ] Task 1: Setup Integration Test Infrastructure
  - [ ] Configure Spring Boot Test with Testcontainers
  - [ ] Setup PostgreSQL test container
  - [ ] Configure test application properties
  - [ ] Create base integration test class

- [ ] Task 2: Authentication Flow Integration Tests
  - [ ] Test: Complete registration flow
  - [ ] Test: Email verification flow
  - [ ] Test: Login after verification
  - [ ] Test: Login blocked for unverified users
  - [ ] Test: Duplicate email rejection

- [ ] Task 3: JWT Token Tests
  - [ ] Test: JWT token structure and claims
  - [ ] Test: Token validation
  - [ ] Test: Token expiry (401 response)
  - [ ] Test: Refresh token generation
  - [ ] Test: Refresh token rotation

- [ ] Task 4: Password Validation Tests
  - [ ] Test: Weak password rejection
  - [ ] Test: Strong password acceptance
  - [ ] Test: Password validation error messages

- [ ] Task 5: Token Lifecycle Tests
  - [ ] Test: Logout clears tokens (client-side)
  - [ ] Test: Expired access token refresh flow
  - [ ] Test: Invalid refresh token rejection

- [ ] Task 6: Security Tests (Optional - Basic)
  - [ ] Test: SQL injection protection in login
  - [ ] Test: XSS protection in registration
  - [ ] Test: Unauthorized access returns 401

- [ ] Task 7: Run All Tests
  - [ ] Execute integration test suite
  - [ ] Verify all tests pass
  - [ ] Check test coverage

- [ ] Task 8: Document Test Results
  - [ ] Update story file with test results
  - [ ] Create validation guide
  - [ ] Document any known issues

## Dev Notes

### Architecture Patterns and Constraints

**Integration Test Strategy:**
- Use Spring Boot Test (@SpringBootTest)
- Use Testcontainers for PostgreSQL
- Use WebTestClient for reactive endpoint testing
- Mock email service (use MockEmailService)
- Test real database interactions
- Test full request/response cycles

**Test Structure:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthenticationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private WebTestClient webTestClient;

    // Tests...
}
```

**Test Scenarios:**

1. **Full Registration Flow:**
```
POST /api/auth/register
→ Check response 201 Created
→ Extract verification token from mock email
→ GET /api/auth/verify-email?token={token}
→ Check response 200 OK
→ POST /api/auth/login
→ Check response 200 with tokens
```

2. **JWT Validation:**
```
Decode JWT accessToken
→ Verify claims (sub, email, iat, exp)
→ Verify signature
→ Verify expiration time (15 minutes)
```

3. **Refresh Token Rotation:**
```
POST /api/auth/login → tokens1
POST /api/auth/refresh with tokens1.refreshToken → tokens2
→ Verify tokens2.accessToken ≠ tokens1.accessToken
→ Verify tokens2.refreshToken ≠ tokens1.refreshToken (rotation)
→ Try using old refresh token → 401 Unauthorized
```

4. **Unverified User Login:**
```
POST /api/auth/register
→ Skip verification
→ POST /api/auth/login
→ Check response 401 with message "Please verify your email"
```

5. **Token Expiry:**
```
POST /api/auth/login → get tokens
→ Wait for expiration or manipulate time
→ GET /api/user/profile with expired token
→ Check response 401 Unauthorized
```

[Source: docs/epics/epic-1-authentication-onboarding.md#Story-1.6]

### Source Tree Components to Touch

```
backend/
├── build.gradle (add testcontainers dependency)
├── src/test/java/com/rapidphoto/
│   └── integration/
│       ├── AuthenticationIntegrationTest.java (NEW)
│       ├── JwtTokenIntegrationTest.java (NEW)
│       └── UserRegistrationIntegrationTest.java (NEW)
├── src/test/resources/
│   └── application-test.yml (NEW or update)
```

### Testing Standards Summary

**Testcontainers Setup:**
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", () ->
        "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/testdb");
    registry.add("spring.r2dbc.username", postgres::getUsername);
    registry.add("spring.r2dbc.password", postgres::getPassword);
}
```

**WebTestClient Usage:**
```java
webTestClient.post()
    .uri("/api/auth/login")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(loginRequest)
    .exchange()
    .expectStatus().isOk()
    .expectBody(LoginResponse.class)
    .returnResult()
    .getResponseBody();
```

### Project Structure Notes

**Dependencies on Previous Stories:**
- Story 1.1: JWT endpoints to test
- Story 1.2: Registration and email verification to test
- Story 1.3: Login UI (not tested here - backend only)
- Story 1.4: Settings endpoints (not tested in this story)
- Story 1.5: Onboarding endpoints (not tested in this story)

**New Dependencies:**
```gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'io.projectreactor:reactor-test'
testImplementation 'org.testcontainers:testcontainers:1.19.3'
testImplementation 'org.testcontainers:postgresql:1.19.3'
testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
```

### Learnings from Previous Stories

**From Story 1.1 (JWT Authentication Backend):**
- Login endpoint: POST /api/auth/login
- Refresh endpoint: POST /api/auth/refresh
- Access token expires in 15 minutes
- Refresh token expires in 30 days

**From Story 1.2 (User Registration & Email Verification):**
- Register endpoint: POST /api/auth/register
- Verify endpoint: GET /api/auth/verify-email?token={token}
- Email verification is required before login
- Verification tokens expire in 24 hours

**Key Integration Points:**
- Mock email service already exists (MockEmailService)
- Database schema with all tables (users, user_preferences, refresh_tokens, email_verification_tokens)
- Full CQRS implementation with commands and handlers

### References

- Epic Overview: [docs/epics/epic-1-authentication-onboarding.md#Story-1.6]
- Spring Boot Testing: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing
- Testcontainers: https://www.testcontainers.org/
- WebTestClient: https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#webtestclient

## Dev Agent Record

### Context Reference

- docs/stories/1-6-authentication-integration-tests.context.xml

### Agent Model Used

claude-sonnet-4-5-20250929

### Debug Log References

### Completion Notes List

### File List
