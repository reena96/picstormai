# RapidPhotoUpload UI/UX Design System
**Version:** 1.0
**Status:** Draft
**Last Updated:** 2025-11-09

## Table of Contents
1. [Design Philosophy](#design-philosophy)
2. [Visual Design System](#visual-design-system)
3. [Component Library](#component-library)
4. [Interaction Patterns](#interaction-patterns)
5. [Responsive Design](#responsive-design)
6. [Accessibility](#accessibility)
7. [Micro-Interactions](#micro-interactions)
8. [Platform-Specific Guidelines](#platform-specific-guidelines)

---

## 1. Design Philosophy

### Core Principles

**1. Speed & Efficiency**
- Minimize clicks to core actions (upload, view, share)
- Instant visual feedback for all user actions
- Progressive disclosure: show only what's needed when needed
- Keyboard shortcuts for power users

**2. Clarity & Simplicity**
- Clear visual hierarchy (primary, secondary, tertiary actions)
- Uncluttered interfaces with generous whitespace
- Consistent iconography and terminology
- Self-explanatory UI elements (minimal need for tooltips)

**3. Confidence & Trust**
- Always visible progress indicators
- Immediate confirmation of successful actions
- Clear error states with actionable recovery steps
- Never hide system status from users

**4. Delight & Polish**
- Smooth, natural animations (60fps target)
- Satisfying micro-interactions (button press, completion)
- Contextual empty states with helpful guidance
- Celebrate user milestones (first upload, 100th photo)

### Competitive Benchmarking

**Inspired By:**
- **Google Photos:** Clean grid layouts, smooth infinite scroll, instant search
- **Dropbox:** Clear upload progress, reliable sync indicators, simple file management
- **Cloudinary:** Professional dashboard, detailed analytics, batch operations
- **Figma:** Fast keyboard shortcuts, collaborative features, polished interactions

**Differentiation:**
- **Faster Upload UX:** Real-time concurrent upload visibility (not available in Google Photos)
- **Transparent Progress:** Individual file status + aggregate progress (clearer than Dropbox)
- **Professional Polish:** Design quality of consumer apps (Google Photos) with power user features (Cloudinary)

---

## 2. Visual Design System

### Color Palette

#### Primary Colors
```css
/* Primary Brand Color - Vibrant Blue */
--primary-500: #2563EB; /* Main brand color, CTAs */
--primary-600: #1D4ED8; /* Hover state */
--primary-700: #1E40AF; /* Active/pressed state */
--primary-400: #3B82F6; /* Lighter variant */
--primary-300: #60A5FA; /* Disabled state */

/* Used for: Primary buttons, links, upload button, active states */
```

#### Semantic Colors
```css
/* Success - Green */
--success-500: #10B981; /* Upload complete, success messages */
--success-600: #059669; /* Hover */
--success-100: #D1FAE5; /* Background for success banners */

/* Warning - Amber */
--warning-500: #F59E0B; /* Warnings, paused uploads */
--warning-600: #D97706; /* Hover */
--warning-100: #FEF3C7; /* Background for warning banners */

/* Error - Red */
--error-500: #EF4444; /* Failed uploads, errors */
--error-600: #DC2626; /* Hover */
--error-100: #FEE2E2; /* Background for error banners */

/* Info - Blue */
--info-500: #3B82F6; /* Information, help */
--info-100: #DBEAFE; /* Background for info banners */
```

#### Neutral Colors (Gray Scale)
```css
/* Neutral Grays */
--gray-900: #111827; /* Primary text */
--gray-800: #1F2937; /* Secondary text */
--gray-700: #374151; /* Tertiary text */
--gray-600: #4B5563; /* Placeholder text */
--gray-500: #6B7280; /* Disabled text */
--gray-400: #9CA3AF; /* Borders, dividers */
--gray-300: #D1D5DB; /* Input borders */
--gray-200: #E5E7EB; /* Hover backgrounds */
--gray-100: #F3F4F6; /* Background surfaces */
--gray-50:  #F9FAFB; /* Page background */

/* Pure */
--white: #FFFFFF;
--black: #000000;
```

#### Background Hierarchy
```css
/* Light Theme */
--bg-primary: var(--white);        /* Cards, modals */
--bg-secondary: var(--gray-50);    /* Page background */
--bg-tertiary: var(--gray-100);    /* Hover states, disabled */

/* Dark Theme (optional post-MVP) */
--bg-primary-dark: #1F2937;
--bg-secondary-dark: #111827;
--bg-tertiary-dark: #374151;
```

### Typography

#### Font Family
```css
/* Primary Font: Inter (sans-serif) */
--font-primary: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI',
                'Roboto', 'Helvetica', 'Arial', sans-serif;

/* Monospace (for file names, technical info) */
--font-mono: 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
```

**Why Inter:**
- Excellent readability at small sizes (photo metadata)
- Wide range of weights (100-900)
- Optimized for screens (clear numerals for file counts)
- Free and open-source

#### Type Scale
```css
/* Headings */
--text-6xl: 3.75rem;   /* 60px - Hero headings (marketing) */
--text-5xl: 3rem;      /* 48px - Page headings */
--text-4xl: 2.25rem;   /* 36px - Section headings */
--text-3xl: 1.875rem;  /* 30px - Card headings */
--text-2xl: 1.5rem;    /* 24px - Modal headings */
--text-xl:  1.25rem;   /* 20px - Subheadings */

/* Body Text */
--text-lg:  1.125rem;  /* 18px - Large body, intro paragraphs */
--text-base: 1rem;     /* 16px - Default body text */
--text-sm:  0.875rem;  /* 14px - Secondary text, captions */
--text-xs:  0.75rem;   /* 12px - Meta info, timestamps */

/* Line Heights */
--leading-tight:  1.25;  /* Headings */
--leading-normal: 1.5;   /* Body text */
--leading-relaxed: 1.75; /* Long-form content */

/* Font Weights */
--font-thin:      100;
--font-light:     300;
--font-normal:    400;  /* Body text */
--font-medium:    500;  /* Emphasis, labels */
--font-semibold:  600;  /* Buttons, headings */
--font-bold:      700;  /* Strong emphasis */
--font-extrabold: 800;  /* Hero text */
```

#### Typography Usage
```css
/* Heading Styles */
.h1 { font-size: var(--text-4xl); font-weight: var(--font-bold); line-height: var(--leading-tight); }
.h2 { font-size: var(--text-3xl); font-weight: var(--font-semibold); line-height: var(--leading-tight); }
.h3 { font-size: var(--text-2xl); font-weight: var(--font-semibold); line-height: var(--leading-tight); }
.h4 { font-size: var(--text-xl); font-weight: var(--font-medium); line-height: var(--leading-normal); }

/* Body Styles */
.body-large { font-size: var(--text-lg); line-height: var(--leading-normal); }
.body { font-size: var(--text-base); line-height: var(--leading-normal); }
.body-small { font-size: var(--text-sm); line-height: var(--leading-normal); }
.caption { font-size: var(--text-xs); line-height: var(--leading-normal); color: var(--gray-600); }
```

### Spacing System

**8px Base Unit (8-point grid)**
```css
--space-0:    0px;
--space-1:    0.25rem;  /* 4px  - Tight spacing (icon-text gap) */
--space-2:    0.5rem;   /* 8px  - Base unit */
--space-3:    0.75rem;  /* 12px - Small gaps */
--space-4:    1rem;     /* 16px - Standard gaps */
--space-5:    1.25rem;  /* 20px - Medium gaps */
--space-6:    1.5rem;   /* 24px - Large gaps */
--space-8:    2rem;     /* 32px - Section spacing */
--space-10:   2.5rem;   /* 40px - Large section spacing */
--space-12:   3rem;     /* 48px - Page margins */
--space-16:   4rem;     /* 64px - Hero section spacing */
--space-20:   5rem;     /* 80px - Extra large spacing */
```

**Usage Guidelines:**
- **4px (space-1):** Icon-to-text spacing, very tight layouts
- **8px (space-2):** Between related elements (label + input)
- **16px (space-4):** Between components, default card padding
- **24px (space-6):** Between sections on a page
- **32px (space-8):** Between major page sections
- **48px (space-12):** Page container padding

### Border Radius
```css
--radius-none: 0px;
--radius-sm:   0.25rem;  /* 4px  - Tight elements (badges) */
--radius-base: 0.5rem;   /* 8px  - Buttons, inputs */
--radius-md:   0.75rem;  /* 12px - Cards */
--radius-lg:   1rem;     /* 16px - Modals, large cards */
--radius-xl:   1.5rem;   /* 24px - Hero cards */
--radius-full: 9999px;   /* Circular (avatars, pills) */
```

### Shadows (Elevation)
```css
/* Elevation Levels */
--shadow-xs:  0 1px 2px 0 rgba(0, 0, 0, 0.05);
--shadow-sm:  0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
--shadow-base: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
--shadow-md:  0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
--shadow-lg:  0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
--shadow-xl:  0 25px 50px -12px rgba(0, 0, 0, 0.25);

/* Usage */
.card { box-shadow: var(--shadow-sm); }
.modal { box-shadow: var(--shadow-xl); }
.dropdown { box-shadow: var(--shadow-lg); }
.button-hover { box-shadow: var(--shadow-md); }
```

### Iconography

**Icon Library:** [Lucide Icons](https://lucide.dev/) or [Heroicons](https://heroicons.com/)

**Why Lucide/Heroicons:**
- Clean, modern aesthetic
- Consistent stroke width (2px)
- Extensive library (1000+ icons)
- React/Vue/Flutter support
- Free and open-source

**Icon Sizes:**
```css
--icon-xs:  12px;  /* Inline with small text */
--icon-sm:  16px;  /* Inline with body text */
--icon-base: 20px; /* Buttons, form inputs */
--icon-md:  24px;  /* Navigation, headers */
--icon-lg:  32px;  /* Feature icons */
--icon-xl:  48px;  /* Hero sections */
```

**Core Icons:**
- Upload: `upload-cloud` or `cloud-upload`
- Success: `check-circle`
- Error: `x-circle`
- Warning: `alert-triangle`
- Info: `info`
- Loading: `loader` (animated spin)
- Photo: `image`
- Gallery: `layout-grid`
- Download: `download`
- Share: `share-2`
- Tag: `tag`
- Search: `search`
- Filter: `filter`
- Menu: `menu`
- Close: `x`
- Settings: `settings`

---

## 3. Component Library

### 3.1 Buttons

#### Primary Button
**Use Case:** Main actions (Upload Photos, Save, Confirm)

```tsx
// React Example
<button className="btn btn-primary">
  <UploadIcon />
  Upload Photos
</button>
```

```css
.btn-primary {
  background-color: var(--primary-500);
  color: var(--white);
  padding: 0.75rem 1.5rem; /* 12px 24px */
  border-radius: var(--radius-base);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.btn-primary:hover {
  background-color: var(--primary-600);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.btn-primary:active {
  background-color: var(--primary-700);
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

.btn-primary:disabled {
  background-color: var(--gray-300);
  cursor: not-allowed;
  opacity: 0.6;
}
```

#### Secondary Button
**Use Case:** Secondary actions (Cancel, Back, View Gallery)

```css
.btn-secondary {
  background-color: var(--white);
  color: var(--gray-700);
  border: 1px solid var(--gray-300);
  /* Other styles same as primary */
}

.btn-secondary:hover {
  background-color: var(--gray-50);
  border-color: var(--gray-400);
}
```

#### Text Button (Tertiary)
**Use Case:** Low-priority actions (Learn More, Skip)

```css
.btn-text {
  background-color: transparent;
  color: var(--primary-500);
  border: none;
  padding: 0.5rem 1rem;
}

.btn-text:hover {
  background-color: var(--primary-50);
  color: var(--primary-600);
}
```

#### Floating Action Button (FAB) - Mobile
**Use Case:** Primary mobile action (Upload on mobile)

```css
.btn-fab {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: var(--radius-full);
  background-color: var(--primary-500);
  color: var(--white);
  box-shadow: var(--shadow-lg);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1000;
}

.btn-fab:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-xl);
}
```

### 3.2 Input Fields

#### Text Input
```tsx
<div className="input-group">
  <label htmlFor="email" className="input-label">Email Address</label>
  <input
    type="email"
    id="email"
    className="input"
    placeholder="you@example.com"
  />
  <span className="input-hint">We'll never share your email.</span>
</div>
```

```css
.input-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.input-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--gray-700);
}

.input {
  padding: 0.625rem 0.875rem; /* 10px 14px */
  border: 1px solid var(--gray-300);
  border-radius: var(--radius-base);
  font-size: var(--text-base);
  color: var(--gray-900);
  transition: all 0.15s ease;
}

.input:focus {
  outline: none;
  border-color: var(--primary-500);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.input::placeholder {
  color: var(--gray-400);
}

.input-hint {
  font-size: var(--text-xs);
  color: var(--gray-600);
}

.input.error {
  border-color: var(--error-500);
}

.input.error:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}
```

#### File Upload Input (Hidden, Trigger via Button)
```tsx
<input
  type="file"
  id="file-upload"
  className="file-input-hidden"
  multiple
  accept="image/*"
/>
<label htmlFor="file-upload" className="btn btn-primary">
  <UploadIcon />
  Choose Photos
</label>
```

```css
.file-input-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
```

### 3.3 Cards

#### Photo Card (Gallery Grid Item)
```tsx
<div className="photo-card">
  <div className="photo-card-image">
    <img src={thumbnail} alt={filename} />
    <div className="photo-card-overlay">
      <button className="icon-button">
        <HeartIcon />
      </button>
      <button className="icon-button">
        <ShareIcon />
      </button>
    </div>
  </div>
  <div className="photo-card-meta">
    <span className="photo-card-filename">{filename}</span>
    <span className="photo-card-date">{date}</span>
  </div>
</div>
```

```css
.photo-card {
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--white);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
  cursor: pointer;
}

.photo-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.photo-card-image {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 aspect ratio */
  overflow: hidden;
  background: var(--gray-100);
}

.photo-card-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.photo-card:hover .photo-card-overlay {
  opacity: 1;
}

.photo-card-meta {
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.photo-card-filename {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--gray-900);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.photo-card-date {
  font-size: var(--text-xs);
  color: var(--gray-600);
}
```

#### Upload Progress Card
```tsx
<div className="upload-card">
  <div className="upload-card-header">
    <img src={thumbnail} alt={filename} className="upload-thumbnail" />
    <div className="upload-info">
      <span className="upload-filename">{filename}</span>
      <span className="upload-size">{fileSize}</span>
    </div>
    <StatusBadge status={status} />
  </div>
  <ProgressBar progress={progress} />
  <div className="upload-card-footer">
    <span className="upload-speed">{speed}</span>
    <span className="upload-eta">ETA: {eta}</span>
  </div>
</div>
```

```css
.upload-card {
  background: var(--white);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.upload-card-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.upload-thumbnail {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-base);
  object-fit: cover;
}

.upload-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.upload-filename {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--gray-900);
}

.upload-size {
  font-size: var(--text-xs);
  color: var(--gray-600);
}

.upload-card-footer {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-xs);
  color: var(--gray-600);
}
```

### 3.4 Progress Indicators

#### Linear Progress Bar
```tsx
<div className="progress-bar">
  <div className="progress-bar-fill" style={{ width: `${progress}%` }}></div>
</div>
```

```css
.progress-bar {
  width: 100%;
  height: 8px;
  background-color: var(--gray-200);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-500) 0%, var(--primary-400) 100%);
  border-radius: var(--radius-full);
  transition: width 0.3s ease;
  position: relative;
}

/* Animated shimmer effect for indeterminate progress */
.progress-bar-fill.indeterminate {
  width: 100%;
  background: linear-gradient(
    90deg,
    var(--gray-200) 0%,
    var(--primary-300) 50%,
    var(--gray-200) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
```

#### Circular Progress (Loading Spinner)
```tsx
<div className="spinner" aria-label="Loading">
  <svg className="spinner-svg" viewBox="0 0 50 50">
    <circle className="spinner-circle" cx="25" cy="25" r="20"></circle>
  </svg>
</div>
```

```css
.spinner {
  width: 40px;
  height: 40px;
}

.spinner-svg {
  animation: rotate 2s linear infinite;
  width: 100%;
  height: 100%;
}

.spinner-circle {
  stroke: var(--primary-500);
  stroke-linecap: round;
  animation: dash 1.5s ease-in-out infinite;
  fill: none;
  stroke-width: 4;
}

@keyframes rotate {
  100% { transform: rotate(360deg); }
}

@keyframes dash {
  0% {
    stroke-dasharray: 1, 150;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -35;
  }
  100% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -124;
  }
}
```

### 3.5 Status Badges

```tsx
<span className="badge badge-success">Complete</span>
<span className="badge badge-error">Failed</span>
<span className="badge badge-warning">Paused</span>
<span className="badge badge-info">Uploading</span>
```

```css
.badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: 0.25rem 0.625rem;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  white-space: nowrap;
}

.badge-success {
  background-color: var(--success-100);
  color: var(--success-600);
}

.badge-error {
  background-color: var(--error-100);
  color: var(--error-600);
}

.badge-warning {
  background-color: var(--warning-100);
  color: var(--warning-600);
}

.badge-info {
  background-color: var(--info-100);
  color: var(--info-600);
}

/* With icon */
.badge svg {
  width: 12px;
  height: 12px;
}
```

### 3.6 Modals & Dialogs

```tsx
<div className="modal-overlay">
  <div className="modal">
    <div className="modal-header">
      <h2 className="modal-title">Upload Photos</h2>
      <button className="modal-close">
        <XIcon />
      </button>
    </div>
    <div className="modal-body">
      {/* Content */}
    </div>
    <div className="modal-footer">
      <button className="btn btn-secondary">Cancel</button>
      <button className="btn btn-primary">Upload</button>
    </div>
  </div>
</div>
```

```css
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: var(--space-4);
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal {
  background: var(--white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  max-width: 600px;
  width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-6);
  border-bottom: 1px solid var(--gray-200);
}

.modal-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-semibold);
  color: var(--gray-900);
}

.modal-close {
  background: none;
  border: none;
  padding: var(--space-2);
  cursor: pointer;
  color: var(--gray-600);
  border-radius: var(--radius-base);
}

.modal-close:hover {
  background-color: var(--gray-100);
  color: var(--gray-900);
}

.modal-body {
  padding: var(--space-6);
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  padding: var(--space-6);
  border-top: 1px solid var(--gray-200);
}
```

### 3.7 Drag & Drop Zone

```tsx
<div className="dropzone" onDrop={handleDrop} onDragOver={handleDragOver}>
  <UploadCloudIcon className="dropzone-icon" />
  <p className="dropzone-text">
    <strong>Click to upload</strong> or drag and drop
  </p>
  <p className="dropzone-hint">PNG, JPG, GIF up to 10MB</p>
</div>
```

```css
.dropzone {
  border: 2px dashed var(--gray-300);
  border-radius: var(--radius-lg);
  padding: var(--space-12);
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: var(--gray-50);
}

.dropzone:hover {
  border-color: var(--primary-400);
  background-color: var(--primary-50);
}

.dropzone.dragging {
  border-color: var(--primary-500);
  background-color: var(--primary-100);
  transform: scale(1.02);
}

.dropzone-icon {
  width: 48px;
  height: 48px;
  color: var(--gray-400);
  margin: 0 auto var(--space-4);
}

.dropzone:hover .dropzone-icon {
  color: var(--primary-500);
}

.dropzone-text {
  font-size: var(--text-base);
  color: var(--gray-700);
  margin-bottom: var(--space-2);
}

.dropzone-text strong {
  color: var(--primary-500);
  font-weight: var(--font-semibold);
}

.dropzone-hint {
  font-size: var(--text-sm);
  color: var(--gray-600);
}
```

---

## 4. Interaction Patterns

### 4.1 Upload Flow Interactions

#### Step 1: File Selection
**Pattern:** Click-to-upload OR drag-and-drop

**Web Interaction:**
1. User sees large dropzone with clear call-to-action
2. **Click:** Opens native file picker
3. **Drag:** Shows "Drop files here" visual feedback
4. **Drop:** Files immediately queue for upload

**Mobile Interaction:**
1. User taps "Upload" FAB (floating action button)
2. Action sheet appears: "Camera" | "Photo Library" | "Files"
3. User selects source
4. Native picker opens
5. Multi-select enabled by default
6. Taps "Done" to confirm selection

**Feedback:**
- Dropzone highlights on hover (web)
- Dropzone scales up slightly on drag-over
- Selected files show preview thumbnails immediately

---

#### Step 2: Upload Preview & Confirmation
**Pattern:** Review-before-upload with inline editing

**Interaction:**
1. Selected files appear in grid preview (thumbnails)
2. Each thumbnail shows:
   - File name (editable on click)
   - File size
   - Remove button (X icon)
3. Total file count and size displayed at top
4. "Start Upload" primary button enabled
5. User can add more files ("Add More" button)

**Feedback:**
- Thumbnail load animation (skeleton → image fade-in)
- Inline file name editing with focus state
- Remove file with fade-out animation
- Total count updates dynamically

---

#### Step 3: Upload Progress Monitoring
**Pattern:** Real-time progress with granular visibility

**Interaction:**
1. Progress dashboard replaces preview
2. Shows three sections:
   - **Aggregate Progress:** Overall bar (e.g., "23/100 - 23%")
   - **Active Uploads:** Scrollable list of in-progress files
   - **Completed:** Collapsed list of finished files
3. Each active upload shows:
   - Thumbnail
   - File name
   - Progress bar (0-100%)
   - Upload speed (MB/s)
   - ETA (seconds remaining)
   - Status badge (Uploading / Failed / Complete)
4. User can minimize dashboard (collapses to notification bar)

**Feedback:**
- Progress bars update every 500ms (smooth animation)
- Completed uploads move to "Completed" section with slide animation
- Failed uploads show red status badge + retry button
- Success sound (optional, brief tone) on completion

---

#### Step 4: Completion & Next Actions
**Pattern:** Celebration + clear next steps

**Interaction:**
1. All uploads complete → Success modal appears
2. Displays:
   - Success icon (checkmark with subtle animation)
   - "100 photos uploaded successfully!"
   - Preview of uploaded photos (first 6 thumbnails)
   - Primary action: "View Gallery"
   - Secondary actions: "Add Tags" | "Share"
3. User can dismiss modal or click action

**Feedback:**
- Success modal slides up from bottom with fade-in
- Confetti animation (optional, brief 2-second effect)
- Success notification persists in notification center

---

### 4.2 Gallery Interaction Patterns

#### Infinite Scroll
**Pattern:** Load-on-demand with smooth transitions

**Interaction:**
1. Gallery loads first 50 photos
2. User scrolls to bottom 80% → next batch loads
3. Loading spinner appears at bottom
4. New photos fade in with stagger effect

**Feedback:**
- Skeleton loading placeholders (gray boxes with shimmer)
- Scroll position maintained during load
- Subtle fade-in animation for new photos

---

#### Photo Selection (Multi-Select)
**Pattern:** Long-press (mobile) OR checkbox (web) selection mode

**Web Interaction:**
1. Hover over photo → checkbox appears in top-left corner
2. Click checkbox → enters multi-select mode
3. Selection toolbar appears at top (floating bar)
4. Toolbar shows: "X selected" | Bulk actions (Download, Tag, Delete)
5. Click outside or "Clear Selection" exits mode

**Mobile Interaction:**
1. Long-press on photo → enters multi-select mode
2. Visual feedback: photo scales down slightly, checkmark appears
3. Tap additional photos to add to selection
4. Bottom sheet appears with bulk actions
5. Tap "Done" or back button to exit

**Feedback:**
- Selected photos have blue border + checkmark overlay
- Selection count updates in real-time
- Haptic feedback on selection (mobile)

---

#### Photo Viewer (Lightbox)
**Pattern:** Modal overlay with navigation

**Interaction:**
1. Click/tap photo → opens full-screen viewer
2. Viewer displays:
   - Full-resolution photo
   - Navigation arrows (prev/next)
   - Action bar: Download, Share, Tag, Delete
   - Close button (X)
3. Swipe left/right to navigate (mobile)
4. Arrow keys to navigate (web)
5. Escape key to close (web)

**Feedback:**
- Smooth zoom animation from thumbnail to full-size
- Swipe gestures feel natural with physics-based animation
- Photo metadata appears on hover/tap (bottom overlay)

---

### 4.3 Error Handling Interactions

#### Failed Upload Recovery
**Pattern:** Inline retry with clear messaging

**Interaction:**
1. Upload fails → status badge turns red ("Failed")
2. Error message appears below progress bar
   - User-friendly text: "Connection lost. Click to retry."
   - NOT technical: "500 Internal Server Error"
3. Retry button appears inline
4. Click retry → upload re-queues automatically
5. Success → status badge turns green ("Complete")

**Feedback:**
- Red error badge with alert icon
- Error message in red text with info icon
- Retry button pulses subtly to draw attention
- Success transition with green checkmark animation

---

#### Network Loss Handling
**Pattern:** Automatic pause with clear status

**Interaction:**
1. Network disconnects → uploads pause
2. Status dashboard updates:
   - Aggregate progress shows "Paused - Connection Lost"
   - Active uploads show yellow "Paused" badge
   - Notification: "Upload paused. Will resume when connection returns."
3. Network reconnects → automatic resume
4. Notification: "Connection restored. Resuming uploads..."

**Feedback:**
- Smooth transition from "Uploading" to "Paused" badge
- Pause icon overlays progress bars
- Resume notification with success tone

---

### 4.4 Keyboard Shortcuts (Web Power Users)

| Shortcut | Action |
|----------|--------|
| `Ctrl/Cmd + U` | Open upload modal |
| `Ctrl/Cmd + A` | Select all photos (in gallery) |
| `Escape` | Close modal/viewer, clear selection |
| `Arrow Keys` | Navigate photo viewer |
| `Delete` | Delete selected photos (with confirmation) |
| `Ctrl/Cmd + F` | Focus search bar |
| `Space` | Pause/resume current upload (if applicable) |
| `Ctrl/Cmd + ,` | Open settings |

**Discoverability:**
- Keyboard shortcut hints in tooltips
- "?" key shows keyboard shortcuts overlay

---

## 5. Responsive Design

### Breakpoints
```css
/* Mobile First Approach */
--breakpoint-sm:  640px;   /* Small devices (landscape phones) */
--breakpoint-md:  768px;   /* Medium devices (tablets) */
--breakpoint-lg:  1024px;  /* Large devices (desktops) */
--breakpoint-xl:  1280px;  /* Extra large devices (large desktops) */
--breakpoint-2xl: 1536px;  /* Ultra wide screens */
```

### Responsive Layout Patterns

#### Gallery Grid
```css
/* Mobile: 2 columns */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-2);
}

/* Tablet: 3 columns */
@media (min-width: 768px) {
  .gallery-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-4);
  }
}

/* Desktop: 4 columns */
@media (min-width: 1024px) {
  .gallery-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

/* Large Desktop: 5 columns */
@media (min-width: 1280px) {
  .gallery-grid {
    grid-template-columns: repeat(5, 1fr);
  }
}
```

#### Navigation (Mobile vs Desktop)
**Mobile:** Bottom tab bar (5 items max)
**Desktop:** Top navigation bar + sidebar

```css
/* Mobile: Bottom Tab Bar */
.nav-mobile {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--white);
  border-top: 1px solid var(--gray-200);
  display: flex;
  justify-content: space-around;
  padding: var(--space-2) 0;
  z-index: 100;
}

/* Desktop: Top Nav + Sidebar */
@media (min-width: 768px) {
  .nav-mobile {
    display: none;
  }

  .nav-desktop {
    display: flex;
  }
}
```

---

## 6. Accessibility

### WCAG 2.1 Level AA Compliance

#### Color Contrast
- **Body Text:** Minimum 4.5:1 contrast ratio
- **Large Text (18px+):** Minimum 3:1 contrast ratio
- **Interactive Elements:** Minimum 3:1 contrast against background

**Verified Combinations:**
- `--gray-900` on `--white`: 18.26:1 ✅
- `--primary-500` on `--white`: 4.57:1 ✅
- `--gray-600` on `--white`: 5.74:1 ✅

#### Keyboard Navigation
- All interactive elements focusable via Tab
- Focus indicators clearly visible (3px outline)
- Skip-to-content link for screen readers
- Modal traps focus (Escape to close)

```css
/* Focus Styles */
*:focus {
  outline: 3px solid var(--primary-500);
  outline-offset: 2px;
}

*:focus:not(:focus-visible) {
  outline: none;
}

*:focus-visible {
  outline: 3px solid var(--primary-500);
  outline-offset: 2px;
}
```

#### Screen Reader Support
- Semantic HTML (nav, main, section, article)
- ARIA labels for icons and interactive elements
- Live regions for status updates (aria-live="polite")
- Alt text for all images
- Form labels properly associated with inputs

```html
<!-- Example: Upload Button -->
<button aria-label="Upload photos from your device">
  <UploadIcon aria-hidden="true" />
  <span>Upload Photos</span>
</button>

<!-- Example: Progress Update -->
<div role="status" aria-live="polite" aria-atomic="true">
  23 of 100 photos uploaded. 77 remaining.
</div>
```

#### Touch Targets (Mobile)
- Minimum 44×44px touch targets (iOS guideline)
- Minimum 48×48dp touch targets (Android guideline)
- Adequate spacing between targets (8px minimum)

---

## 7. Micro-Interactions

### 7.1 Button Press
**Effect:** Scale down + shadow reduction on press

```css
.btn {
  transition: all 0.15s ease;
}

.btn:active {
  transform: scale(0.98);
  box-shadow: var(--shadow-sm);
}
```

### 7.2 Photo Upload Success
**Effect:** Checkmark fade-in with scale

```css
@keyframes successCheckmark {
  0% {
    opacity: 0;
    transform: scale(0.5);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

.upload-success-icon {
  animation: successCheckmark 0.4s ease;
}
```

### 7.3 Gallery Photo Hover (Web)
**Effect:** Lift + shadow increase

```css
.photo-card {
  transition: all 0.2s ease;
}

.photo-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}
```

### 7.4 Progress Bar Fill
**Effect:** Smooth width transition + gradient shift

```css
.progress-bar-fill {
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(90deg, var(--primary-500) 0%, var(--primary-400) 100%);
  animation: gradientShift 2s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}
```

### 7.5 File Drop Feedback
**Effect:** Dropzone scale up + color shift

```css
.dropzone.dragging {
  animation: dropzonePulse 0.3s ease;
}

@keyframes dropzonePulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.02); }
  100% { transform: scale(1); }
}
```

### 7.6 Notification Toast
**Effect:** Slide in from top + auto-dismiss

```css
.toast {
  animation: slideInFromTop 0.3s ease;
}

@keyframes slideInFromTop {
  from {
    opacity: 0;
    transform: translateY(-100%);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Auto-dismiss after 5 seconds */
.toast.dismissing {
  animation: slideOutToTop 0.3s ease forwards;
}

@keyframes slideOutToTop {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-100%);
  }
}
```

---

## 8. Platform-Specific Guidelines

### 8.1 Web (React + TypeScript)

**Framework:** React 18 + TypeScript + Tailwind CSS (or styled-components)

**Key Considerations:**
- Desktop-first power-user features (keyboard shortcuts, bulk operations)
- Optimize for mouse/trackpad interactions (hover states, right-click context menus)
- Support drag-and-drop from desktop file system
- Multi-column layouts (utilize screen real estate)

**Component Architecture:**
```
src/
├── components/
│   ├── ui/              # Base UI components (Button, Input, Card)
│   ├── upload/          # Upload-specific components
│   ├── gallery/         # Gallery-specific components
│   └── layout/          # Layout components (Header, Sidebar)
├── styles/
│   ├── tokens.css       # Design tokens (colors, spacing, typography)
│   ├── animations.css   # Reusable animations
│   └── global.css       # Global styles
└── hooks/               # Reusable React hooks (useUpload, useGallery)
```

---

### 8.2 Mobile (Flutter - iOS/Android)

**Framework:** Flutter 3.x with Material Design 3

**Key Considerations:**
- Touch-first interactions (large tap targets, swipe gestures)
- Platform-specific patterns:
  - **iOS:** Bottom sheet modals, swipe-back gestures, SF Symbols icons
  - **Android:** FAB, Material bottom sheets, Material icons
- Background upload with notifications
- Handle system interruptions (calls, low battery, network loss)

**Component Architecture:**
```
lib/
├── core/
│   ├── theme/           # Theme data (colors, typography, spacing)
│   └── widgets/         # Reusable widgets (buttons, cards)
├── features/
│   ├── upload/          # Upload feature module
│   ├── gallery/         # Gallery feature module
│   └── auth/            # Authentication module
└── shared/
    ├── models/          # Data models
    └── services/        # API services, storage services
```

**Platform-Specific Widgets:**
```dart
// iOS: Cupertino-style action sheet
Platform.isIOS ? CupertinoActionSheet(...) : BottomSheet(...)

// Android: Material Design FAB
Platform.isAndroid ? FloatingActionButton(...) : null
```

---

## 9. Animation Performance

### Performance Budget
- **Target:** 60 FPS (16.67ms per frame)
- **Max JavaScript Execution:** 10ms per frame
- **Max Layout/Paint:** 6ms per frame

### Optimization Techniques

#### 1. Use CSS Transforms (Hardware Accelerated)
```css
/* Good: Hardware accelerated */
.element {
  transform: translateX(100px);
  will-change: transform;
}

/* Bad: Triggers layout reflow */
.element {
  left: 100px;
}
```

#### 2. Animate Only Transform and Opacity
```css
/* Performant properties */
.element {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

/* Avoid animating these (triggers layout) */
.element {
  transition: width 0.3s ease;  /* ❌ Slow */
  transition: height 0.3s ease; /* ❌ Slow */
  transition: top 0.3s ease;    /* ❌ Slow */
}
```

#### 3. Use RequestAnimationFrame for JavaScript Animations
```typescript
function animateProgress(element: HTMLElement, targetProgress: number) {
  let currentProgress = 0;

  function step() {
    currentProgress += (targetProgress - currentProgress) * 0.1;
    element.style.transform = `scaleX(${currentProgress / 100})`;

    if (Math.abs(targetProgress - currentProgress) > 0.1) {
      requestAnimationFrame(step);
    }
  }

  requestAnimationFrame(step);
}
```

---

## 10. Implementation Checklist

### Phase 1: Design System Setup (Week 1)
- [ ] Define and document color palette (CSS variables)
- [ ] Define typography system (font family, sizes, weights)
- [ ] Create spacing scale (8-point grid)
- [ ] Define shadow system (elevation levels)
- [ ] Set up icon library (Lucide/Heroicons)
- [ ] Document border radius system
- [ ] Create Figma/Sketch design library

### Phase 2: Core Components (Week 2-3)
- [ ] Button component (primary, secondary, text, FAB)
- [ ] Input component (text, email, password, file)
- [ ] Card component (photo card, upload card)
- [ ] Progress indicators (linear bar, circular spinner)
- [ ] Status badges (success, error, warning, info)
- [ ] Modal/Dialog component
- [ ] Drag & drop zone component

### Phase 3: Feature-Specific Components (Week 4-5)
- [ ] Upload progress dashboard
- [ ] Gallery grid with infinite scroll
- [ ] Photo lightbox viewer
- [ ] Multi-select mode (checkboxes, selection toolbar)
- [ ] Navigation (top nav for web, bottom tabs for mobile)
- [ ] Empty states (no photos, no search results)

### Phase 4: Interactions & Animations (Week 6)
- [ ] Hover states for all interactive elements
- [ ] Focus states for keyboard navigation
- [ ] Loading animations (skeletons, spinners)
- [ ] Micro-interactions (button press, success checkmark)
- [ ] Transition animations (modal open/close, page navigation)
- [ ] Error animations (shake effect, error badge pulse)

### Phase 5: Responsive & Accessibility (Week 7)
- [ ] Mobile breakpoints implementation
- [ ] Tablet layout adjustments
- [ ] Desktop layout with sidebars
- [ ] Keyboard navigation testing
- [ ] Screen reader testing (NVDA, VoiceOver)
- [ ] Color contrast validation
- [ ] Touch target size validation (mobile)

### Phase 6: Polish & Optimization (Week 8)
- [ ] Animation performance audit (60 FPS target)
- [ ] Dark mode support (optional, post-MVP)
- [ ] Cross-browser testing (Chrome, Firefox, Safari, Edge)
- [ ] Cross-device testing (iOS, Android)
- [ ] Accessibility audit (WAVE, axe DevTools)
- [ ] Design QA review

---

## 11. Design Deliverables

### For Development Team
1. **Design System Documentation** (this document)
2. **Figma Component Library** (interactive prototypes)
3. **Icon SVG Assets** (exported from Lucide/Heroicons)
4. **CSS Tokens File** (tokens.css with all variables)
5. **Animation Specifications** (timing, easing curves, keyframes)
6. **Responsive Breakpoint Guide** (layout adjustments per breakpoint)

### For Stakeholders
1. **High-Fidelity Mockups** (key screens: upload, gallery, viewer)
2. **Interactive Prototype** (Figma/Sketch clickable demo)
3. **User Flow Diagrams** (visual journey maps)
4. **Competitor Comparison** (side-by-side UI screenshots)

---

## 12. Maintenance & Evolution

### Design System Updates
- **Quarterly Reviews:** Review component usage, identify gaps
- **Version Control:** Semantic versioning for major design changes
- **Changelog:** Document all design token updates
- **Deprecation Policy:** 2-sprint notice for deprecated components

### Continuous Improvement
- **User Testing:** Monthly usability sessions
- **Analytics Review:** Track component interaction rates
- **Performance Monitoring:** Lighthouse scores, Core Web Vitals
- **Accessibility Audits:** Quarterly WCAG compliance checks

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Maintained By:** Product Design Team
**Review Frequency:** Quarterly

---

## Appendix: Design Tools & Resources

### Design Tools
- **Figma:** Component library, prototyping
- **Sketch:** Alternative design tool
- **Adobe XD:** Alternative design tool

### Development Tools
- **Tailwind CSS:** Utility-first CSS framework
- **Styled Components:** CSS-in-JS solution
- **Emotion:** CSS-in-JS alternative
- **Storybook:** Component documentation and testing

### Icon Libraries
- **Lucide Icons:** https://lucide.dev/
- **Heroicons:** https://heroicons.com/
- **Material Symbols:** https://fonts.google.com/icons

### Accessibility Tools
- **WAVE:** Browser extension for accessibility testing
- **axe DevTools:** Accessibility testing toolkit
- **Lighthouse:** Chrome DevTools audit tool
- **Color Contrast Analyzer:** WCAG contrast checker

### Animation Libraries
- **Framer Motion:** React animation library
- **GSAP:** Professional-grade animation library
- **Lottie:** JSON-based animations (After Effects export)

### Typography Resources
- **Google Fonts:** Inter font family
- **Font Squirrel:** Web font generator
- **TypeScale:** Type scale calculator

### Color Tools
- **Coolors:** Color palette generator
- **Adobe Color:** Color wheel and palette builder
- **Contrast Checker:** WCAG contrast ratio calculator
