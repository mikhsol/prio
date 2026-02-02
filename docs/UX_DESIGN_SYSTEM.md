# Jeeves - UX Design System & Guidelines

## Design Philosophy

### Core Principles

#### 1. Invisible Design
The best interface is one you don't notice. Jeeves should feel like a natural extension of thought—minimal friction, maximum utility.

#### 2. Conversational First
Voice and natural language are the primary interaction modes. Visual UI supports and enhances the conversation, never replaces it.

#### 3. Anticipatory UX
Design for what users will need next. Proactive suggestions and intelligent defaults reduce cognitive load.

#### 4. Trust Through Transparency
Privacy and security should be visible and understandable. Users should always know what Jeeves knows about them.

#### 5. Delightful Details
Micro-interactions, subtle animations, and personality in responses create emotional connection without sacrificing efficiency.

---

## User Research Insights

### Key Findings from User Research

#### Pain Points
1. **Information Overload**: Users check 5+ apps daily for tasks, calendar, notes
2. **Friction in Input**: Typing on mobile is slow; voice is underutilized
3. **Forgotten Context**: Assistants don't remember previous conversations
4. **Privacy Anxiety**: Users want AI help but fear data misuse
5. **Notification Fatigue**: Too many alerts lead to ignoring all alerts

#### User Needs
1. Quick capture of thoughts and tasks
2. Intelligent scheduling without manual input
3. Personalized, context-aware responses
4. Clear visibility into what the assistant knows
5. Seamless integration with existing tools

### Personas

#### Persona 1: Alex (Primary)
- **Role**: Product Manager, 34
- **Quote**: "I need an assistant that thinks ahead so I don't have to"
- **Goals**: 
  - Never miss important meetings
  - Quick task capture during walks
  - Daily preparation without effort
- **Frustrations**:
  - Siri doesn't understand context
  - Too many apps to check
  - Constantly context-switching

#### Persona 2: Maya (Secondary)
- **Role**: Working Parent, 41
- **Quote**: "I'm the family calendar, and I need backup"
- **Goals**:
  - Coordinate family schedules
  - Remember school events and activities
  - Reduce mental load
- **Frustrations**:
  - Shared calendars are clunky
  - Reminders come at wrong times
  - Kids' schedules constantly change

---

## Information Architecture

### App Structure

```
Jeeves App
├── Home (Conversation)
│   ├── Voice Input
│   ├── Text Input
│   ├── Suggestions
│   └── Conversation History
├── Today
│   ├── Schedule Timeline
│   ├── Tasks Due Today
│   ├── Briefing Summary
│   └── Quick Actions
├── Tasks
│   ├── Inbox
│   ├── Today
│   ├── Upcoming
│   ├── Projects
│   └── Completed
├── Calendar
│   ├── Day View
│   ├── Week View
│   ├── Month View
│   └── Event Details
├── Notes
│   ├── Quick Notes
│   ├── Voice Memos
│   └── Organized Notes
└── Settings
    ├── Profile
    ├── Preferences
    ├── Integrations
    ├── Privacy
    ├── Notifications
    └── Subscription
```

### Navigation Patterns

#### Primary Navigation
- **Tab Bar** (iOS) / **Bottom Navigation** (Android)
- 4 main tabs: Home, Today, Tasks, More
- Floating Action Button for quick capture

#### Secondary Navigation
- Contextual menus
- Bottom sheets for actions
- Swipe gestures for quick actions
- Long-press for power features

---

## Conversational UX

### Conversation Flow Principles

#### 1. Progressive Disclosure
Start with simple responses, offer details on request
```
User: "What's my day look like?"
Jeeves: "You have 3 meetings today, starting with your 9am standup. 
        Want me to walk through each one?"
```

#### 2. Contextual Memory
Remember and reference previous context
```
User: "Remind me about the thing with Sarah"
Jeeves: "Got it. I'll remind you about the Q3 budget review 
        with Sarah tomorrow at 9am. Sound good?"
```

#### 3. Graceful Failure
Handle uncertainty with options, not errors
```
User: "Schedule lunch with John"
Jeeves: "I found 3 Johns in your contacts. Which one?
        • John Smith (Work)
        • John Doe (Family)
        • John Wilson (Gym)"
```

