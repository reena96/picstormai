# Story 1.5: Onboarding Tutorial (First-Time Users)

Status: todo

## Story

As a first-time user,
I want to see an interactive tutorial,
so that I understand how to use the app.

## Acceptance Criteria

1. **First Login Check**: Show onboarding on first login by checking user.hasSeenOnboarding flag
2. **3-Screen Carousel**: Display 3 onboarding screens with illustrations and tips
3. **Skip Button**: Each screen has a skip button to dismiss onboarding immediately
4. **Get Started Button**: Final screen has "Get Started" button instead of skip
5. **Mark Complete**: Call backend API to set hasSeenOnboarding=true when complete or skipped
6. **Never Show Again**: Onboarding never shows again once completed (unless reset in settings)
7. **Smooth Animations**: Carousel transitions are smooth and intuitive

## Tasks / Subtasks

- [ ] Task 1: Backend - Add hasSeenOnboarding to User Entity (AC: #1)
  - [ ] Add hasSeenOnboarding boolean field to User.java
  - [ ] Add getter method isHasSeenOnboarding()
  - [ ] Add business method markOnboardingComplete()
  - [ ] Update User entity tests

- [ ] Task 2: Backend - Database Migration (AC: #1)
  - [ ] Create V8__add_has_seen_onboarding.sql
  - [ ] Add has_seen_onboarding column (default false)
  - [ ] Test migration runs successfully

- [ ] Task 3: Backend - UpdateUserFlagCommand (AC: #5)
  - [ ] Create UpdateUserFlagCommand record
  - [ ] Create UpdateUserFlagCommandHandler
  - [ ] Implement handler logic to update hasSeenOnboarding
  - [ ] Return user data after update

- [ ] Task 4: Backend - API Endpoint (AC: #5)
  - [ ] Add PATCH /api/user/onboarding endpoint
  - [ ] Secure with JWT authentication
  - [ ] Call UpdateUserFlagCommandHandler
  - [ ] Return updated user data

- [ ] Task 5: Backend - Unit Tests
  - [ ] Test User.markOnboardingComplete()
  - [ ] Test UpdateUserFlagCommandHandler
  - [ ] Test /api/user/onboarding endpoint
  - [ ] Test idempotency (calling twice works)

- [ ] Task 6: Frontend - OnboardingScreen Component (AC: #2, #3, #4, #7)
  - [ ] Create OnboardingScreen.tsx
  - [ ] Implement 3-screen carousel with React Native
  - [ ] Add screen 1: "Welcome to RapidPhotoUpload!" + Upload icon
  - [ ] Add screen 2: "Upload photos quickly and easily" + Image icon
  - [ ] Add screen 3: "Organize with tags and view your gallery" + Tag icon
  - [ ] Add Skip button on screens 1 & 2
  - [ ] Add "Get Started" button on screen 3
  - [ ] Add smooth swipe/animation transitions
  - [ ] Add pagination dots indicator

- [ ] Task 7: Frontend - API Integration (AC: #5)
  - [ ] Create markOnboardingComplete() API call
  - [ ] Call API when user taps Skip or Get Started
  - [ ] Update local user state after API success
  - [ ] Handle API errors gracefully

- [ ] Task 8: Frontend - AuthContext Update (AC: #1)
  - [ ] Add GET /api/user/profile endpoint (backend)
  - [ ] Fetch user profile after login
  - [ ] Store hasSeenOnboarding in user state
  - [ ] Expose hasSeenOnboarding via AuthContext

- [ ] Task 9: Frontend - Navigation Flow (AC: #1, #6)
  - [ ] Check hasSeenOnboarding after login
  - [ ] If false, navigate to OnboardingScreen
  - [ ] If true, navigate directly to home screen
  - [ ] After onboarding complete, navigate to home screen

- [ ] Task 10: Testing
  - [ ] Test: First login shows onboarding
  - [ ] Test: Second login skips onboarding
  - [ ] Test: Skip button works on all screens
  - [ ] Test: Get Started button completes onboarding
  - [ ] Test: API call marks hasSeenOnboarding=true

## Dev Notes

### Architecture Patterns and Constraints

**Onboarding Flow:**
```
1. User logs in successfully → AuthContext sets user state
2. AuthContext fetches user profile from GET /api/user/profile
3. Navigation checks user.hasSeenOnboarding flag
4. If false → Navigate to OnboardingScreen
5. If true → Navigate to HomeScreen
6. User completes/skips onboarding → Call PATCH /api/user/onboarding
7. Backend updates hasSeenOnboarding=true
8. Frontend navigates to HomeScreen
9. On next login, hasSeenOnboarding=true → skip directly to home
```

**User Entity Changes:**
```java
@Table("users")
public class User implements Persistable<UUID> {
    // ... existing fields ...
    private boolean hasSeenOnboarding; // NEW FIELD

    // NEW METHOD
    public void markOnboardingComplete() {
        if (this.hasSeenOnboarding) {
            return; // Idempotent
        }
        this.hasSeenOnboarding = true;
        this.updatedAt = Instant.now();
    }

    // NEW GETTER
    public boolean isHasSeenOnboarding() {
        return hasSeenOnboarding;
    }
}
```

**Database Migration V8:**
```sql
ALTER TABLE users
ADD COLUMN has_seen_onboarding BOOLEAN DEFAULT FALSE NOT NULL;

COMMENT ON COLUMN users.has_seen_onboarding IS
'Tracks whether user has completed onboarding tutorial';
```

**Command Pattern:**
```java
public record UpdateUserFlagCommand(
    UUID userId,
    boolean hasSeenOnboarding
) {
    public UpdateUserFlagCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }
}

@Component
public class UpdateUserFlagCommandHandler {
    private final UserRepository userRepository;

    public Mono<User> handle(UpdateUserFlagCommand command) {
        return userRepository.findById(command.userId())
            .switchIfEmpty(Mono.error(new UserNotFoundException()))
            .flatMap(user -> {
                user.markOnboardingComplete();
                return userRepository.save(user);
            });
    }
}
```

**API Endpoint:**
```java
@PatchMapping("/api/user/onboarding")
public Mono<UserResponse> markOnboardingComplete(
    @AuthenticationPrincipal JwtAuthenticationToken token
) {
    UUID userId = UUID.fromString(token.getName());
    UpdateUserFlagCommand command = new UpdateUserFlagCommand(userId, true);

    return updateUserFlagCommandHandler.handle(command)
        .map(UserResponse::from);
}
```

**Frontend Onboarding Screen:**
```typescript
// OnboardingScreen.tsx
const onboardingScreens = [
  {
    id: 1,
    title: "Welcome to RapidPhotoUpload!",
    description: "Upload your photos quickly and securely",
    icon: "Upload",
  },
  {
    id: 2,
    title: "Upload photos quickly and easily",
    description: "Batch upload hundreds of photos at once",
    icon: "Image",
  },
  {
    id: 3,
    title: "Organize with tags and view your gallery",
    description: "Tag, search, and manage your photo collection",
    icon: "Tag",
  },
];

// Use FlatList with horizontal pagination
// lucide-react-native for icons
// Skip button on screens 1 & 2
// "Get Started" button on screen 3
```

[Source: docs/epics/epic-1-authentication-onboarding.md#Story-1.5]

### Source Tree Components to Touch

```
backend/
├── src/main/java/com/rapidphoto/
│   ├── domain/user/
│   │   └── User.java (MODIFY - add hasSeenOnboarding field)
│   ├── cqrs/commands/
│   │   ├── UpdateUserFlagCommand.java (NEW)
│   │   └── handlers/
│   │       └── UpdateUserFlagCommandHandler.java (NEW)
│   ├── api/
│   │   └── UserController.java (NEW or MODIFY - add PATCH endpoint)
│   └── dto/
│       └── UserResponse.java (MODIFY - add hasSeenOnboarding field)
├── src/main/resources/db/migration/
│   └── V8__add_has_seen_onboarding.sql (NEW)
└── src/test/java/com/rapidphoto/
    ├── domain/user/UserTest.java (MODIFY)
    ├── cqrs/handlers/UpdateUserFlagCommandHandlerTest.java (NEW)
    └── api/UserControllerTest.java (NEW or MODIFY)

frontend/
├── src/
│   ├── screens/
│   │   └── OnboardingScreen.tsx (NEW)
│   ├── contexts/
│   │   └── AuthContext.tsx (MODIFY - fetch user profile)
│   ├── navigation/
│   │   └── RootNavigator.tsx (MODIFY - add onboarding check)
│   ├── services/
│   │   └── api.ts (MODIFY - add user endpoints)
│   └── types/
│       └── user.ts (NEW - User type with hasSeenOnboarding)
└── __tests__/
    └── screens/
        └── OnboardingScreen.test.tsx (NEW)
```

### Testing Standards Summary

**Backend Unit Tests:**
- User.markOnboardingComplete() marks flag true
- User.markOnboardingComplete() is idempotent
- UpdateUserFlagCommandHandler updates user
- UpdateUserFlagCommandHandler throws UserNotFoundException
- PATCH /api/user/onboarding requires authentication
- PATCH /api/user/onboarding updates hasSeenOnboarding

**Frontend Tests:**
- OnboardingScreen renders 3 screens
- Skip button dismisses onboarding
- Get Started button completes onboarding
- Navigation shows onboarding only on first login
- API call marks onboarding complete

**Test Coverage Targets:**
- User entity: 100%
- UpdateUserFlagCommandHandler: 100%
- OnboardingScreen: >85%

### Project Structure Notes

**Dependencies on Previous Stories:**
- Story 1.1: JWT authentication backend
- Story 1.3: Login UI and AuthContext

**New Dependencies:**
- None (using existing React Native components)
- Icons: lucide-react-native (likely already installed)

**Key Design Decisions:**
- Simple carousel with FlatList (no external library needed)
- No Lottie animations yet (can add later)
- Simple text and icons using lucide-react-native
- Onboarding flag stored in User entity (not separate preferences table)
- Single API call to mark complete (no tracking per screen)

### Learnings from Previous Story

**From Story 1.3 (Login/Logout UI):**
- AuthContext pattern for user state management
- JWT token storage and refresh logic
- Navigation structure (AuthStack vs MainStack)
- API service with axios interceptors

**Key API Contracts:**
```typescript
// GET /api/user/profile (NEW)
Response: {
  id: string;
  email: string;
  displayName: string;
  emailVerified: boolean;
  hasSeenOnboarding: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

// PATCH /api/user/onboarding (NEW)
Request: (empty body, user from JWT token)
Response: {
  id: string;
  email: string;
  displayName: string;
  emailVerified: boolean;
  hasSeenOnboarding: boolean; // true after call
  createdAt: string;
  lastLoginAt?: string;
}
```

### References

- Epic Overview: [docs/epics/epic-1-authentication-onboarding.md#Story-1.5]
- React Native FlatList: https://reactnative.dev/docs/flatlist
- Lucide React Native: https://lucide.dev/guide/packages/lucide-react-native
- Spring Data R2DBC: https://spring.io/projects/spring-data-r2dbc

## Dev Agent Record

### Context Reference

- TBD: docs/stories/1-5-onboarding-tutorial.context.xml

### Agent Model Used

claude-sonnet-4-5-20250929

### Debug Log References

### Completion Notes List

### File List
