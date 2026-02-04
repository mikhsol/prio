# Prio - UX Design System & Guidelines

## Design Philosophy

### Core Principles

#### 1. 80/20 Focus
Design for the 20% of features that deliver 80% of value. Every element must earn its place. Remove friction from the most common actions.

#### 2. Clarity Over Complexity
Every screen has one clear purpose. Eisenhower prioritization powers the app—showing users what matters most, fast.

#### 2. Offline-First Experience
Design for no connectivity as the default. Never show loading spinners for local operations. Cloud features are graceful enhancements.

#### 3. Anticipatory UX
Design for what users will need next. AI suggestions surface at the right moment. Morning briefings prepare; evening summaries reflect.

#### 4. Modern & Minimal
Clean Android Material Design 3 aesthetics. Generous whitespace. Purposeful animations. The UI should feel premium but lightweight.

#### 5. Pluggable Extensibility  
Each feature is a self-contained module. Navigation adapts to enabled plugins. Future features integrate seamlessly.

---

## User Research Insights

### Key Findings from User Research

#### Pain Points
1. **Priority Paralysis**: Users don't know what to do first among 50+ tasks
2. **No Goal Connection**: Tasks feel disconnected from meaningful outcomes
3. **App Overload**: Switching between todo, calendar, notes, goals apps
4. **Meeting Black Holes**: Meetings end without captured action items
5. **Privacy Concerns**: Distrust of cloud-based AI assistants

#### User Needs
1. Clear visual priority system (Eisenhower is known but poorly implemented)
2. Connect daily tasks to quarterly/yearly goals
3. Single app for productivity needs
4. AI that helps without requiring internet
5. Quick capture without friction

### Personas

#### Persona 1: Alex Chen (Primary - Overwhelmed Professional)
- **Role**: Senior Product Manager, 32
- **Income**: $130K | **Device**: Pixel 8 Pro
- **Quote**: "I need something that tells me what to work on next, not another app that stores tasks"
- **Goals**: 
  - Clear priorities each morning
  - Track progress on quarterly OKRs
  - Capture meeting action items instantly
- **Pain Points**:
  - Prioritization paralysis: "50 tasks, don't know where to start"
  - Goal disconnect: quarterly objectives never translate to daily work
  - Tool fatigue: tried everything, nothing sticks
- **Willingness to Pay**: High ($10/mo)
- **Key Features**: Eisenhower AI, Meeting Action Items, Daily Briefings

#### Persona 2: Maya Rodriguez (Secondary - Privacy-Conscious Creator)
- **Role**: Freelance UX Designer, 28
- **Income**: $75K | **Device**: Samsung Galaxy S24
- **Quote**: "I'd love an AI assistant, but I'm not uploading my client list to someone's cloud"
- **Goals**:
  - Stay organized across client projects
  - Protect client confidentiality (NDAs)
  - Maintain work-life boundaries
- **Pain Points**:
  - Privacy vs functionality trade-off
  - Works all hours, forgets to stop
  - No account = no useful AI tools
- **Willingness to Pay**: Medium ($5/mo), prefers lifetime
- **Key Features**: Local-Only Mode, Project Tags, End-of-Day Nudge

#### Persona 3: Jordan Williams (Tertiary - Aspiring Achiever)
- **Role**: Junior Software Developer, 24
- **Income**: $72K | **Device**: OnePlus 12
- **Quote**: "I've read Atomic Habits three times. I just need something to make me do it."
- **Goals**:
  - Get promoted to mid-level within 18 months
  - Build healthy habits (gym, reading)
  - Feel like making progress on life goals
- **Pain Points**:
  - Goal overwhelm: wants everything, achieves nothing
  - System hopping: new productivity system monthly
  - No accountability for personal goals
- **Willingness to Pay**: Low (freemium), convert later
- **Key Features**: Goal Integration, Progress Visualization, Goal Streaks

---

## Information Architecture

### App Structure (MVP)