#### 4. Confirmation Without Friction
Confirm critical actions, streamline routine ones
```
// Low stakes - no confirmation needed
User: "Add milk to my shopping list"
Jeeves: "Added milk to Shopping List ✓"

// High stakes - confirm
User: "Cancel my 2pm meeting"
Jeeves: "Cancel 'Client Review' at 2pm with 4 attendees?
        I'll notify them of the cancellation."
        [Cancel Meeting] [Keep It]
```

### Voice Interaction Design

#### Wake Word
- "Hey Jeeves" or customizable
- Visual indicator when listening
- Haptic feedback on activation

#### Voice Feedback
- Concise spoken responses
- Visual text backup
- Non-disruptive audio levels

#### Error Recovery
```
Jeeves: "I didn't catch that. Could you try again?"
[Shows what was heard with option to edit]
```

### Response Personality

#### Tone Guidelines
- **Helpful**: Always prioritize being useful
- **Concise**: Get to the point, offer more if needed
- **Warm**: Friendly but not overly casual
- **Intelligent**: Show competence through accuracy

#### Examples

| Context | Too Casual | Too Formal | Just Right |
|---------|------------|------------|------------|
| Task complete | "Yay! Done!" | "Task completion confirmed." | "Done! ✓" |
| Error | "Oops my bad" | "An error has occurred." | "Hmm, that didn't work. Let me try again." |
| Greeting | "Hey hey!" | "Good morning." | "Good morning, Alex. Ready for your 9am?" |

---

## Visual Design System

### Color Palette

#### Primary Colors
```css
--color-primary: #0D9488;        /* Teal - main brand */
--color-primary-light: #14B8A6;  /* Hover/active states */
--color-primary-dark: #0F766E;   /* Pressed states */
```

#### Secondary Colors
```css
--color-accent: #F59E0B;         /* Amber - CTAs, highlights */
--color-accent-light: #FBBF24;
--color-accent-dark: #D97706;
```

#### Semantic Colors
```css
--color-success: #10B981;        /* Completed, confirmed */
--color-warning: #F59E0B;        /* Attention needed */
--color-error: #EF4444;          /* Errors, destructive */
--color-info: #3B82F6;           /* Informational */
```

#### Neutral Palette
```css
/* Light Mode */
--color-bg-primary: #FFFFFF;
--color-bg-secondary: #F9FAFB;
--color-bg-tertiary: #F3F4F6;
--color-text-primary: #111827;
--color-text-secondary: #6B7280;
--color-text-tertiary: #9CA3AF;
--color-border: #E5E7EB;

/* Dark Mode */
--color-bg-primary-dark: #111827;
--color-bg-secondary-dark: #1F2937;
--color-bg-tertiary-dark: #374151;
--color-text-primary-dark: #F9FAFB;
--color-text-secondary-dark: #9CA3AF;
--color-text-tertiary-dark: #6B7280;
--color-border-dark: #374151;
```

### Typography

#### Font Family
- **iOS**: SF Pro (system)
- **Android**: Roboto (system)
- **Web**: Inter

#### Type Scale
```css
--font-xs: 12px;     /* Captions, labels */
--font-sm: 14px;     /* Secondary text */
--font-base: 16px;   /* Body text */
--font-lg: 18px;     /* Subtitles */
--font-xl: 20px;     /* Section headers */
--font-2xl: 24px;    /* Page titles */
--font-3xl: 30px;    /* Large titles */
```

#### Font Weights
```css
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;
```

### Spacing System

```css
--space-1: 4px;
--space-2: 8px;
--space-3: 12px;
--space-4: 16px;
--space-5: 20px;
--space-6: 24px;
--space-8: 32px;
--space-10: 40px;
--space-12: 48px;
--space-16: 64px;
```

### Border Radius

```css
--radius-sm: 4px;    /* Buttons, inputs */
--radius-md: 8px;    /* Cards, containers */
--radius-lg: 12px;   /* Modals, sheets */
--radius-xl: 16px;   /* Large cards */
--radius-full: 9999px; /* Pills, avatars */
```

### Elevation (Shadows)

