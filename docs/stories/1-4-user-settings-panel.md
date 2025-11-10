# Story 1.4: User Settings Panel

Status: in_progress

## Story

As a user,
I want to customize my app preferences,
so that the app behaves according to my preferences.

## Acceptance Criteria

1. **Settings Screen**: Accessible from navigation
2. **Upload Complete Notifications**: Toggle for upload notifications
3. **Success Animations**: Toggle to enable/disable animations
4. **Success Sounds**: Toggle to enable/disable sound effects
5. **Theme Toggle**: Light/Dark/Auto theme selection
6. **Concurrent Upload Limit**: Slider for 1-20 concurrent uploads
7. **Auto-Retry Failed**: Toggle for auto-retry failed uploads
8. **Save Button**: Persists settings to backend API
9. **Settings Persist**: Settings persist across sessions
10. **Immediate Application**: Changes apply immediately after save

## Tasks / Subtasks

- [ ] Task 1: Create Backend Command & Handler (AC: #8, #9)
  - [ ] Create UpdateUserPreferencesCommand with all preference fields
  - [ ] Create UpdateUserPreferencesCommandHandler
  - [ ] Handler validates concurrent uploads range (1-20)
  - [ ] Handler updates UserPreferences entity
  - [ ] Handler saves to database via repository
  - [ ] Unit tests for command handler

- [ ] Task 2: Create API Endpoint (AC: #8)
  - [ ] Create UserController (or add to existing controller)
  - [ ] Add PUT /api/user/preferences endpoint
  - [ ] Map request DTO to UpdateUserPreferencesCommand
  - [ ] Return UserPreferencesDTO on success
  - [ ] Add JWT authentication requirement
  - [ ] Handle validation errors (400 Bad Request)

- [ ] Task 3: Create Settings Screen (AC: #1-7, #10)
  - [ ] Create SettingsScreen component
  - [ ] Integrate existing SettingsPanel organism
  - [ ] Fetch current preferences on mount (GET /api/user/preferences)
  - [ ] Pass settings state to SettingsPanel
  - [ ] Handle onSaveSettings callback
  - [ ] Call PUT /api/user/preferences on save
  - [ ] Show loading state during save
  - [ ] Show success/error message after save

- [ ] Task 4: Update Navigation (AC: #1)
  - [ ] Add Settings screen to MainStack in RootNavigator
  - [ ] Add Settings navigation button to HomeScreen
  - [ ] Use Settings icon from lucide-react-native
  - [ ] Add header navigation option

- [ ] Task 5: API Integration
  - [ ] Create userService.ts with updatePreferences()
  - [ ] Create userService.ts with getPreferences()
  - [ ] Use axios instance from api.ts
  - [ ] Handle authentication headers
  - [ ] Type-safe request/response interfaces

- [ ] Task 6: Testing
  - [ ] Unit test: UpdateUserPreferencesCommandHandler
  - [ ] Unit test: UserController endpoint
  - [ ] Integration test: PUT /api/user/preferences saves to database
  - [ ] Frontend test: SettingsScreen renders correctly
  - [ ] Frontend test: Save button calls API
  - [ ] E2E test: Settings persist across sessions

## Dev Notes

### Architecture Patterns and Constraints

**Backend Flow:**
```
1. PUT /api/user/preferences request received
2. Extract userId from JWT token (SecurityContext)
3. Create UpdateUserPreferencesCommand
4. Handler validates preferences
5. Handler loads UserPreferences entity by userId
6. Handler updates entity fields
7. Repository saves to database
8. Return UserPreferencesDTO
```

**Frontend Flow:**
```
1. User opens Settings screen from Home
2. SettingsScreen fetches GET /api/user/preferences
3. Pass current settings to SettingsPanel
4. User modifies toggles/theme/slider
5. User taps "Save Changes"
6. Call PUT /api/user/preferences with updated settings
7. Show success message
8. Settings immediately applied (theme, etc.)
```

**UserPreferences Fields:**
- animationsEnabled: boolean (default: true)
- soundEnabled: boolean (default: true)
- theme: LIGHT | DARK | SYSTEM (default: SYSTEM)
- concurrentUploads: int (1-20, default: 3)
- uploadCompleteNotifications: boolean (add to entity)
- autoRetryFailed: boolean (add to entity)

**SettingsPanel Integration:**
The existing SettingsPanel at `frontend/src/components/organisms/SettingsPanel.tsx` already has:
- UserSettings interface with notifications, appearance, upload sections
- Theme toggle (light/dark/auto)
- Success animations and sounds toggles
- Upload settings (auto-retry, concurrent limit)
- Modal/slide-in panel behavior
- Save callback

We need to:
1. Create SettingsScreen that wraps SettingsPanel
2. Fetch current preferences from backend
3. Map backend UserPreferencesDTO to SettingsPanel's UserSettings
4. Map SettingsPanel's UserSettings to backend UpdateUserPreferencesCommand
5. Call API on save

[Source: docs/epics/epic-1-authentication-onboarding.md#Story-1.4]

### Source Tree Components to Touch

```
backend/
├── src/main/java/com/rapidphoto/
│   ├── api/
│   │   └── UserController.java (NEW)
│   ├── cqrs/
│   │   ├── commands/
│   │   │   └── UpdateUserPreferencesCommand.java (NEW)
│   │   └── commands/handlers/
│   │       └── UpdateUserPreferencesCommandHandler.java (NEW)
│   └── domain/user/
│       └── UserPreferences.java (UPDATE - add missing fields)
└── src/test/java/com/rapidphoto/
    └── cqrs/commands/handlers/
        └── UpdateUserPreferencesCommandHandlerTest.java (NEW)

frontend/
├── src/
│   ├── screens/
│   │   └── SettingsScreen.tsx (NEW)
│   ├── navigation/
│   │   └── RootNavigator.tsx (UPDATE - add Settings screen)
│   ├── services/
│   │   └── userService.ts (NEW)
│   └── types/
│       └── user.ts (NEW - UserPreferences interface)
└── __tests__/
    └── screens/
        └── SettingsScreen.test.tsx (NEW)
```

### Testing Standards Summary

**Backend Unit Tests:**
- UpdateUserPreferencesCommandHandler: valid preferences
- UpdateUserPreferencesCommandHandler: invalid concurrent uploads (0, 21)
- UpdateUserPreferencesCommandHandler: null theme
- UpdateUserPreferencesCommandHandler: user not found

**Backend Integration Tests:**
- PUT /api/user/preferences: authenticated user saves successfully
- PUT /api/user/preferences: unauthenticated returns 401
- PUT /api/user/preferences: invalid data returns 400

**Frontend Unit Tests:**
- SettingsScreen: renders SettingsPanel with current preferences
- SettingsScreen: calls API on save
- SettingsScreen: shows error on API failure
- SettingsScreen: applies theme immediately

### Project Structure Notes

**Dependencies on Previous Stories:**
- Story 1.1: JWT authentication (extract userId from token)
- Story 1.3: Navigation, useAuth hook, API client
- Story 0.5: Design system (SettingsPanel already exists)

**Backend Entity Updates:**
UserPreferences needs additional fields:
- uploadCompleteNotifications: boolean (default: true)
- autoRetryFailed: boolean (default: true)

Map between backend and frontend:
```
Backend                     Frontend (SettingsPanel)
---------------------------------------------------
animationsEnabled    <->    appearance.successAnimations
soundEnabled         <->    appearance.successSounds
theme                <->    appearance.theme
concurrentUploads    <->    upload.concurrentLimit
autoRetryFailed      <->    upload.autoRetryFailed
(NEW) uploadCompleteNotifications <-> notifications.uploadComplete
```

### Learnings from Previous Story

**From Story 1.3 (Login/Logout UI):**
- useAuth hook provides userId for authenticated requests
- axios instance automatically attaches JWT token
- Navigation uses conditional rendering based on authentication
- Screens use useTheme hook for consistent styling

**Key Patterns:**
- Backend: Command → Handler → Repository → DTO
- Frontend: Screen → Service → API → State Update
- Always use DTOs, never expose domain entities
- Use Mono/Flux for reactive backend operations

### References

- Epic Overview: [docs/epics/epic-1-authentication-onboarding.md#Story-1.4]
- SettingsPanel: [frontend/src/components/organisms/SettingsPanel.tsx]
- UserPreferences Entity: [backend/src/main/java/com/rapidphoto/domain/user/UserPreferences.java]
- CQRS Pattern: [docs/stories/0-4-cqrs-structure-setup.md]

## Dev Agent Record

### Context Reference

- TBD: docs/stories/1-4-user-settings-panel.context.xml

### Agent Model Used

claude-sonnet-4-5-20250929

### Debug Log References

### Completion Notes List

### File List