```
Prio App
├── Today (Home)
│   ├── Morning/Evening Briefing Card
│   ├── Eisenhower Quick View (2x2 counts)
│   ├── Today's Top 3 Priorities
│   ├── Calendar Timeline
│   └── Goal Progress Highlights
│
├── Tasks
│   ├── List View (default - prioritized list with quadrant badges)
│   ├── Focus View (Q1 + top Q2 only)
│   ├── Matrix View (optional 2x2 grid)
│   │   ├── Q1: Do Now (Urgent + Important)
│   │   ├── Q2: Schedule (Important, Not Urgent)  
│   │   ├── Q3: Delegate (Urgent, Not Important)
│   │   └── Q4: Drop/Eliminate (Neither)
│   ├── Inbox (uncategorized)
│   └── Completed
│
├── Goals
│   ├── Active Goals (cards with progress)
│   ├── Goal Detail
│   │   ├── Progress Chart
│   │   ├── Linked Tasks
│   │   ├── Milestones
│   │   └── Analytics
│   ├── Add/Edit Goal
│   └── Completed Goals
│
├── Calendar
│   ├── Day View with Tasks
│   ├── Week View
│   ├── Meeting Detail
│   │   ├── Agenda/Checklist
│   │   ├── Notes
│   │   └── Action Items
│   └── Briefings History
│
├── Insights (Analytics)
│   ├── Productivity Dashboard
│   ├── Task Completion Trends
│   ├── Goal Progress Charts
│   ├── Missed Deadlines Analysis
│   └── Weekly/Monthly Reports
│
└── Settings
    ├── Profile
    ├── AI & Model Settings
    ├── Notifications
    ├── Eisenhower Defaults
    ├── Plugins (enabled features)
    ├── Sync & Backup
    └── Privacy
```

### Navigation Patterns

#### Primary Navigation
- **Bottom Navigation Bar** (Material 3)
- 4 main tabs: Today, Tasks, Goals, More
- FAB for quick task capture (always visible)
- Badge indicators for overdue/urgent items

#### Secondary Navigation
- Top app bar with contextual actions
- Bottom sheets for quick actions
- Swipe gestures for task actions
- Long-press for power features
- Pull-to-refresh where applicable

---

## Task Views UX (Eisenhower-Powered Prioritization)

The Eisenhower prioritization engine automatically categorizes tasks into four quadrants behind the scenes. Users can choose their preferred view while benefiting from AI-driven priority suggestions.

### 1. List View (Default)

```
┌─────────────────────────────────────────────────────────────────┐
│                    TODAY'S TASKS                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  🔴 DO FIRST (3)                                    [Collapse]  │
│  ├─ ✅ Call dentist about appointment          Due: 2pm        │
│  ├─ ⬜ Submit quarterly report                 Due: 5pm        │
│  └─ ⬜ Review team's PRs                       Due: EOD        │
│                                                                  │
│  🟡 SCHEDULE (5)                                    [Collapse]  │
│  ├─ ⬜ Research vacation destinations          This week       │
│  ├─ ⬜ Start online course module 3            By Friday       │
│  └─ ... +3 more                                                 │
│                                                                  │
│  🟠 CONSIDER DELEGATING (2)                         [Collapse]  │
│  └─ ⬜ Organize team lunch                     Next week       │
│                                                                  │
│  ⚪ MAYBE LATER (4)                                 [Collapse]  │
│  └─ ⬜ Reorganize bookshelf                    No due date     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Features:**
- Grouped by Eisenhower priority (collapsible sections)
- Color-coded priority indicators
- Focus on "Do First" tasks prominently
- Shows task counts per category
- Swipe actions (complete, reschedule, snooze)

### 2. Focus View (Minimal)

```
┌─────────────────────────────────────────────────────────────────┐
│                    FOCUS MODE                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    Your next task:                               │
│                                                                  │
│                 ┌────────────────────┐                          │
│                 │                    │                          │
│                 │  Submit quarterly  │                          │
│                 │      report        │                          │
│                 │                    │                          │
│                 │    Due: 5:00 PM    │                          │
│                 │    ⏱️ ~45 min      │                          │
│                 │                    │                          │
│                 └────────────────────┘                          │
│                                                                  │
│            [✓ Done]    [⏰ Snooze]    [→ Skip]                  │
│                                                                  │
│            2 more urgent tasks after this                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Features:**
- Shows one task at a time
- Eliminates decision fatigue
- Clear action buttons
- Minimal distractions

### 3. Matrix View (Optional - Classic Eisenhower)