```css
/* Light mode */
--shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
--shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1);
--shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1);
--shadow-xl: 0 20px 25px rgba(0, 0, 0, 0.15);

/* Dark mode - use lighter shadows */
--shadow-sm-dark: 0 1px 2px rgba(0, 0, 0, 0.2);
--shadow-md-dark: 0 4px 6px rgba(0, 0, 0, 0.3);
```

---

## Component Library

### Buttons

#### Primary Button
- Background: Primary color
- Text: White
- Height: 48px (mobile), 44px (web)
- Border radius: 8px
- Font: 16px, semibold

#### Secondary Button
- Background: Transparent
- Border: 1px primary color
- Text: Primary color

#### Ghost Button
- Background: Transparent
- Text: Primary color
- Used for less prominent actions

#### Icon Button
- 44x44px touch target minimum
- 24px icon
- Optional background circle

### Input Fields

#### Text Input
- Height: 48px
- Border: 1px border color
- Focus: Primary color border, subtle shadow
- Placeholder: Tertiary text color
- Clear button on content

#### Voice Input
- Large microphone button (64x64px)
- Pulsing animation when listening
- Waveform visualization
- Transcript preview

### Cards

#### Task Card
```
┌────────────────────────────────────┐
│ ○ Task title                   ⋯  │
│   Due: Tomorrow, 3pm              │
│   🏷️ Work   ⚑ High priority       │
└────────────────────────────────────┘
```

#### Event Card
```
┌────────────────────────────────────┐
│ █ 9:00 AM - 10:00 AM              │
│ │ Team Standup                    │
│ │ 📍 Zoom   👥 5 attendees        │
│ █                                 │
└────────────────────────────────────┘
```

### Bottom Sheets

- Drag handle at top (32px wide, 4px tall)
- Rounded top corners (16px)
- Max height: 90% of screen
- Backdrop: 50% black opacity
- Gesture: Drag down to dismiss

### Navigation Bar

#### iOS
- SF Symbols icons
- 5 maximum items
- Badge indicators for counts

#### Android
- Material icons
- 3-5 items
- FAB integration

---

## Interaction Patterns

### Gestures

| Gesture | Action |
|---------|--------|
| Tap | Select, toggle |
| Long press | Context menu |
| Swipe left | Delete, archive |
| Swipe right | Complete, mark read |
| Pull down | Refresh |
| Drag | Reorder |
| Pinch | Zoom (calendar) |

### Animations

#### Timing Functions
```css
--ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
--ease-out: cubic-bezier(0, 0, 0.2, 1);
--ease-in: cubic-bezier(0.4, 0, 1, 1);
--spring: cubic-bezier(0.175, 0.885, 0.32, 1.275);
```

#### Duration
- Micro (feedback): 100-150ms
- Quick (state change): 200-300ms
- Standard (navigation): 300-400ms
- Expressive (emphasis): 400-500ms

#### Animation Examples
- **Button press**: Scale 0.95, 100ms
- **Card appear**: Fade in + slide up, 300ms
- **Modal open**: Scale 0.9→1 + fade, 300ms
- **Success check**: Draw checkmark, 400ms

### Loading States

#### Skeleton Screens
- Use for content loading
- Match final layout
- Subtle shimmer animation

#### Progress Indicators
- Spinner: Quick, indeterminate operations
- Progress bar: Downloads, uploads with %
- Inline: "Processing..." text with animation

---

## Platform-Specific Guidelines

### iOS (Human Interface Guidelines)

#### Navigation
- Large titles in navigation bars
- Tab bar always visible
- Swipe from edge to go back

#### Components
- Use SF Symbols
- Native share sheets
- Standard alerts and action sheets

#### Features to Leverage
- Dynamic Type (accessibility)
- Haptic feedback (Taptic Engine)
- Face ID / Touch ID
- Widgets (WidgetKit)
- Siri Shortcuts

### Android (Material Design 3)

#### Navigation
- Material 3 bottom navigation
- Predictive back gesture
- Navigation drawer for secondary

#### Components
- Material icons
- Bottom sheets over alerts
- Snackbars for confirmations

#### Features to Leverage
- Material You theming
- Android widgets (Glance)
- Google Assistant actions
- Quick settings tile
- Dynamic shortcuts

