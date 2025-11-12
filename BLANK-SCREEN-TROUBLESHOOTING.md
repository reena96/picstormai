# Blank Screen Issue - Root Cause Analysis & Fix

## Problem Statement
Frontend displays blank screen at http://localhost:8081 despite:
- ✅ Webpack compiling successfully
- ✅ Bundle loading (3.26 MiB)
- ✅ HTML serving correctly with #root div
- ✅ No visible webpack errors

## Common Root Causes (In Order of Likelihood)

### 1. **JavaScript Runtime Error in App.tsx**
**Symptoms**: Bundle loads but React fails to render
**Causes**:
- Import errors (missing modules)
- TypeScript compilation errors being ignored
- React component throwing error during render
- Missing dependencies in node_modules

**Fix**:
```bash
# Check browser console for errors
# Look for red errors in DevTools Console

# Common issues:
- Missing API_BASE_URL or environment variables
- Broken imports after adding new dependencies
- React Navigation issues with web
```

### 2. **Index.tsx Not Being Executed**
**Symptoms**: Bundle loads but AppRegistry not running
**Causes**:
- webpack entry point misconfigured
- index.tsx not importing/running correctly

**Check**:
```javascript
// webpack.config.js entry point
entry: './src/index.tsx'  // Must be correct

// index.tsx must call:
AppRegistry.runApplication('PicStormAI', {
  rootTag: document.getElementById('root'),
});
```

### 3. **React Native Web Compatibility Issues**
**Symptoms**: React Native components failing on web
**Causes**:
- Missing react-native-web aliases in webpack
- Native-only APIs being called
- Platform-specific code not wrapped

**Fix**:
```javascript
// webpack.config.js must have:
resolve: {
  alias: {
    'react-native$': 'react-native-web',
  },
  extensions: ['.web.tsx', '.web.ts', '.tsx', '.ts', '.web.js', '.js']
}
```

### 4. **Environment Variable Missing**
**Symptoms**: App expects API_BASE_URL or other env vars
**Causes**:
- .env file not loaded
- webpack DefinePlugin not configured
- Runtime code expects process.env.XXX

**Fix**:
```javascript
// webpack.config.js
const webpack = require('webpack');

plugins: [
  new webpack.DefinePlugin({
    'process.env.API_BASE_URL': JSON.stringify(process.env.API_BASE_URL || 'http://localhost:8080'),
  })
]
```

### 5. **Broken Component After New Dependencies Added**
**Symptoms**: Blank screen after adding @stomp/stompjs, canvas-confetti, etc.
**Causes**:
- Import statements failing
- Module not web-compatible
- Type definitions causing runtime issues

**Today's Changes That Could Break**:
- Added @stomp/stompjs (WebSocket library)
- Added sockjs-client
- Added canvas-confetti
- Added react-native-confetti-cannon
- Created UploadCompletionModal.tsx

**Check**:
```bash
# See if removing new modal fixes it
# Comment out UploadCompletionModal import in App.tsx or wherever it's used
```

## Diagnostic Steps (In Order)

### Step 1: Check Browser Console
```bash
# Open DevTools (F12 or Cmd+Option+I)
# Look for red errors in Console tab
# Common errors:
- "Module not found"
- "Cannot read property of undefined"
- "Uncaught TypeError"
- "Failed to resolve module"
```

### Step 2: Check Webpack Output for Warnings
```bash
# Look at webpack compilation output
# Common issues:
- "Module not found: Error: Can't resolve 'X'"
- "Critical dependency: the request of a dependency is an expression"
```

### Step 3: Test Minimal App
```javascript
// Temporarily replace App.tsx with minimal version:
import React from 'react';
import { View, Text } from 'react-native';

export default function App() {
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>Hello World</Text>
    </View>
  );
}

// If this works, the issue is in the real App.tsx
```

### Step 4: Check API Connection
```javascript
// App.tsx might be waiting for API
// Check if AuthContext or API calls are blocking render
// Look for:
- useEffect with API calls that never resolve
- Suspense without fallback
- Context providers with async initialization
```

### Step 5: Check for TypeScript Errors Being Ignored
```bash
npm run type-check
# TypeScript errors can compile with webpack but break at runtime
```

## Quick Fix Checklist

- [ ] Open browser DevTools → Console tab
- [ ] Check for red errors
- [ ] Look at Network tab - is bundle.js loading?
- [ ] Check webpack terminal output for errors
- [ ] Try minimal App.tsx (just render "Hello World")
- [ ] Comment out recent changes (UploadCompletionModal)
- [ ] Check API_BASE_URL is defined
- [ ] Verify react-native-web alias in webpack.config.js

## Known Issue Pattern for This Project

**Pattern**: After adding new dependencies, blank screen appears

**Root Cause**: Usually one of:
1. Import of new dependency fails (not web-compatible)
2. New component uses Platform-specific APIs
3. Missing webpack configuration for new dependency
4. Environment variable required but not set

**Solution**:
1. Check browser console FIRST
2. Comment out new imports
3. Add minimal version back incrementally
4. Test at each step

## Prevention for Next Time

### Before Adding Dependencies:
1. Check if library is web-compatible (look for "react-native-web" in docs)
2. Test import in isolation first
3. Always check browser console after adding new code

### After Adding Dependencies:
1. Run `npm install`
2. Restart webpack dev server
3. Hard refresh browser (Cmd+Shift+R)
4. Check console for errors
5. If blank screen → immediately check console

### Standard Debug Flow:
```bash
# 1. Check browser console (ALWAYS FIRST)
open http://localhost:8081
# F12 → Console tab

# 2. Check webpack output
# Look for errors or warnings

# 3. Test minimal app
# Comment out everything except basic render

# 4. Add back incrementally
# Uncomment one piece at a time until it breaks
```

## For This Specific Session

**What We Added**:
- UploadCompletionModal.tsx (NEW)
- Dependencies: @stomp/stompjs, sockjs-client, canvas-confetti

**Most Likely Cause**:
- UploadCompletionModal imports canvas-confetti
- canvas-confetti might need special webpack config
- OR: Modal is being imported/rendered somewhere causing error
- OR: Missing environment variable for API calls

**Immediate Fix**:
1. Check browser console
2. If error mentions "canvas-confetti" → add webpack alias
3. If error mentions "process.env" → add DefinePlugin
4. If no error but blank → App.tsx has silent render issue

---

**REMEMBER**: ALWAYS check browser console FIRST when debugging blank screens!