```
┌─────────────────────────────────────────────────────────────────┐
│                    MATRIX VIEW                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    URGENT          NOT URGENT                    │
│               ┌──────────────┬──────────────┐                   │
│               │              │              │                   │
│   IMPORTANT   │   🔴 DO      │   🟡 SCHEDULE │                   │
│               │   FIRST      │              │                   │
│               │   (3 tasks)  │   (7 tasks)  │                   │
│               │              │              │                   │
│               ├──────────────┼──────────────┤                   │
│               │              │              │                   │
│   NOT         │   🟠 DELEGATE│   ⚪ LATER   │                   │
│   IMPORTANT   │              │              │                   │
│               │   (2 tasks)  │   (5 tasks)  │                   │
│               │              │              │                   │
│               └──────────────┴──────────────┘                   │
│                                                                  │
│   [Tap quadrant to expand • Drag tasks between quadrants]       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Features:**
- Classic 2x2 grid for visual learners
- Drag-and-drop between quadrants
- Tap to expand quadrant list
- Good for weekly reviews

### View Switching

```
┌─────────────────────────────────────────────────────────────────┐
│  📋 List  │  🎯 Focus  │  ⊞ Matrix  │  📅 Calendar             │
└─────────────────────────────────────────────────────────────────┘
```

Users can switch views via bottom tab or settings. Default: List View (80/20: most users prefer lists).

### Quadrant Interaction (All Views)

#### Expanded Quadrant View
- Full task list for selected quadrant/priority
- Swipe actions (complete, reschedule, delegate)
- Drag to reorder within quadrant
- Tap task for detail sheet

### Task Quick Capture

```
┌─────────────────────────────────────────────────────────────────┐
│                    QUICK CAPTURE (FAB tap)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 🎤  "Call mom about birthday party tomorrow"               │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  AI Interpretation:                                              │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 📋 Call mom about birthday party                           │ │
│  │ 📅 Tomorrow (Feb 3)                                        │ │
│  │ 🎯 Suggested: Q1 - Do First (urgent + family)              │ │
│  │ � Project: Personal (tap to change)                       │ │
│  │ �🔗 Goal: None detected                                     │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  [Edit Details]              [✓ Create Task]                    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Goals & Progress UX

### Goal Card Design

```
┌────────────────────────────────────────────────────────────────┐
│                                                                 │
│  🎯 CAREER                                        ⋯            │
│                                                                 │
│  Get promoted to Senior PM                                     │
│                                                                 │
│  ████████████░░░░░░░░░░░░░░░░░░  42%                          │
│                                                                 │
│  📅 Target: June 2026  •  ⏱️ 4 months left                    │
│  ✅ 5/12 milestones  •  📋 3 linked tasks                      │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### Progress Analytics

```
┌────────────────────────────────────────────────────────────────┐
│                    INSIGHTS DASHBOARD                           │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  This Week                                                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Tasks Completed: 23  (+15% vs last week)                │  │
│  │  Deadlines Met:   91%  🟢                                │  │
│  │  Goal Progress:   +8%  across 4 active goals             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Completion Trend (30 days)                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │    ▄▄                                                    │  │
│  │   ▄██▄   ▄▄                    ▄▄▄                       │  │
│  │  ▄████▄ ▄██▄▄▄   ▄▄▄   ▄▄▄   ▄███▄▄▄▄                   │  │
│  │ ▄██████████████ ████▄ ▄███▄ ▄████████▄                   │  │
│  │ ─────────────────────────────────────                    │  │
│  │ Jan 3                              Feb 2                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ⚠️ Patterns Detected:                                         │
│  • You miss deadlines on Fridays (3x more likely)              │
│  • Q2 tasks often become Q1 (schedule earlier)                 │
│  • Health goals stall mid-week                                 │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

---

## Daily Briefing UX

### Morning Briefing