---

## Accessibility

### WCAG 2.1 AA Compliance

#### Visual
- Minimum contrast ratio: 4.5:1 (text), 3:1 (large text)
- Support Dynamic Type / Font scaling
- No color as only indicator
- Dark mode fully functional

#### Motor
- Touch targets: 44x44px minimum
- Adequate spacing between targets
- Support for assistive devices
- Reduced motion option

#### Screen Readers
- All images have alt text
- Logical heading hierarchy
- Form labels properly associated
- Announce dynamic content changes

#### Cognitive
- Clear, simple language
- Consistent navigation
- Error prevention and recovery
- Progress saving

### Accessibility Checklist

- [ ] Color contrast passes
- [ ] Dynamic Type supported
- [ ] VoiceOver / TalkBack tested
- [ ] Reduced motion honored
- [ ] Keyboard navigation works
- [ ] Focus states visible
- [ ] Error messages helpful
- [ ] Time limits adjustable

---

## User Flows

### First Time User Experience (FTUE)

```
1. Welcome Screen
   ├── App benefits (3 slides)
   └── [Get Started]

2. Account Creation
   ├── Email/Apple/Google sign up
   └── Basic profile info

3. Permissions
   ├── Notifications (explain value)
   ├── Microphone (for voice)
   ├── Calendar (for integration)
   └── Contacts (for context)

4. Personalization
   ├── Work schedule
   ├── Preferred name
   └── Primary use cases

5. Integration Setup
   ├── Calendar connection
   └── [Skip for now]

6. First Interaction
   ├── Guided conversation
   ├── Success moment
   └── [Continue to app]
```

### Daily Briefing Flow

```
User opens app in morning
    │
    ├── Morning briefing auto-plays (if enabled)
    │   ├── Weather summary
    │   ├── Today's schedule
    │   ├── Tasks due today
    │   └── Proactive suggestions
    │
    ├── User can interrupt/skip
    │
    └── Briefing card remains for reference
```

### Task Creation Flow

```
Voice: "Hey Jeeves, remind me to call mom tomorrow at 2pm"
    │
    ├── Parse intent: reminder, contact, time
    │
    ├── Create task with details
    │
    ├── Confirm: "I'll remind you to call Mom 
    │            tomorrow at 2pm ✓"
    │
    └── Show undo option (3 seconds)
```

---

## Design Handoff

### Figma Structure

```
Jeeves Design System/
├── 🎨 Foundations/
│   ├── Colors
│   ├── Typography
│   ├── Spacing
│   ├── Icons
│   └── Illustrations
├── 🧩 Components/
│   ├── Atoms/
│   ├── Molecules/
│   └── Organisms/
├── 📱 Screens/
│   ├── iOS/
│   ├── Android/
│   └── Web/
├── 🎬 Prototypes/
│   └── Interactive flows
└── 📋 Documentation/
    └── Specs and notes
```

### Component Naming Convention

```
[Platform]/[Category]/[Component]/[Variant]/[State]

Examples:
iOS/Buttons/Primary/Default/Rest
iOS/Buttons/Primary/Default/Pressed
Android/Cards/Task/WithDate/Selected
```

### Developer Handoff

1. **Components**: Export with auto-layout specs
2. **Spacing**: Use design tokens, not pixels
3. **Colors**: Reference color variables
4. **Assets**: Export @1x, @2x, @3x (iOS), mdpi-xxxhdpi (Android)
5. **Animations**: Provide Lottie files or specs

---

## Design Review Checklist

### Before Development

- [ ] All states designed (empty, loading, error, success)
- [ ] Dark mode variant complete
- [ ] Accessibility reviewed
- [ ] Responsive breakpoints defined
- [ ] Edge cases considered
- [ ] Copy reviewed and final
- [ ] Assets exported correctly
- [ ] Developer questions addressed

### After Development

- [ ] Implementation matches design
- [ ] Animations smooth and correct
- [ ] Accessibility features working
- [ ] Dark mode rendering correctly
- [ ] Edge cases handled gracefully
- [ ] Performance acceptable
- [ ] User tested if possible

---

*Document Owner: Principal UX Designer*
*Last Updated: August 2025*
*Status: Living Document*
