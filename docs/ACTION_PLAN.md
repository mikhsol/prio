# Jeeves - MVP Action Plan

## Overview

This action plan outlines the complete development roadmap for Jeeves MVP, from research to Play Store launch. 

### Guiding Principles

- **80/20 Rule (Pareto Principle)**: Focus on the 20% of features that deliver 80% of value
- **SMART Tasks**: Each task is Specific, Measurable, Achievable, Relevant, Time-bound
- **Offline-First**: Core features work without internet
- **Android-First**: iOS architecture-ready but not in MVP scope

### Key Constraints

- **Platform**: Android (API 29+)
- **AI**: On-device LLM (llama.cpp + Phi-3-mini/Gemma 2B)
- **Timeline**: 14 weeks to beta, 16 weeks to launch
- **Task Limit**: Each task ≤4 hours

### SMART Task Format

Each task follows this format:
- **Specific**: Clear description of what to do
- **Measurable**: Concrete deliverable or acceptance criteria
- **Achievable**: Scoped to ≤4 hours
- **Relevant**: Directly contributes to MVP
- **Time-bound**: Duration estimate provided

---

## Phase 0: Research & Planning (Weeks 1-2)

### Milestone 0.1: Market Analysis
**Goal**: Understand competitive landscape and identify positioning opportunities  
**Owner**: Marketing Expert + Product Manager

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 0.1.1 | Analyze top 6 todo app competitors (Todoist, TickTick, Any.do, Things, Google Tasks, Microsoft To Do) | Marketing | 3h | Comparison matrix with features, pricing, ratings, download counts for each app |
| 0.1.2 | Analyze AI assistant landscape (Google Assistant, Samsung AI, Apple Intelligence) | Marketing | 2h | Feature matrix comparing AI capabilities, privacy approaches, platform availability |
| 0.1.3 | Research on-device LLM options (Phi-3, Gemma, TinyLlama, Qwen) | Backend Engineer | 3h | Technical comparison doc: model size, RAM needs, inference speed, quality benchmarks |
| 0.1.4 | Synthesize competitive insights into positioning opportunities | Marketing | 2h | 1-page positioning brief with 3 key differentiation opportunities |
| 0.1.5 | Define 3 target user personas with pain points and goals | Product Manager | 3h | 3 documented personas (1 page each) with demographics, pain points, goals, behaviors |
| 0.1.6 | Create competitive analysis report | Marketing | 2h | 5-page report with market gaps, opportunities, and recommended positioning |

**Milestone Exit Criteria**: 
- [ ] Competitive matrix complete with 6+ apps analyzed
- [ ] 3 personas documented and validated
- [ ] Positioning opportunities identified

### Milestone 0.2: On-Device LLM Technical Research
**Goal**: Select optimal LLM model and integration approach for Android  
**Owner**: Android Developer

*Note: LLM integration via JNI/NDK is Android-native work. Backend Engineer assists with prompt design only.*

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 0.2.1 | Set up llama.cpp Android test project with JNI | Android Developer | 4h | Working Android project that can load a GGUF model via NDK |
| 0.2.2 | Benchmark Phi-3-mini-4k-instruct (Q4_K_M) on 2 reference devices | Android Developer | 3h | Performance report: tokens/sec, memory usage, first inference time on Pixel 6/7 and Samsung mid-range |
| 0.2.3 | Test task categorization accuracy with 20 sample prompts | Android Developer | 2h | Accuracy report: % correct Eisenhower classification |
| 0.2.4 | Document memory/storage requirements and device compatibility | Android Developer | 1h | Compatibility matrix: minimum device specs, storage needs |
| 0.2.5 | Write LLM selection recommendation with rule-based fallback | Android Developer | 1h | 1-page technical recommendation doc with primary model, regex fallback approach |

**Milestone Exit Criteria**:
- [ ] Phi-3-mini benchmarked on 2 device tiers (high-end, mid-range)
- [ ] Task categorization accuracy >80%
- [ ] Model recommendation documented with fallback strategy