```
┌────────────────────────────────────────────────────────────────┐
│                    ☀️ GOOD MORNING, ALEX                        │
│                    Tuesday, February 3                          │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  📊 Today's Focus                                              │
│  ───────────────────────────────────────                       │
│  You have 3 urgent tasks and 2 meetings.                       │
│  Your most important task: "Finalize Q1 roadmap"               │
│                                                                 │
│  🔴 Do First (3)                                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 1. Finalize Q1 roadmap          📅 Due today            │  │
│  │ 2. Review Sarah's proposal       📅 Due today            │  │
│  │ 3. Call client re: contract      📅 Due today            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  📅 Schedule                                                   │
│  ───────────────────────────────────────                       │
│  9:00  Team standup (15 min)                                   │
│  11:00 Client call (1 hr)                                      │
│  ░░░░░ 3 hours focus time available                            │
│                                                                 │
│  🎯 Goal Check-in                                              │
│  ───────────────────────────────────────                       │
│  "Get promoted" - Complete today's tasks to stay on track      │
│                                                                 │
│  [Start My Day →]                                              │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### Evening Summary

```
┌────────────────────────────────────────────────────────────────┐
│                    🌙 DAY COMPLETE                              │
│                    Tuesday, February 3                          │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ✅ Accomplishments                                            │
│  ───────────────────────────────────────                       │
│  • Completed 7 of 9 tasks (78%)                                │
│  • All urgent items handled ✓                                  │
│  • 2 meetings with action items captured                       │
│                                                                 │
│  📋 Moved to Tomorrow                                          │
│  ───────────────────────────────────────                       │
│  • Review budget proposal (rescheduled)                        │
│  • Gym session (moved, again... 😅)                            │
│                                                                 │
│  🎯 Goal Progress                                              │
│  ───────────────────────────────────────                       │
│  "Get promoted" +2% today (now 44%)                            │
│                                                                 │
│  💡 Tomorrow's Priority                                        │
│  ───────────────────────────────────────                       │
│  "Quarterly review prep" - Start early, big task               │
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐│
│  │ 🏠 Time to disconnect! You've earned your rest.            ││
│  │    (End-of-day set for 6:00 PM)                [Settings]  ││
│  └────────────────────────────────────────────────────────────┘│
│                                                                 │
│  [Plan Tomorrow →]                                             │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

**End-of-Day Feature (Maya Persona):**
- Configurable "end of workday" time in Settings
- Evening summary includes "work complete" encouragement
- Optional notification at end-of-day to close the app
- Helps freelancers/creators maintain work-life boundaries

---

## Visual Design System

### Color Palette

#### Eisenhower Quadrant Colors
```css
/* Quadrant colors - clear visual distinction */
--color-q1-do:        #DC2626;  /* Red - urgent + important */
--color-q1-bg:        #FEF2F2;
--color-q2-schedule:  #F59E0B;  /* Amber - important, not urgent */
--color-q2-bg:        #FFFBEB;
--color-q3-delegate:  #F97316;  /* Orange - urgent, not important */
--color-q3-bg:        #FFF7ED;
--color-q4-eliminate: #6B7280;  /* Gray - neither */
--color-q4-bg:        #F9FAFB;
```

#### Primary Colors (Material 3)
```css
--md-primary:         #0D9488;  /* Teal - main brand */
--md-on-primary:      #FFFFFF;
--md-primary-container: #A7F3D0;
--md-secondary:       #F59E0B;  /* Amber - accent */
--md-tertiary:        #6366F1;  /* Indigo - special */
```

#### Semantic Colors
```css
--color-success: #10B981;  /* Completed, achieved */
--color-warning: #F59E0B;  /* Attention, approaching */
--color-error:   #EF4444;  /* Overdue, failed */
--color-info:    #3B82F6;  /* Informational */
```

#### Neutral Palette (Material 3)
```css
/* Light Theme */
--md-surface:        #FEFEFE;
--md-surface-variant: #F3F4F6;
--md-on-surface:     #1F2937;
--md-on-surface-variant: #6B7280;
--md-outline:        #E5E7EB;

/* Dark Theme */
--md-surface-dark:        #1F2937;
--md-surface-variant-dark: #374151;
--md-on-surface-dark:     #F9FAFB;
--md-outline-dark:        #4B5563;
```

### Typography (Material 3)

```css
/* Android - Roboto */
--font-display-large:   57px / 64px;
--font-display-medium:  45px / 52px;
--font-headline-large:  32px / 40px;
--font-headline-medium: 28px / 36px;
--font-title-large:     22px / 28px;
--font-title-medium:    16px / 24px;  /* Semibold */
--font-body-large:      16px / 24px;
--font-body-medium:     14px / 20px;
--font-label-large:     14px / 20px;  /* Medium */
--font-label-medium:    12px / 16px;
```

### Spacing System

```css
--space-1:  4px;
--space-2:  8px;
--space-3:  12px;
--space-4:  16px;
--space-5:  20px;
--space-6:  24px;
--space-8:  32px;
--space-10: 40px;
--space-12: 48px;
--space-16: 64px;
```

