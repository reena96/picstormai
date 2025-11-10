# Story 1.3: Login/Logout UI (Web + Mobile)

Status: todo

## Story

As a user,
I want to log in and log out from a beautiful UI,
so that I can access my account on any device.

## Acceptance Criteria

1. **Login Screen**: Enter email and password to log in
2. **Valid Login**: Successful login navigates to home screen
3. **Invalid Credentials**: Display error "Invalid email or password"
4. **Email Validation**: Validate email format before submission
5. **Password Toggle**: Show/hide password with eye icon
6. **Logout Button**: Tap logout to clear tokens and return to login screen
7. **Token Persistence**: Tokens persist across app restarts via AsyncStorage
8. **Auto-Refresh**: Axios interceptor auto-refreshes token on 401 response
9. **Loading States**: Show spinner during login/logout operations
10. **Keyboard Handling**: Proper keyboard behavior on mobile (KeyboardAvoidingView)

## Tasks / Subtasks

- [ ] Task 1: Create Auth Context & Hook (AC: #7, #8)
  - [ ] Create AuthContext with user state
  - [ ] Create useAuth hook for login/logout/refresh
  - [ ] Implement token storage with AsyncStorage
  - [ ] Add auto-login on app start if valid token exists
  - [ ] Export AuthProvider for App.tsx

- [ ] Task 2: Create API Client with Interceptors (AC: #8)
  - [ ] Create axios instance with base URL
  - [ ] Add request interceptor to attach Authorization header
  - [ ] Add response interceptor to handle 401 errors
  - [ ] Implement token refresh logic on 401
  - [ ] Add retry logic after successful refresh

- [ ] Task 3: Create Login Screen UI (AC: #1, #4, #5, #10)
  - [ ] Create LoginScreen component
  - [ ] Add email Input with validation
  - [ ] Add password Input with toggle visibility
  - [ ] Add "Log In" Button
  - [ ] Add KeyboardAvoidingView for mobile
  - [ ] Add form validation (email format, required fields)
  - [ ] Style with design system tokens

- [ ] Task 4: Login Functionality (AC: #2, #3, #9)
  - [ ] Connect login form to useAuth.login()
  - [ ] Show loading spinner during API call
  - [ ] Handle success: navigate to home screen
  - [ ] Handle error: display "Invalid email or password"
  - [ ] Clear error message on form change

- [ ] Task 5: Logout Functionality (AC: #6)
  - [ ] Add logout button to home/settings screen
  - [ ] Call useAuth.logout() on tap
  - [ ] Clear AsyncStorage tokens
  - [ ] Navigate to login screen
  - [ ] Show confirmation dialog (optional)

- [ ] Task 6: Navigation Setup
  - [ ] Install React Navigation
  - [ ] Create AuthStack (Login, Register screens)
  - [ ] Create MainStack (Home, Settings screens)
  - [ ] Use conditional rendering based on isAuthenticated

- [ ] Task 7: Unit Tests
  - [ ] Test useAuth hook: login success
  - [ ] Test useAuth hook: login failure
  - [ ] Test useAuth hook: logout
  - [ ] Test useAuth hook: auto-refresh on 401
  - [ ] Test LoginScreen: form validation
  - [ ] Test LoginScreen: submit button disabled when invalid
  - [ ] Test Axios interceptor: attaches token
  - [ ] Test Axios interceptor: refreshes on 401

- [ ] Task 8: E2E Tests (Detox or similar)
  - [ ] Test: Login with valid credentials navigates to home
  - [ ] Test: Login with invalid credentials shows error
  - [ ] Test: Logout clears tokens and returns to login
  - [ ] Test: Auto-login on app restart
  - [ ] Accessibility test: Screen reader navigation

## Dev Notes

### Architecture Patterns and Constraints

**Authentication Flow:**
```
1. User enters email/password → LoginScreen
2. LoginScreen calls useAuth.login(email, password)
3. useAuth makes POST /api/auth/login
4. Backend returns { accessToken, refreshToken, expiresIn }
5. useAuth stores tokens in AsyncStorage
6. useAuth sets user state → triggers navigation to MainStack
7. All API calls attach Authorization: Bearer {accessToken}
8. On 401 response → axios interceptor calls refresh endpoint
9. If refresh succeeds → retry original request
10. If refresh fails → logout user
```

**Token Storage:**
- Use @react-native-async-storage/async-storage
- Keys: @auth:accessToken, @auth:refreshToken
- Clear on logout
- Load on app start to check if user is logged in

**Axios Interceptor Pattern:**
```typescript
// Request interceptor
axios.interceptors.request.use(config => {
  const token = await AsyncStorage.getItem('@auth:accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      const newToken = await refreshToken();
      error.config.headers.Authorization = `Bearer ${newToken}`;
      return axios(error.config);
    }
    return Promise.reject(error);
  }
);
```

**Navigation Structure:**
```
App.tsx
├── AuthProvider (wraps entire app)
└── NavigationContainer
    ├── if (!isAuthenticated) → AuthStack
    │   ├── LoginScreen
    │   └── RegisterScreen (Story 1.2, future)
    └── if (isAuthenticated) → MainStack
        ├── HomeScreen
        ├── UploadScreen
        └── SettingsScreen
```

[Source: docs/epics/epic-1-authentication-onboarding.md#Story-1.3]

### Source Tree Components to Touch

```
frontend/
├── package.json (add @react-native-async-storage/async-storage, @react-navigation/native)
├── src/
│   ├── App.tsx (wrap with AuthProvider, add navigation)
│   ├── contexts/
│   │   └── AuthContext.tsx (NEW)
│   ├── hooks/
│   │   └── useAuth.ts (NEW)
│   ├── services/
│   │   └── api.ts (NEW - axios instance with interceptors)
│   ├── screens/
│   │   ├── LoginScreen.tsx (NEW)
│   │   ├── HomeScreen.tsx (NEW - placeholder)
│   │   └── SettingsScreen.tsx (NEW - placeholder)
│   ├── navigation/
│   │   ├── AuthStack.tsx (NEW)
│   │   ├── MainStack.tsx (NEW)
│   │   └── RootNavigator.tsx (NEW)
│   ├── types/
│   │   └── auth.ts (NEW - User, LoginRequest, LoginResponse types)
│   └── utils/
│       └── validation.ts (email format validation)
└── __tests__/
    ├── hooks/
    │   └── useAuth.test.ts (NEW)
    ├── services/
    │   └── api.test.ts (NEW)
    └── screens/
        └── LoginScreen.test.tsx (NEW)
```

### Testing Standards Summary

**Unit Tests (Jest + React Testing Library):**
- useAuth hook: login, logout, auto-refresh
- LoginScreen: form validation, error messages
- API interceptors: token attachment, 401 handling

**E2E Tests (Detox):**
- Login flow: valid/invalid credentials
- Logout flow
- Auto-login on restart
- Accessibility (screen reader)

**Test Coverage Targets:**
- useAuth hook: 100%
- LoginScreen: >90%
- API interceptors: 100%

### Project Structure Notes

**Dependencies on Previous Stories:**
- Story 1.1: JWT backend endpoints (/api/auth/login, /api/auth/refresh)
- Story 1.2: Registration backend (future RegisterScreen)
- Story 0.5: Design system (Button, Input, Spinner components)

**New Dependencies:**
```json
{
  "@react-native-async-storage/async-storage": "^1.21.0",
  "@react-navigation/native": "^6.1.9",
  "@react-navigation/native-stack": "^6.9.17",
  "axios": "^1.6.2",
  "react-native-screens": "^3.29.0",
  "react-native-safe-area-context": "^4.8.2"
}
```

**Dev Dependencies:**
```json
{
  "@testing-library/react-native": "^12.4.1",
  "@testing-library/jest-native": "^5.4.3",
  "detox": "^20.13.5"
}
```

### Learnings from Previous Story

**From Story 1.1 (JWT Authentication Backend):**
- Login endpoint: POST /api/auth/login
- Response: { accessToken, refreshToken, expiresIn, tokenType }
- Refresh endpoint: POST /api/auth/refresh { refreshToken }
- Access token expires in 15 minutes
- Refresh token expires in 30 days

**Key API Contracts:**
```typescript
// POST /api/auth/login
Request: {
  email: string;
  password: string;
}
Response: {
  accessToken: string;
  refreshToken: string;
  expiresIn: number; // seconds (900 for 15 min)
  tokenType: "Bearer";
}

// POST /api/auth/refresh
Request: {
  refreshToken: string;
}
Response: {
  accessToken: string;
  refreshToken: string; // New refresh token (rotation)
  expiresIn: number;
  tokenType: "Bearer";
}
```

### References

- Epic Overview: [docs/epics/epic-1-authentication-onboarding.md#Story-1.3]
- React Navigation: https://reactnavigation.org/docs/getting-started
- AsyncStorage: https://react-native-async-storage.github.io/async-storage/
- Axios Interceptors: https://axios-http.com/docs/interceptors
- React Native for Web: https://necolas.github.io/react-native-web/

## Dev Agent Record

### Context Reference

- TBD: docs/stories/1-3-login-logout-ui.context.xml

### Agent Model Used

claude-sonnet-4-5-20250929

### Debug Log References

### Completion Notes List

### File List