### Milestone 0.3: MVP Definition & Validation
**Goal**: Define minimal feature set that delivers 80% of value, validated with target users  
**Owner**: Product Manager + UX Designer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 0.3.1 | Apply 80/20 analysis to identify core features | Product Manager | 2h | Prioritized feature list with estimated value/effort scores |
| 0.3.2 | Write user stories for Task Management (8-10 stories) | Product Manager | 3h | User stories in "As a... I want... So that..." format with acceptance criteria |
| 0.3.3 | Write user stories for Goals plugin (5-6 stories) | Product Manager | 2h | User stories with acceptance criteria |
| 0.3.4 | Write user stories for Calendar/Briefings (5-6 stories) | Product Manager | 2h | User stories with acceptance criteria |
| 0.3.5 | Define MVP scope boundary (what's in/out) | Product Manager | 2h | Explicit in-scope and out-of-scope feature lists |
| 0.3.6 | Create MVP PRD document | Product Manager | 3h | Complete PRD with vision, personas, features, success metrics, risks |
| 0.3.7 | Conduct 3 guerrilla user interviews (coffee shop/online) | UX Designer | 2h | Quick validation of top 3 pain points, notes on key insights |
| 0.3.8 | Define 5 core success metrics | Product Manager | 1h | DAU, retention, task completion rate, AI accuracy, crash-free rate with targets |

**Milestone Exit Criteria**:
- [ ] 20-25 user stories documented with acceptance criteria
- [ ] Clear MVP boundary defined
- [ ] PRD reviewed and approved
- [ ] Key pain points validated with 3+ users

---

## Phase 1: Design & Setup (Weeks 2-3)

### Milestone 1.1: UX Design (Text-Based Specifications)
**Goal**: Create detailed screen specifications without visual design tools. If possible, generate mocks
**Owner**: UX Designer

*Note: Using text-based specifications instead of Figma. Free alternatives like Penpot or Excalidraw can be used for wireframes if needed.*

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 1.1.1 | Write detailed spec for Task List screen | UX Designer | 3h | Screen spec document: all elements, states, interactions, edge cases (template below) |
| 1.1.2 | Write detailed spec for Task Detail sheet | UX Designer | 2h | Screen spec with all fields, validation rules, AI interaction points |
| 1.1.3 | Write detailed spec for Quick Capture flow | UX Designer | 2h | Flow spec with voice/text input, AI response display, confirmation |
| 1.1.4 | Write detailed spec for Goals List and Detail screens | UX Designer | 3h | Screen specs for goal list, goal detail with progress, milestones |
| 1.1.5 | Write detailed spec for Today/Dashboard screen | UX Designer | 3h | Screen spec showing briefing, top tasks, calendar, goals summary |
| 1.1.6 | Write detailed spec for Calendar Day view | UX Designer | 2h | Screen spec for schedule view, meeting cards, task blocks |
| 1.1.7 | Write detailed spec for Analytics/Insights screen | UX Designer | 2h | Screen spec for charts, metrics, patterns display |
| 1.1.8 | Write detailed spec for Onboarding flow (5 screens) | UX Designer | 3h | Screen specs for welcome, value props, model download, permissions, first task |
| 1.1.9 | Write detailed spec for Settings screens | UX Designer | 2h | Screen spec for all settings categories and options |
| 1.1.10 | Define error states, empty states, and offline indicators | UX Designer | 2h | Standard patterns for errors, empty screens, offline banner |
| 1.1.11 | Define basic accessibility requirements | UX Designer | 1h | Touch targets ≥48dp, contrast ratios, TalkBack basics |
| 1.1.12 | Create wireframes using Penpot/Excalidraw (optional) | UX Designer | 4h | Low-fidelity wireframes for key screens (if time permits) |
| 1.1.13 | Define component specifications (buttons, cards, inputs) | UX Designer | 3h | Component spec document with sizes, colors, states, spacing |

**Screen Specification Template**:
```
## Screen: [Name]

### Purpose
[What this screen accomplishes]

### Entry Points
[How users get to this screen]

### Layout
[Top to bottom description of all elements]

### Elements
| Element | Type | Content | Behavior |
|---------|------|---------|----------|
| Header | AppBar | "Tasks" | Back button if nested |
| ... | ... | ... | ... |

### States
- Empty state: [description]
- Loading state: [description]
- Error state: [description]
- Populated state: [description]

### Interactions
- Tap on X: [behavior]
- Swipe on Y: [behavior]
- Long press on Z: [behavior]

### AI Integration Points
[Where AI/LLM is invoked]

### Accessibility
[TalkBack descriptions, touch targets]
```

**Milestone Exit Criteria**:
- [ ] All 9 key screens + onboarding flow have text specifications
- [ ] Error/empty/offline patterns defined
- [ ] Basic accessibility requirements documented
- [ ] Component specifications defined
- [ ] Specifications reviewed by Android Developer for feasibility

### Milestone 1.2: Project Setup
**Goal**: Create Android project with proper architecture and dependencies  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 1.2.1 | Create Android project with Gradle version catalog | Android Developer | 2h | Project builds successfully, all dependencies in libs.versions.toml |
| 1.2.2 | Configure build variants (debug, release, benchmark) | Android Developer | 1h | 3 build variants configured with appropriate flags |
| 1.2.3 | Set up multi-module structure | Android Developer | 2h | Modules created: :app, :core:common, :core:ui, :core:data, :core:domain, :core:ai |
| 1.2.4 | Configure Hilt dependency injection | Android Developer | 2h | Hilt set up in all modules, sample injection working |
| 1.2.5 | Set up Room database | Android Developer | 2h | Database initializes, sample entity CRUD works (encryption deferred to v1.1) |
| 1.2.6 | Configure DataStore for preferences | Android Developer | 1h | UserPreferences DataStore created with sample preferences |
| 1.2.7 | Set up Compose navigation with type-safe routes | Android Developer | 2h | Navigation graph with 5+ placeholder destinations |
| 1.2.8 | Create Material 3 theme (colors, typography) | Android Developer | 2h | Theme applied, light/dark mode working |
| 1.2.9 | Set up testing infrastructure | Android Developer | 2h | JUnit 5, MockK, Turbine configured, sample test passing |
| 1.2.10 | Configure GitHub Actions CI | Android Developer | 2h | CI runs on PR: build, lint, test |
| 1.2.11 | Set up Firebase Crashlytics | Android Developer | 1h | Crash reporting configured for debug and release builds |

**Milestone Exit Criteria**:
- [ ] Project builds and runs on emulator
- [ ] All modules created and connected
- [ ] CI pipeline passing
- [ ] Encrypted database working

### Milestone 1.3: Quick Design Validation
**Goal**: Lightweight validation of core flows before development  
**Owner**: UX Designer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 1.3.1 | Create paper/Excalidraw prototype of 3 core flows | UX Designer | 2h | Task creation, goal tracking, daily briefing flows sketched |
| 1.3.2 | Run 3 hallway usability tests (informal) | UX Designer | 2h | 3 people walk through flows, major confusion points noted |
| 1.3.3 | Fix critical UX issues in specs | UX Designer | 1h | Top 3 issues addressed, specs updated |

**Milestone Exit Criteria**:
- [ ] Core flows tested with 3 people
- [ ] No major confusion points remain
- [ ] Specs updated and ready for development

---

## Phase 2: Core Infrastructure (Weeks 3-5)

### Milestone 2.1: Data Layer
**Goal**: Implement all database entities, DAOs, and repositories  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 2.1.1 | Create TaskEntity with all fields | Android Developer | 2h | Entity with 15+ fields, Eisenhower quadrant, all enums defined |
| 2.1.2 | Create TaskDao with CRUD + queries | Android Developer | 2h | DAO with insert, update, delete, getByQuadrant, getByDate, search |
| 2.1.3 | Create GoalEntity and GoalDao | Android Developer | 2h | Entity with milestones support, DAO with progress queries |
| 2.1.4 | Create MeetingEntity and MeetingDao | Android Developer | 2h | Entity for meeting notes, action items, DAO with date range queries |
| 2.1.5 | Create DailyAnalyticsEntity and DAO | Android Developer | 2h | Entity for daily stats, DAO with aggregation queries |
| 2.1.6 | Implement TaskRepository with Flow | Android Developer | 3h | Repository exposing Flow<List<Task>>, all CRUD operations |
| 2.1.7 | Implement GoalRepository | Android Developer | 2h | Repository with progress calculation, task linking |
| 2.1.8 | Implement MeetingRepository | Android Developer | 2h | Repository with calendar event linking |
| 2.1.9 | Implement AnalyticsRepository | Android Developer | 2h | Repository with aggregation methods |
| 2.1.10 | Write unit tests for all repositories | Android Developer | 3h | 80%+ coverage on repository layer, 15+ test cases |
| 2.1.11 | Create UserPreferences with DataStore | Android Developer | 2h | All preference fields, type-safe access |

**Milestone Exit Criteria**:
- [ ] All 4 entities created with proper relationships
- [ ] All repositories tested with 80%+ coverage
- [ ] Migrations strategy documented

### Milestone 2.2: AI Engine
**Goal**: Integrate on-device LLM and create inference interface  
**Owner**: Android Developer (Backend Engineer assists with prompts only)

*Note: JNI/NDK integration is Android work. Streaming deferred to post-MVP; sync inference is sufficient.*

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 2.2.1 | Integrate llama.cpp via JNI/NDK | Android Developer | 4h | JNI wrapper compiles, basic inference works in test app |
| 2.2.2 | Create LlmEngine interface | Android Developer | 1h | Interface with classifyTask(), parseTask() methods |
| 2.2.3 | Implement LlamaCppEngine (sync inference) | Android Developer | 3h | Implementation using JNI wrapper, background thread |
| 2.2.4 | Create ModelManager for download/verification | Android Developer | 3h | Downloads model with progress, verifies checksum, resumes partial downloads |
| 2.2.5 | Create RuleBasedFallback engine | Android Developer | 2h | Regex-based task parsing for low-end devices |
| 2.2.6 | Write Eisenhower classification prompts | Android Developer | 2h | Prompt template with 80%+ accuracy on 20 test cases |
| 2.2.7 | Write task parsing prompts | Android Developer | 2h | Prompt extracts: title, date, time, priority from natural language |
| 2.2.8 | Performance test: inference under 3 seconds | Android Developer | 1h | 90%+ queries complete in <3s on mid-range devices |
| 2.2.9 | Write AI engine tests | Android Developer | 2h | Unit tests for prompt processing, mock-based inference tests |

**Milestone Exit Criteria**:
- [ ] LLM sync inference working on device (<3s)
- [ ] Model download with resume support
- [ ] Rule-based fallback for low-end devices
- [ ] Eisenhower classification accuracy >80%

### Milestone 2.3: UI Design System Implementation
**Goal**: Implement reusable Compose components matching specifications  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 2.3.1 | Implement color tokens and theme provider | Android Developer | 2h | All colors from spec, dynamic theming support |
| 2.3.2 | Implement typography scale | Android Developer | 1h | All text styles matching Material 3 scale |
| 2.3.3 | Create TaskCard component | Android Developer | 2h | Card with all states: default, overdue, completed, swiping |
| 2.3.4 | Create GoalCard component | Android Developer | 2h | Card with progress bar, milestone count, category icon |
| 2.3.5 | Create MeetingCard component | Android Developer | 2h | Card with time, title, attendees, action items count |
| 2.3.6 | Create BriefingCard component | Android Developer | 2h | Card with summary text, expandable sections |
| 2.3.7 | Create TextField and VoiceInput | Android Developer | 3h | Text input with AI indicator, voice button with waveform |
| 2.3.8 | Create BottomSheet and Dialog | Android Developer | 2h | Reusable bottom sheet with drag handle, confirmation dialogs |
| 2.3.9 | Create BottomNavigation | Android Developer | 2h | Bottom nav with 4 items, badges, FAB integration |
| 2.3.10 | Create component preview showcase | Android Developer | 2h | Preview composables for all components in all states |

**Milestone Exit Criteria**:
- [ ] All 7+ components implemented
- [ ] Light and dark theme working
- [ ] Components match text specifications
- [ ] Preview showcase complete

---

## Phase 3: Feature Plugins (Weeks 5-10)

### Milestone 3.1: Tasks Plugin
**Goal**: Complete Eisenhower-based task management with AI prioritization  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 3.1.1 | Implement EisenhowerEngine (priority calculation) | Android Developer | 4h | Engine calculates quadrant from urgency/importance, handles deadlines |
| 3.1.2 | Implement Task List screen with filters | Android Developer | 4h | List view with quadrant filters, sort options, search |
| 3.1.4 | Implement Task Detail bottom sheet | Android Developer | 3h | All fields editable, goal linking, delete confirmation |
| 3.1.5 | Implement Quick Capture with AI | Android Developer | 3h | Text input → AI parsing → preview → create (voice deferred to v1.1) |
| 3.1.6 | Implement drag-and-drop reordering | Android Developer | 3h | Reorder within list, haptic feedback |
| 3.1.7 | Implement swipe actions | Android Developer | 2h | Swipe right = complete, swipe left = delete |
| 3.1.8 | Implement task filters and search | Android Developer | 2h | Filter by quadrant, status, date range; full-text search |
| 3.1.9 | Implement recurring tasks | Android Developer | 2h | Daily/weekly/monthly presets (full RRULE deferred to v1.1) |
| 3.1.10 | Implement smart reminders (WorkManager) | Android Developer | 3h | Scheduled notifications, snooze support |
| 3.1.11 | Write UI tests for Tasks plugin | Android Developer | 3h | 10+ UI test cases covering main flows |

**Milestone Exit Criteria**:
- [ ] Tasks can be created via AI natural language
- [ ] Eisenhower prioritization working (80%+ accuracy)
- [ ] All CRUD operations functional
- [ ] Reminders trigger correctly

### Milestone 3.2: Goals Plugin
**Goal**: Goal setting and progress tracking linked to tasks  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 3.2.1 | Implement Goals List screen | Android Developer | 3h | List with category grouping, progress indicators |
| 3.2.2 | Implement Goal Detail screen | Android Developer | 3h | Progress chart, linked tasks, milestones |
| 3.2.3 | Implement Goal Creation wizard | Android Developer | 4h | Multi-step: title, category, target date, milestones |
| 3.2.4 | Implement milestone tracking | Android Developer | 2h | Add/complete milestones, progress recalculation |
| 3.2.5 | Implement task-to-goal linking | Android Developer | 2h | Link existing tasks, create task from goal |
| 3.2.6 | Implement progress calculation | Android Developer | 2h | Progress from milestones and/or linked task completion |
| 3.2.7 | Implement goal-based AI suggestions | Android Developer | 3h | AI suggests tasks to advance goals |
| 3.2.8 | Write UI tests for Goals plugin | Android Developer | 2h | 8+ UI test cases |

**Milestone Exit Criteria**:
- [ ] Goals can be created with milestones
- [ ] Progress updates automatically from task completion
- [ ] Goal-task linking bidirectional

### Milestone 3.3: Calendar Plugin
**Goal**: Calendar integration with meeting notes and action items  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 3.3.1 | Implement calendar provider integration | Android Developer | 4h | Read device calendar events, permission handling |
| 3.3.2 | Implement Calendar Day view | Android Developer | 3h | Timeline with events and task blocks |
| 3.3.3 | Implement Meeting Detail sheet | Android Developer | 3h | View/edit notes, agenda checklist, action items |
| 3.3.4 | Implement meeting notes editor | Android Developer | 2h | Plain text notes with auto-save (rich text deferred to v1.1) |
| 3.3.5 | Implement AI action item extraction | Android Developer | 2h | Extract action items from notes → create tasks |
| 3.3.6 | Implement meeting checklist/agenda | Android Developer | 2h | Checklist items, completion tracking |
| 3.3.7 | Write UI tests for Calendar plugin | Android Developer | 2h | 6+ UI test cases |

**Milestone Exit Criteria**:
- [ ] Calendar events display correctly
- [ ] Meeting notes persist
- [ ] Action items extractable and convertible to tasks

### Milestone 3.4: Daily Briefings
**Goal**: AI-generated morning and evening summaries  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 3.4.1 | Implement BriefingGenerator | Android Developer | 3h | Generates briefing from tasks, calendar, goals using LLM |
| 3.4.2 | Implement Morning Briefing screen | Android Developer | 3h | Today's priorities, schedule, goal check-ins |
| 3.4.3 | Implement Evening Summary screen | Android Developer | 2h | Completed vs planned, insights |
| 3.4.4 | Implement briefing notifications | Android Developer | 2h | Configurable morning/evening notification times |
| 3.4.5 | Implement Today/Dashboard screen | Android Developer | 4h | Combined view: briefing + top tasks + calendar + goals |

**Milestone Exit Criteria**:
- [ ] Briefings generate in <3 seconds
- [ ] Briefings are contextual and actionable
- [ ] Notifications trigger at configured times

### Milestone 3.5: Basic Analytics (Simplified)
**Goal**: Simple productivity metrics (detailed insights deferred to v1.1)  
**Owner**: Android Developer

*Note: 80/20 insights, complex charts, and missed deadline analysis deferred to v1.1 to accelerate MVP.*

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 3.5.1 | Implement analytics data collection | Android Developer | 2h | Track task creation, completion, timing |
| 3.5.2 | Implement Simple Stats screen | Android Developer | 3h | Weekly view: tasks completed, goals progress, streaks |
| 3.5.3 | Implement task completion chart | Android Developer | 2h | Simple bar chart of completions over 7 days |

**Milestone Exit Criteria**:
- [ ] Basic metrics displayed (tasks completed, streaks)
- [ ] Simple chart renders correctly
- [ ] Data collection working in background

---

## Phase 4: Polish & Integration (Weeks 10-12)

### Milestone 4.1: Onboarding & Settings
**Goal**: Smooth first-time experience and configuration  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 4.1.1 | Implement onboarding flow (5 screens) | Android Developer | 4h | Welcome, value props, model download, permissions, first task |
| 4.1.2 | Implement model download screen | Android Developer | 3h | Progress indicator, skip option, resume support |
| 4.1.3 | Implement permissions explanation | Android Developer | 2h | Clear rationale for each permission |
| 4.1.4 | Implement Settings screen | Android Developer | 3h | All preference categories |
| 4.1.5 | Implement theme switching | Android Developer | 2h | Light/dark/system toggle |
| 4.1.6 | Implement notification settings | Android Developer | 2h | Configure briefing times, nudge frequency |
| 4.1.7 | Write onboarding tests | Android Developer | 2h | Full flow UI test |

**Milestone Exit Criteria**:
- [ ] New users complete onboarding in <3 minutes
- [ ] Model download works reliably
- [ ] All settings persist correctly

### Milestone 4.2: Notifications
**Goal**: Proactive engagement through notifications  
**Owner**: Android Developer

*Note: Home screen widget and Quick Settings tile deferred to v1.1 to reduce complexity.*

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 4.2.1 | Implement notification channels | Android Developer | 2h | Channels for reminders, briefings, nudges |
| 4.2.2 | Implement smart nudge system | Android Developer | 2h | Nudges for overdue/important tasks |
| 4.2.3 | Write notification tests | Android Developer | 1h | Verify notification content and timing |

**Milestone Exit Criteria**:
- [ ] Notifications appear correctly on Android 10+
- [ ] Nudges trigger for overdue tasks
- [ ] Briefing notifications at configured times

### Milestone 4.3: Performance & Testing
**Goal**: Meet performance targets, essential test coverage  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 4.3.1 | Profile and optimize app performance | Android Developer | 3h | Identify and fix top 3 performance issues |
| 4.3.2 | Optimize LLM memory usage | Android Developer | 3h | Peak memory <1GB during inference |
| 4.3.3 | Optimize cold start time | Android Developer | 2h | Cold start <4s on mid-range device |
| 4.3.4 | Create critical path E2E tests | Android Developer | 3h | 8 E2E tests: onboarding, task CRUD, goals, calendar |
| 4.3.5 | Test on 3 device configurations | Android Developer | 3h | Test on Pixel, Samsung, and one budget device |
| 4.3.6 | Accessibility smoke test | Android Developer | 2h | TalkBack navigation works, touch targets ≥48dp |
| 4.3.7 | Configure ProGuard/R8 for release | Android Developer | 1h | Obfuscation and minification enabled |

**Milestone Exit Criteria**:
- [ ] Cold start <4s
- [ ] LLM inference <3s on mid-range devices
- [ ] Critical path E2E tests pass
- [ ] 3 devices tested
- [ ] Release build configured with R8

---

## Phase 5: Launch Preparation (Weeks 12-14)

### Milestone 5.1: Release Preparation
**Goal**: Prepare all assets and configurations for Play Store  
**Owner**: Android Developer + Marketing

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 5.1.1 | Create app icon (adaptive icon) | UX Designer | 2h | Icon meets Material Design guidelines |
| 5.1.2 | Create Play Store screenshots (8 screens) | UX Designer | 4h | Screenshots with device frames and captions |
| 5.1.3 | Create feature graphic (1024x500) | UX Designer | 2h | Banner for Play Store listing |
| 5.1.4 | Write Play Store listing copy | Marketing | 3h | Title, short desc, full desc with keywords |
| 5.1.5 | Configure release signing | Android Developer | 2h | Upload key, Play App Signing configured |
| 5.1.6 | Create privacy policy page | Security Expert | 3h | Hosted privacy policy covering all data |
| 5.1.7 | Complete data safety form | Security Expert | 2h | All data collection disclosed |
| 5.1.8 | Build and upload release AAB | Android Developer | 1h | Signed release bundle uploaded to Play Console |

**Milestone Exit Criteria**:
- [ ] All Play Store assets uploaded
- [ ] Privacy policy live and linked
- [ ] AAB uploaded and validated

### Milestone 5.2: Beta Testing
**Goal**: Validate with real users, fix critical issues  
**Owner**: Product Manager

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 5.2.1 | Recruit 50 beta testers from waitlist | Marketing | 3h | 50 confirmed testers with diverse devices |
| 5.2.2 | Deploy to closed beta track | Android Developer | 1h | Beta track live with opt-in link |
| 5.2.3 | Create feedback form + add in-app feedback button | Product Manager | 2h | Google Form + simple in-app "Send Feedback" option |
| 5.2.4 | Run 1-week beta test | Product Manager | - | (Time passes) |
| 5.2.5 | Analyze beta feedback and crash reports | Product Manager | 3h | Prioritized issue list, top 5 critical bugs |
| 5.2.6 | Fix critical issues from beta | Android Developer | 4h | All P0 bugs fixed |
| 5.2.7 | Release beta update | Android Developer | 1h | v0.9.1 with fixes |
| 5.2.8 | Final beta validation | Product Manager | 2h | Confirm critical issues resolved, check crash-free rate |

**Milestone Exit Criteria**:
- [ ] 30+ beta testers provided feedback
- [ ] 0 P0 bugs remaining
- [ ] Crash-free rate >99%
- [ ] NPS ≥30 from beta testers

### Milestone 5.3: Launch
**Goal**: Successfully launch on Google Play Store  
**Owner**: Marketing + Product Manager

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 5.3.1 | Submit for production review | Android Developer | 1h | App submitted to production track |
| 5.3.2 | Prepare Product Hunt launch | Marketing | 3h | Maker profile, description, media ready |
| 5.3.3 | Prepare press release | Marketing | 2h | Press release drafted and distributed |
| 5.3.4 | Launch on Product Hunt | Marketing | 2h | Posted, engaging with comments |
| 5.3.5 | Execute social media campaign | Marketing | 2h | Posts on Reddit, Twitter, LinkedIn |
| 5.3.6 | Monitor launch day metrics | Product Manager | 4h | Track installs, crashes, reviews |
| 5.3.7 | Respond to reviews and feedback | Product Manager | 4h | Reply to all reviews within 24h |

**Milestone Exit Criteria**:
- [ ] App live on Play Store
- [ ] 1,000+ downloads in first week
- [ ] Rating ≥4.0 with 10+ reviews

---

## Phase 6: Post-Launch (Weeks 14+)

### Milestone 6.1: Stabilization
**Goal**: Maintain quality and respond to user feedback  
**Owner**: All

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 6.1.1 | Set up crash monitoring dashboard | Android Developer | 1h | Firebase Crashlytics dashboard reviewed |
| 6.1.2 | Establish weekly release cadence | Android Developer | 1h | Release process documented |
| 6.1.3 | Create user feedback triage process | Product Manager | 2h | Process for categorizing and prioritizing feedback |
| 6.1.4 | Fix top 3 issues from week 1 | Android Developer | 4h | Issues resolved in v1.0.1 |

### Milestone 6.2: Growth & Iteration
**Goal**: Grow user base and plan next features  
**Owner**: Product Manager + Marketing

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 6.2.1 | Set up analytics dashboard | Marketing | 3h | Key metrics tracked and visualized |
| 6.2.2 | Week 2 metrics analysis | Product Manager | 2h | Report on downloads, retention, engagement |
| 6.2.3 | Synthesize user feedback themes | Product Manager | 3h | Top 5 feature requests identified |
| 6.2.4 | Plan v1.1 features | Product Manager | 3h | Prioritized backlog for next release |
| 6.2.5 | ASO optimization round 1 | Marketing | 2h | Updated keywords based on search data |

**Milestone Exit Criteria**:
- [ ] v1.1 roadmap defined
- [ ] Crash-free rate >99%
- [ ] DAU growing week-over-week

---

## Summary

### Timeline Overview (MVP Only)

| Phase | Weeks | Key Deliverables |
|-------|-------|------------------|
| 0: Research | 1-2 | Market analysis, LLM selection, MVP PRD |
| 1: Design & Setup | 2-3 | UX specs, project architecture |
| 2: Core | 3-5 | Data layer, AI engine, design system |
| 3: Features | 5-10 | Tasks, Goals, Calendar, Briefings, Analytics |
| 4: Polish | 10-12 | Onboarding, notifications, testing |
| 5: Launch | 12-14 | Beta testing, Play Store launch |
| 6: Post-Launch | 14-16 | Stabilization, iteration |

*See [POST_MVP_ROADMAP.md](POST_MVP_ROADMAP.md) for v1.1 features, Cloud AI (Phase 7), and Custom Agents (Phase 8).*

### Resource Allocation (MVP)

| Role | Phase 0-1 | Phase 2-3 | Phase 4-5 | Phase 6 |
|------|-----------|-----------|-----------|---------|
| Product Manager | 100% | 50% | 80% | 60% |
| UX Designer | 80% | 30% | 20% | 10% |
| Android Developer | 80% | 100% | 100% | 80% |
| Backend Engineer | 20% | 10% | 10% | 10% |
| Marketing | 40% | 20% | 60% | 60% |
| Security Expert | 10% | 10% | 30% | 10% |

*Note: Backend Engineer is minimally involved in MVP (offline-first). Full allocation begins post-MVP for Cloud AI.*

### Total MVP Task Count

| Phase | Tasks | Estimated Hours |
|-------|-------|-----------------|
| Phase 0: Research | 18 | ~36h |
| Phase 1: Design & Setup | 27 | ~54h |
| Phase 2: Core | 28 | ~56h |
| Phase 3: Features | 33 | ~100h |
| Phase 4: Polish | 15 | ~35h |
| Phase 5: Launch | 16 | ~35h |
| Phase 6: Post-Launch | 9 | ~20h |
| **Total MVP** | **~146** | **~336h** |

*MVP scope optimized for faster delivery. Post-MVP tasks moved to [POST_MVP_ROADMAP.md](POST_MVP_ROADMAP.md).*

---

## Free Design Tool Alternatives

Instead of Figma, the team can use these free tools:

| Tool | Use Case | Notes |
|------|----------|-------|
| **Penpot** | Full design tool | Open source Figma alternative, browser-based |
| **Excalidraw** | Quick wireframes | Excellent for low-fi sketches, collaborative |
| **Canva** | Marketing assets | Free tier for screenshots, graphics |
| **Material Theme Builder** | Color system | Google's tool for Material 3 colors |
| **Lunacy** | Design tool | Free Figma alternative, Windows/Mac/Linux |

For MVP, **text-based specifications** are prioritized over visual mockups to reduce tool dependency and speed up development.

---

*Document Owner: Product Manager*
*Last Updated: February 2026*
*Status: Approved for Execution*