### Elevation (Material 3)

```css
/* Tonal elevation for M3 */
--elevation-1: 1dp;   /* Cards */
--elevation-2: 3dp;   /* FAB resting */
--elevation-3: 6dp;   /* Bottom sheets */
--elevation-4: 8dp;   /* Dialogs */
--elevation-5: 12dp;  /* FAB pressed */
```

---

## Component Library

### Task Card (Eisenhower)

```
┌────────────────────────────────────────────────────────────────┐
│                     TASK CARD VARIANTS                          │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Standard Task Card:                                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 🔴 │ ○ Finalize Q1 roadmap                           ⋯ │  │
│  │    │   📅 Today  •  🎯 Career Goal                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Overdue Task Card:                                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ ⚠️ │ ○ Review budget                                 ⋯ │  │
│  │    │   📅 Overdue (2 days)  •  ⏰ Snooze               │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Completed Task Card:                                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ ✅ │ ✓ Submit expense report          ━━━━━━━━━━━━━ ⋯ │  │
│  │    │   Completed 2h ago                                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### Goal Progress Card

```
┌────────────────────────────────────────────────────────────────┐
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │  🎯 Get promoted to Senior PM                    ⋯     │  │
│  │                                                          │  │
│  │  ████████████████░░░░░░░░░░░░░░  44%                   │  │
│  │                                                          │  │
│  │  📅 Jun 2026  •  ✅ 5/12 milestones  •  📋 3 tasks     │  │
│  │                                                          │  │
│  │  On track - Complete "Q1 roadmap" to advance            │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

### Meeting Card

```
┌────────────────────────────────────────────────────────────────┐
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ █                                                        │  │
│  │ █  9:00 - 9:15 AM                                       │  │
│  │ █  Team Standup                                         │  │
│  │ █                                                        │  │
│  │ █  📍 Zoom  •  👥 5 attendees                           │  │
│  │ █                                                        │  │
│  │ █  [📋 Agenda]  [📝 Notes]  [✅ Actions: 2]             │  │
│  │ █                                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

### Bottom Navigation (Material 3)

```
┌────────────────────────────────────────────────────────────────┐
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │   ☀️     │  │   📋     │  │   🎯     │  │   ⋯     │       │
│  │  Today   │  │  Tasks   │  │  Goals   │  │  More   │       │
│  │          │  │   (3)    │  │          │  │         │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
│                                                                 │
│                     [  ➕  ]  FAB                               │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

---

## Gestures & Interactions

### Task Gestures

| Gesture | Action | Visual Feedback |
|---------|--------|-----------------|
| Tap | Open task detail | Ripple + sheet slides up |
| Long press | Multi-select mode | Haptic + selection highlight |
| Swipe left | Delete/Archive | Red background reveal |
| Swipe right | Complete | Green background + checkmark |
| Drag | Reorder or change quadrant | Elevation increase + shadow |

### Animation Guidelines

```css
/* Material 3 Motion */
--motion-standard:   300ms cubic-bezier(0.4, 0, 0.2, 1);
--motion-emphasized: 500ms cubic-bezier(0.2, 0, 0, 1);
--motion-decelerate: 400ms cubic-bezier(0, 0, 0, 1);

/* Durations */
--duration-quick: 100ms;   /* Immediate feedback */
--duration-short: 200ms;   /* State changes */
--duration-medium: 300ms;  /* Navigation */
--duration-long: 500ms;    /* Complex transitions */
```

### Task Completion Animation
1. Checkbox scales up with bounce (spring curve)
2. Task text gets strikethrough (left to right, 200ms)
3. Card fades and shrinks (300ms)
4. Celebratory haptic pulse

---

## Accessibility

### WCAG 2.1 AA Compliance

#### Visual
- Minimum contrast: 4.5:1 (text), 3:1 (large text)
- Eisenhower colors have sufficient contrast
- Never rely on color alone (icons + labels)
- Support for font scaling (up to 200%)

#### Motor
- Touch targets: 48x48dp minimum
- Adequate spacing: 8dp between targets
- Full TalkBack navigation
- External keyboard support

#### Cognitive
- Consistent navigation patterns
- Clear, simple language
- Undo for destructive actions (5 seconds)
- Progress saving on interruption

