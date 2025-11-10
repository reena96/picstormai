# PicStormAI Frontend - Design System & Component Library

This is the React Native for Web frontend for PicStormAI, built with a comprehensive design system following Atomic Design methodology.

## 🚀 Quick Start

### Installation

```bash
npm install
```

### Development

```bash
# Run web development server
npm run web

# Run Storybook (component documentation)
npm run storybook

# Run tests
npm test

# Run tests in watch mode
npm run test:watch

# Generate test coverage report
npm run test:coverage

# Type checking
npm run type-check

# Linting
npm run lint
```

## 📁 Project Structure

```
src/
├── components/
│   ├── atoms/          # Basic building blocks (Button, Input, Text, etc.)
│   ├── molecules/      # Simple combinations (PhotoCard, ProgressBar, etc.)
│   └── organisms/      # Complex components (PhotoGrid, UploadDashboard, etc.)
├── contexts/
│   └── ThemeContext.tsx   # Theme provider with light/dark mode
├── styles/
│   └── tokens.ts          # Design tokens (colors, typography, spacing, etc.)
├── hooks/
│   └── useTheme.ts        # Theme hook
├── types/
│   └── theme.d.ts         # TypeScript type definitions
└── index.tsx              # Main entry point

.storybook/
├── stories/            # Component stories for documentation
├── main.js            # Storybook configuration
└── preview.js         # Storybook decorators and parameters
```

## 🎨 Design System

### Design Tokens

All design decisions are codified in `src/styles/tokens.ts`:

- **Colors**: Primary, semantic (success/error/warning), neutral grays
- **Typography**: Inter font family, sizes (12-60px), weights (100-800)
- **Spacing**: 8px grid system (4, 8, 12, 16, 24, 32, 48, 64px)
- **Shadows**: 6 elevation levels (xs to xl)
- **Border Radius**: sm (4px) to full (pill shape)
- **Animations**: Duration and easing functions

### Theme Support

- Light and dark themes via `ThemeContext`
- System preference detection (prefers-color-scheme)
- Persistent theme preference (localStorage/AsyncStorage)
- Theme toggle in Settings Panel

### Atomic Design Components

**Atoms (6 components)**:
- Button (primary, secondary, text, FAB variants)
- Input (text, email, password with validation)
- Text (h1-h4, body, caption variants)
- Icon (Lucide icons with theme support)
- Badge (success, error, warning, info states)
- Spinner (loading indicators)

**Molecules (5 components)**:
- PhotoCard (1:1 aspect ratio with metadata)
- ProgressBar (linear progress with gradient)
- UploadStatusIndicator (complete upload item display)
- TagChip (pill-shaped tags with remove button)
- EmptyState (illustration + message + CTA)

**Organisms (4 components)**:
- PhotoGrid (responsive 2-5 column grid with lazy loading)
- UploadDashboard (aggregate progress + categorized lists)
- SettingsPanel (slide-in panel with user preferences)
- Lightbox (full-screen photo viewer with navigation)

## ♿ Accessibility

All components meet WCAG 2.1 Level AA standards:

- ✅ Color contrast ratios: 4.5:1 minimum (body text), 3:1 (large text)
- ✅ Keyboard navigation: Tab, Enter, Escape, Arrow keys
- ✅ Screen reader support: ARIA labels, semantic elements
- ✅ Focus indicators: 3px outline, 2px offset
- ✅ Touch targets: Minimum 44×44px (iOS), 48×48dp (Android)

### Testing Accessibility

1. **Automated**: Run axe DevTools browser extension
2. **Keyboard**: Tab through all components, verify focus indicators
3. **Screen Readers**:
   - Web: NVDA (Windows) or VoiceOver (macOS)
   - iOS: VoiceOver in simulator
   - Android: TalkBack in emulator

## 🧪 Testing

### Unit Tests

Tests are co-located with components using Jest and React Native Testing Library.

```bash
# Run all tests
npm test

# Watch mode
npm run test:watch

# Coverage report
npm run test:coverage
```

### Storybook (Visual Testing)

```bash
# Start Storybook
npm run storybook

# Build static Storybook
npm run build-storybook
```

View component documentation and visual testing at: http://localhost:6006

## 🌐 Cross-Platform Support

Built with React Native for Web for 100% code reuse across platforms:

- **Web**: Chrome, Firefox, Safari (via react-native-web)
- **iOS**: iPhone, iPad (via React Native)
- **Android**: Phone, Tablet (via React Native)

Platform-specific code uses:
- `Platform.OS` checks
- `.web.tsx` / `.native.tsx` file extensions
- `Platform.select()` for inline platform-specific values

## 📦 Key Dependencies

- **react-native**: 0.73.0
- **react-native-web**: 0.19.9
- **lucide-react-native**: Icon library
- **typescript**: 5.3.3
- **@testing-library/react-native**: Testing utilities
- **@storybook/react**: Component documentation

## 🔧 Configuration Files

- `package.json`: Dependencies and scripts
- `tsconfig.json`: TypeScript configuration
- `webpack.config.js`: React Native Web bundler
- `jest.config.js`: Test configuration
- `.storybook/main.js`: Storybook configuration

## 📝 Development Notes

### Adding New Components

1. Create component in appropriate atomic layer directory
2. Add TypeScript types for props
3. Use `useTheme()` hook for theme-aware styling
4. Add accessibility attributes (accessibilityRole, accessibilityLabel)
5. Create unit tests (*.test.tsx)
6. Create Storybook story (*.stories.tsx)
7. Export from index.ts

### Design Token Usage

Always reference tokens instead of hard-coding values:

```typescript
// ✅ Good
backgroundColor: theme.colors.primary[500]
padding: theme.spacing[4]
fontSize: theme.typography.fontSize.base

// ❌ Bad
backgroundColor: '#2563EB'
padding: 16
fontSize: 16
```

### Theme-Aware Components

Use the `useTheme()` hook to access current theme:

```typescript
import { useTheme } from '../../hooks/useTheme';

const MyComponent = () => {
  const { theme, isDark, toggleTheme } = useTheme();

  return (
    <View style={{ backgroundColor: theme.colors.background }}>
      <Text style={{ color: theme.colors.text.primary }}>
        Current mode: {isDark ? 'Dark' : 'Light'}
      </Text>
    </View>
  );
};
```

## 🚧 Known Limitations (Story 0.5)

This is a foundation story - the following are deferred to later implementation:

- Full mobile app builds (iOS/Android)
- Image upload functionality
- Backend API integration
- State management beyond Context API
- Production optimizations (code splitting, lazy loading)
- E2E tests
- Performance profiling

## 📚 References

- [Story 0.5 Documentation](../docs/stories/0-5-design-system-component-library.md)
- [UI/UX Design System](../docs/ui-ux-design-system.md)
- [Architecture Documentation](../docs/ARCHITECTURE-BMAD.md)
- [React Native for Web](https://necolas.github.io/react-native-web/)
- [Lucide Icons](https://lucide.dev/)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

## 🎯 Next Steps

After Story 0.5 is complete, the next stories will:

1. **Story 0.6**: Integration tests for infrastructure
2. **Epic 1**: Photo upload implementation
3. **Epic 2**: Gallery and viewing features
4. **Epic 3**: Tagging and organization

---

Generated as part of Epic 0 (Foundation & Infrastructure) - Story 0.5
