# Story 1.2: User Registration & Email Verification

Status: todo

## Story

As a new user,
I want to register and verify my email address,
so that I can create a secure account.

## Acceptance Criteria

1. **Registration Endpoint**: POST /api/auth/register accepts email, password, displayName and creates new user account
2. **Password Validation**: Password must be min 8 chars with at least one uppercase letter and one number
3. **Duplicate Email Check**: Registration fails with "Email already registered" for existing emails
4. **Email Verification Token**: Generate secure random 32-char token that expires in 24 hours
5. **Verification Email**: Send HTML email with verification link to user's email address
6. **Email Verification Endpoint**: GET /api/auth/verify-email?token={token} verifies email if token valid
7. **Login Restriction**: Unverified users cannot login (return error "Please verify your email")
8. **Token Expiration**: Expired tokens return error "Verification token expired"

## Tasks / Subtasks

- [ ] Task 1: Password Validation (AC: #2)
  - [ ] Create PasswordValidator utility class
  - [ ] Validate min 8 characters
  - [ ] Validate at least one uppercase letter
  - [ ] Validate at least one number
  - [ ] Return descriptive error messages

- [ ] Task 2: Email Verification Token Domain (AC: #4, #8)
  - [ ] Create EmailVerificationToken entity (userId, token, expiresAt, createdAt)
  - [ ] Create EmailVerificationTokenRepository with R2DBC
  - [ ] Implement findByToken, deleteByToken, deleteExpiredTokens methods
  - [ ] Generate secure random 32-char token
  - [ ] Set expiration to 24 hours from creation

- [ ] Task 3: RegisterUserCommand and Handler (AC: #1, #2, #3)
  - [ ] Update RegisterUserCommand to include password validation
  - [ ] Update RegisterUserCommandHandler to check duplicate emails
  - [ ] Validate password strength with PasswordValidator
  - [ ] Create User entity with emailVerified=false
  - [ ] Generate email verification token
  - [ ] Store token in database
  - [ ] Publish UserRegisteredEvent with token
  - [ ] Return RegisterResponse(userId, message: "Check email for verification")

- [ ] Task 4: Email Service (AC: #5)
  - [ ] Create EmailService interface
  - [ ] Create EmailTemplate for verification email
  - [ ] HTML email template with styled button
  - [ ] Include verification link with token
  - [ ] Configure SMTP or AWS SES settings
  - [ ] Implement sendVerificationEmail method

- [ ] Task 5: VerifyEmailCommand and Handler (AC: #6, #8)
  - [ ] Create VerifyEmailCommand(token)
  - [ ] Create VerifyEmailCommandHandler
  - [ ] Find token in database
  - [ ] Check token not expired (24 hours)
  - [ ] Find user by userId from token
  - [ ] Call user.verifyEmail() to set emailVerified=true
  - [ ] Delete used token
  - [ ] Return success message

- [ ] Task 6: Update Login to Check Email Verification (AC: #7)
  - [ ] Update LoginCommandHandler to check user.isEmailVerified()
  - [ ] Return error "Please verify your email" if not verified
  - [ ] Existing verified users can login normally

- [ ] Task 7: REST Controllers (AC: #1, #6)
  - [ ] Add POST /api/auth/register endpoint to AuthController
  - [ ] Add GET /api/auth/verify-email endpoint to AuthController
  - [ ] Return 201 Created on successful registration
  - [ ] Return 409 Conflict for duplicate email
  - [ ] Return 400 Bad Request for invalid password
  - [ ] Return 200 OK on successful email verification

- [ ] Task 8: Unit Tests
  - [ ] Test PasswordValidator with valid passwords
  - [ ] Test PasswordValidator rejects weak passwords
  - [ ] Test RegisterUserCommandHandler creates user
  - [ ] Test RegisterUserCommandHandler rejects duplicate email
  - [ ] Test RegisterUserCommandHandler validates password
  - [ ] Test VerifyEmailCommandHandler verifies email
  - [ ] Test VerifyEmailCommandHandler rejects expired token
  - [ ] Test LoginCommandHandler blocks unverified users

- [ ] Task 9: Integration Tests
  - [ ] Test POST /api/auth/register with valid data creates user
  - [ ] Test POST /api/auth/register with duplicate email returns 409
  - [ ] Test POST /api/auth/register with weak password returns 400
  - [ ] Test GET /api/auth/verify-email with valid token verifies email
  - [ ] Test GET /api/auth/verify-email with expired token returns error
  - [ ] Test login with unverified email fails
  - [ ] Test login after verification succeeds

## Dev Notes

### Architecture Patterns and Constraints

**Password Validation Rules:**
- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one number (0-9)
- Return clear error messages for each violation

**Email Verification Token:**
- Generate using SecureRandom for cryptographic security
- 32 characters (hex encoded)
- Store with expiration timestamp (24 hours)
- One-time use (delete after verification)

**Email Template Structure:**
```html
<!DOCTYPE html>
<html>
<head>
    <style>
        .button {
            background-color: #4CAF50;
            border: none;
            color: white;
            padding: 15px 32px;
            text-align: center;
            text-decoration: none;
            display: inline-block;
            font-size: 16px;
        }
    </style>
</head>
<body>
    <h1>Welcome to RapidPhotoUpload!</h1>
    <p>Please verify your email address by clicking the button below:</p>
    <a href="{{verificationLink}}" class="button">Verify Email</a>
    <p>This link will expire in 24 hours.</p>
</body>
</html>
```

**Email Service Configuration:**
- Use Spring Mail for SMTP
- Alternative: AWS SES for production
- Configure in application.yml

[Source: docs/epics/epic-1-authentication-onboarding.md#Story-1.2]

### Source Tree Components to Touch

```
backend/
├── build.gradle (may need spring-boot-starter-mail)
├── src/main/java/com/rapidphoto/
│   ├── security/
│   │   └── PasswordValidator.java (NEW)
│   ├── domain/verification/
│   │   ├── EmailVerificationToken.java (NEW)
│   │   └── EmailVerificationTokenRepository.java (NEW)
│   ├── email/
│   │   ├── EmailService.java (NEW)
│   │   └── EmailTemplate.java (NEW)
│   ├── cqrs/commands/
│   │   ├── RegisterUserCommand.java (EXISTS - enhance)
│   │   └── VerifyEmailCommand.java (EXISTS - may need enhancement)
│   ├── cqrs/commands/handlers/
│   │   ├── RegisterUserCommandHandler.java (EXISTS - enhance)
│   │   ├── VerifyEmailCommandHandler.java (EXISTS - enhance)
│   │   └── LoginCommandHandler.java (EXISTS - add email verification check)
│   ├── cqrs/dtos/
│   │   └── RegisterResponse.java (NEW)
│   └── api/
│       └── AuthController.java (EXISTS - add register + verify endpoints)
├── src/main/resources/
│   ├── application.yml (add email config)
│   └── db/migration/
│       └── V7__create_email_verification_tokens_table.sql (NEW)
└── src/test/java/com/rapidphoto/
    ├── security/
    │   └── PasswordValidatorTest.java (NEW)
    ├── cqrs/commands/handlers/
    │   ├── RegisterUserCommandHandlerTest.java (NEW or enhance)
    │   └── VerifyEmailCommandHandlerTest.java (NEW or enhance)
    └── api/
        └── AuthControllerIntegrationTest.java (enhance with registration tests)
```

### Testing Standards Summary

**Password Validation Tests:**
- Valid password: "Password123"
- Too short: "Pass1" (< 8 chars)
- No uppercase: "password123"
- No number: "Password"

**Email Verification Tests:**
- Valid token flow
- Expired token (mock time)
- Invalid token (random string)
- Already verified user (idempotent)

**Integration Tests:**
- Full registration → email sent → verification → login flow
- Mock email service in tests
- Use Testcontainers for database

### Project Structure Notes

**Dependencies on Previous Stories:**
- Story 1.1: JWT authentication infrastructure (login now checks email verification)
- Story 0.3: User domain model (verifyEmail() method exists)
- Story 0.4: CQRS commands (RegisterUserCommand and VerifyEmailCommand may exist)

**Email Service Implementation:**
- For MVP: Use simple SMTP (Gmail, SendGrid)
- For production: AWS SES recommended
- Mock in tests with @MockBean

### Learnings from Previous Story

**From Story 1.1 (JWT Authentication):**
- Existing LoginCommandHandler can be enhanced to check emailVerified flag
- User entity already has verifyEmail() method
- Spring Security configuration already in place

**Key Files to Reuse:**
- `backend/src/main/java/com/rapidphoto/domain/user/User.java` - verifyEmail() method
- `backend/src/main/java/com/rapidphoto/cqrs/commands/handlers/LoginCommandHandler.java` - add verification check
- `backend/src/main/java/com/rapidphoto/api/AuthController.java` - add registration endpoints

### References

- Epic Overview: [Source: docs/epics/epic-1-authentication-onboarding.md#Story-1.2]
- Spring Mail: https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email
- Password Validation: https://owasp.org/www-community/password-special-characters
- SecureRandom: https://docs.oracle.com/javase/8/docs/api/java/security/SecureRandom.html

## Dev Agent Record

### Context Reference

- TBD: docs/stories/1-2-user-registration-email-verification.context.xml

### Agent Model Used

claude-sonnet-4-5-20250929

### Debug Log References

### Completion Notes List

### File List