### Android Accessibility Features

- contentDescription for all interactive elements
- Semantic headings for screen structure
- Custom actions for complex components
- Focus management for modals

---

## Offline-First UX Patterns

### No Connectivity State

```
┌────────────────────────────────────────────────────────────────┐
│  [Status Bar]                                  📵 Offline     │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  All features work normally.                                   │
│  Everything is saved locally.                                  │
│                                                                 │
│  (Content continues with no degradation)                       │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### Key Principles
- **No Loading Spinners** for local operations
- **Optimistic UI**: Changes appear immediately
- **Background Sync**: Sync happens invisibly when connected
- **Conflict Resolution**: Last-write-wins with history

---

## AI Interaction Patterns

### Natural Language Input

```
┌────────────────────────────────────────────────────────────────┐
│                    AI INPUT PATTERNS                            │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Voice Input:                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │        🎤                                                │  │
│  │   ┌──────────┐                                           │  │
│  │   │  ▁▃▅▇▅▃▁ │  "Call mom about..."                     │  │
│  │   └──────────┘                                           │  │
│  │                                                          │  │
│  │   [Cancel]                      Processing locally...    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Text Input:                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 🎤  Add "review proposal" for tomorrow morning           │  │
│  │     ──────────────────────────────────────────           │  │
│  │     AI suggestions:                                      │  │
│  │     • Set deadline: Tomorrow 9 AM                        │  │
│  │     • Quadrant: Q2 (Important, schedule it)              │  │
│  │     • Link to goal: "Get promoted"                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### AI Suggestion Cards

```
┌────────────────────────────────────────────────────────────────┐
│  💡 Prio Suggestion                                          │
│  ─────────────────────────────────────────                     │
│  "You have 5 tasks in Q3 (Delegate). Consider:                 │
│   delegating to team members or declining"                     │
│                                                                 │
│  [Dismiss]  [Review Q3 Tasks →]                                │
└────────────────────────────────────────────────────────────────┘
```

---

## First-Time User Experience

### Onboarding Flow

```
1. Welcome Screen
   ├── "Welcome to Prio"
   ├── Privacy-first messaging
   └── [Get Started]

2. Value Proposition (3 slides)
   ├── Slide 1: Eisenhower Matrix intro
   ├── Slide 2: Goals connected to tasks
   ├── Slide 3: AI that works offline
   └── [Continue]

3. AI Model Setup
   ├── "Download AI model for offline use"
   ├── Size: ~1.5GB, progress indicator
   ├── "Skip for now" option (limited features)
   └── [Download] or [Skip]

4. Quick Personalization
   ├── Name
   ├── Work schedule (optional)
   ├── Notification preferences
   └── [Continue]

5. First Task Creation
   ├── Guided voice/text task creation
   ├── See Eisenhower classification
   ├── Success moment
   └── [Enter App]
```

---

## Design Handoff

### Figma Structure

```
Prio Design System/
├── 🎨 Foundations/
│   ├── Material 3 Colors
│   ├── Typography
│   ├── Spacing & Grid
│   ├── Icons (Material Symbols)
│   └── Eisenhower Colors
├── 🧩 Components/
│   ├── Task Cards
│   ├── Goal Cards
│   ├── Meeting Cards
│   ├── Briefing Cards
│   ├── Navigation
│   ├── Inputs (NL, Voice)
│   ├── Charts/Progress
│   └── Dialogs/Sheets
├── 📱 Screens/
│   ├── Onboarding/
│   ├── Today/
│   ├── Tasks (Eisenhower)/
│   ├── Goals/
│   ├── Calendar/
│   ├── Insights/
│   └── Settings/
└── 🎬 Prototypes/
    └── Key Flows
```

---

*Document Owner: Principal UX Designer*
*Last Updated: February 2026*
*Status: Approved for MVP Development*

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
Voice: "Hey Prio, remind me to call mom tomorrow at 2pm"
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
Prio Design System/
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

## Post-MVP Feature UX: AI Model Selection & Custom Agents

### AI Model Selection (Settings Screen)

```
┌─────────────────────────────────────────────────────────────────┐
│                    ⚙️ AI SETTINGS                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Current Plan: Pro ($9.99/mo)      [Upgrade]                    │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 💰 AI Credits                                               │ │
│  │                                                              │ │
│  │ ████████████░░░░░░░░░░░░░  $3.47 of $5.00 used             │ │
│  │                                                              │ │
│  │ Resets Feb 28  •  [View Usage Details]                      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Default AI Model                                               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ ● On-Device (Phi-3)         Free, Private, Works Offline  │ │
│  │ ○ Claude Sonnet             Balanced quality & cost        │ │
│  │ ○ GPT-4o                    Fast, general purpose          │ │
│  │ ○ Gemini 1.5 Pro            Long context, multimodal       │ │
│  │ ○ Grok-2                    Creative, real-time info       │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  🧠 Smart Routing                                    [ON]       │
│  Auto-select model based on task complexity                     │
│                                                                  │
│  Model Per Feature                            [Configure >]     │
│  Currently: On-device for simple, Claude for complex           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Model Per Feature Configuration

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Back           MODEL PREFERENCES                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Choose which model to use for each feature:                    │
│                                                                  │
│  Task Categorization                                            │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ On-Device (Phi-3)                                    ▼    │ │
│  └────────────────────────────────────────────────────────────┘ │
│  Simple task → fast local processing                            │
│                                                                  │
│  Daily Briefing                                                 │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Claude Sonnet                                        ▼    │ │
│  └────────────────────────────────────────────────────────────┘ │
│  Complex summary → higher quality                               │
│                                                                  │
│  Meeting Notes Analysis                                         │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ GPT-4o                                               ▼    │ │
│  └────────────────────────────────────────────────────────────┘ │
│  Action extraction → good balance                               │
│                                                                  │
│  Goal Coaching (Agents)                                         │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Claude Sonnet                                        ▼    │ │
│  └────────────────────────────────────────────────────────────┘ │
│  Conversational → nuanced responses                             │
│                                                                  │
│                              [Reset to Defaults]                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Custom Agents - Agent List

```
┌─────────────────────────────────────────────────────────────────┐
│                      🤖 MY AGENTS                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [+ Create New Agent]                     [Browse Templates]    │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  🏃 Fitness Coach                                    ▸    │ │
│  │  Helps with workout goals • Linked to "Run 5K" goal        │ │
│  │  Last chat: 2 hours ago                                    │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  💼 Career Mentor                                    ▸    │ │
│  │  Career development guidance • Linked to "Get promoted"   │ │
│  │  Last chat: Yesterday                                      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  📊 Project Helper                                   ▸    │ │
│  │  Sprint planning & deadlines • Custom agent               │ │
│  │  Last chat: 3 days ago                                     │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ────────────────────────────────────────────────────────────   │
│                                                                  │
│  📚 TEMPLATES                                                   │
│                                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │  📊 Project │ │  💰 Finance │ │  📚 Learning│               │
│  │   Manager   │ │   Advisor   │ │    Coach    │               │
│  │   [Add]     │ │   [Add]     │ │   [Add]     │               │
│  └─────────────┘ └─────────────┘ └─────────────┘               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Agent Builder Wizard

#### Step 1: Purpose

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Cancel        NEW AGENT (1/5)                        Next → │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                  What should your agent help with?              │
│                                                                  │
│  Popular Categories:                                            │
│                                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │     💼      │ │     🏃      │ │     📚      │               │
│  │   Career    │ │   Fitness   │ │  Learning   │               │
│  └─────────────┘ └─────────────┘ └─────────────┘               │
│                                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │     💰      │ │     ✍️      │ │     🏠      │               │
│  │   Finance   │ │   Writing   │ │    Home     │               │
│  └─────────────┘ └─────────────┘ └─────────────┘               │
│                                                                  │
│  Or describe your custom purpose:                               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Help me stay focused on my side project after work...     │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  💡 AI will help refine your agent based on your description   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Step 2: Personality

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Back          NEW AGENT (2/5)                        Next → │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│              How should your agent communicate?                 │
│                                                                  │
│  Tone                                                           │
│  Casual  ●────────────────○────────────────────○  Formal       │
│          ▲                                                      │
│                                                                  │
│  Detail Level                                                   │
│  Brief   ○────────────────●────────────────────○  Detailed     │
│                           ▲                                     │
│                                                                  │
│  Style                                                          │
│  Direct  ○────────────────○────────────────────●  Encouraging  │
│                                                 ▲               │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Preview:                                                    │ │
│  │                                                             │ │
│  │ "Great progress on your workout streak! 🎉 You've been     │ │
│  │ consistent for 5 days. Ready to push a bit harder today?   │ │
│  │ I noticed you usually have energy around 6pm."             │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  [✓] Use emojis                                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Step 3: Expertise

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Back          NEW AGENT (3/5)                        Next → │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│              What should your agent know about?                 │
│                                                                  │
│  Expertise Areas (select all that apply):                       │
│                                                                  │
│  [✓] Fitness & Exercise      [ ] Nutrition                     │
│  [✓] Habit Building          [ ] Sleep & Recovery              │
│  [✓] Motivation              [ ] Sports Training               │
│                                                                  │
│  ────────────────────────────────────────────────────────────   │
│                                                                  │
│  Custom Instructions (optional):                                │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ I'm training for my first 5K race in April. I prefer       │ │
│  │ running outdoors. I have a knee injury history so be       │ │
│  │ careful with high-impact suggestions.                      │ │
│  │                                                             │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Link to Goal (optional):                                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 🏃 Run a 5K in under 30 minutes              [Change]      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Step 4: Permissions

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Back          NEW AGENT (4/5)                        Next → │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│              What can your agent access?                        │
│                                                                  │
│  Your agent needs access to help you effectively.               │
│  You control what it can see and do.                           │
│                                                                  │
│  READ ACCESS                                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ [✓] Tasks         See your tasks and priorities            │ │
│  │ [✓] Goals         Track progress on linked goals           │ │
│  │ [✓] Calendar      Know your schedule and availability      │ │
│  │ [ ] Notes         Read your meeting notes                   │ │
│  │ [✓] Analytics     Understand your productivity patterns    │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ACTION PERMISSIONS                                             │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ [✓] Suggest tasks        Agent can recommend tasks         │ │
│  │ [ ] Create tasks         Agent can add tasks directly      │ │
│  │ [✓] Create reminders     Agent can set reminders           │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ⓘ You can change these permissions anytime in settings        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Step 5: Test & Save

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Back          NEW AGENT (5/5)                     [Create] │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│              Test your agent!                                   │
│                                                                  │
│  Agent Name:                                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 🏃 5K Coach                                                │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Try chatting to make sure it feels right:                      │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                                                             │ │
│  │  🏃 Hey! I'm your 5K Coach. I see you're training for     │ │
│  │     your first 5K - that's exciting! I noticed you         │ │
│  │     haven't logged a run in 3 days. Ready to get back      │ │
│  │     out there? The weather looks good this evening! 🌤️     │ │
│  │                                                             │ │
│  │                    You: I'm feeling tired today            │ │
│  │                                                             │ │
│  │  🏃 I hear you! Rest is part of training too. How about   │ │
│  │     a light 15-minute walk instead? It keeps the habit     │ │
│  │     alive without pushing too hard. Your knee will         │ │
│  │     thank you! Tomorrow we can try an easy run. 💪         │ │
│  │                                                             │ │
│  └────────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Type a message to test...                              🎤 │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  [Adjust Settings]                            [✓ Create Agent] │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Agent Chat Screen

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Back       🏃 5K Coach                              ⚙️ ⋮    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  🏃 Good morning! Ready for your training today?           │ │
│  │     I checked your calendar - you have a free slot at      │ │
│  │     5:30pm. Perfect for a 25-minute easy run! 🌅           │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│                                    ┌──────────────────────────┐ │
│                                    │ Sounds good, remind me   │ │
│                                    │ at 5pm?                  │ │
│                                    └──────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  🏃 Done! I'll ping you at 5pm with a reminder and         │ │
│  │     today's weather. Here's your suggested route: 🗺️       │ │
│  │                                                             │ │
│  │     ┌──────────────────────────────────────────────────┐   │ │
│  │     │ ⏰ Reminder Created                               │   │ │
│  │     │ "5K Training - Easy Run"                         │   │ │
│  │     │ Today at 5:00 PM                                 │   │ │
│  │     └──────────────────────────────────────────────────┘   │ │
│  │                                                             │ │
│  │     Any specific focus for today's run?                    │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Quick Replies:                                                 │
│  [Focus on pace] [Just enjoy it] [Interval training]           │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Message 5K Coach...                               🎤  ➤  │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

*Document Owner: Principal UX Designer*
*Last Updated: February 2026*
*Status: Living Document*
