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
**Owner**: Backend Engineer + Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 0.2.1 | Set up llama.cpp Android test project | Backend Engineer | 3h | Working Android project that can load a GGUF model |
| 0.2.2 | Benchmark Phi-3-mini-4k-instruct (Q4_K_M) on 3 reference devices | Android Developer | 4h | Performance report: tokens/sec, memory usage, first inference time on Pixel 6, Samsung A54, older device |
| 0.2.3 | Benchmark Gemma-2B (Q4_K_M) on same 3 devices | Android Developer | 3h | Same metrics as 0.2.2 for comparison |
| 0.2.4 | Test task categorization accuracy with 20 sample prompts | Backend Engineer | 2h | Accuracy report: % correct Eisenhower classification for each model |
| 0.2.5 | Document memory/storage requirements and device compatibility | Android Developer | 2h | Compatibility matrix: minimum device specs, storage needs, recommended devices |
| 0.2.6 | Write LLM selection recommendation with fallback strategy | Backend Engineer | 2h | 2-page technical recommendation doc with primary model, fallback approach |

**Milestone Exit Criteria**:
- [ ] Both models benchmarked on 3+ device tiers
- [ ] Task categorization accuracy >80%
- [ ] Clear model recommendation documented

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
| 1.2.5 | Set up Room database with SQLCipher encryption | Android Developer | 3h | Encrypted database initializes, sample entity CRUD works |
| 1.2.6 | Configure DataStore for preferences | Android Developer | 1h | UserPreferences DataStore created with sample preferences |
| 1.2.7 | Set up Compose navigation with type-safe routes | Android Developer | 2h | Navigation graph with 5+ placeholder destinations |
| 1.2.8 | Create Material 3 theme (colors, typography) | Android Developer | 2h | Theme applied, light/dark mode working |
| 1.2.9 | Set up testing infrastructure | Android Developer | 2h | JUnit 5, MockK, Turbine configured, sample test passing |
| 1.2.10 | Configure GitHub Actions CI | Backend Engineer | 3h | CI runs on PR: build, lint, test |

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
**Owner**: Backend Engineer + Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 2.2.1 | Integrate llama.cpp via JNI | Backend Engineer | 4h | JNI wrapper compiles, basic inference works in test app |
| 2.2.2 | Create LlmEngine interface | Backend Engineer | 2h | Interface with generateResponse(), classifyTask(), extractEntities() |
| 2.2.3 | Implement LlamaCppEngine | Backend Engineer | 3h | Implementation using JNI wrapper, handles threading |
| 2.2.4 | Create ModelManager for download/verification | Android Developer | 4h | Downloads model with progress, verifies SHA256, resumes partial downloads |
| 2.2.5 | Implement streaming inference | Backend Engineer | 3h | Token-by-token streaming with Flow |
| 2.2.6 | Create RuleBasedFallback engine | Android Developer | 3h | Regex-based task parsing, keyword importance detection |
| 2.2.7 | Write Eisenhower classification prompts | Backend Engineer | 2h | Prompt template with 90%+ accuracy on 20 test cases |
| 2.2.8 | Write task parsing prompts | Backend Engineer | 2h | Prompt extracts: title, date, time, priority from natural language |
| 2.2.9 | Write briefing generation prompts | Backend Engineer | 2h | Prompt generates concise morning/evening summaries |
| 2.2.10 | Performance test: inference under 2 seconds | Android Developer | 2h | Benchmark suite, 95%+ queries complete in <2s on target devices |
| 2.2.11 | Write AI engine tests | Backend Engineer | 2h | Unit tests for prompt processing, mock-based inference tests |

**Milestone Exit Criteria**:
- [ ] LLM inference working on device
- [ ] Model download with resume support
- [ ] Fallback engine available for low-end devices
- [ ] All prompts achieve target accuracy

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
| 3.1.3 | Implement optional Matrix View toggle | Android Developer | 3h | 2x2 grid view as optional visualization, tap to filter |
| 3.1.4 | Implement Task Detail bottom sheet | Android Developer | 3h | All fields editable, goal linking, delete confirmation |
| 3.1.5 | Implement Quick Capture with AI | Android Developer | 4h | Voice/text input → AI parsing → preview → create |
| 3.1.6 | Implement drag-and-drop reordering | Android Developer | 3h | Reorder within list, haptic feedback |
| 3.1.7 | Implement swipe actions | Android Developer | 2h | Swipe right = complete, swipe left = delete |
| 3.1.8 | Implement task filters and search | Android Developer | 2h | Filter by quadrant, status, date range; full-text search |
| 3.1.9 | Implement recurring tasks | Android Developer | 3h | RRULE support, next occurrence calculation |
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
| 3.3.4 | Implement meeting notes editor | Android Developer | 3h | Rich text notes with auto-save |
| 3.3.5 | Implement AI action item extraction | Backend Engineer | 3h | Extract action items from notes → create tasks |
| 3.3.6 | Implement meeting checklist/agenda | Android Developer | 2h | Checklist items, completion tracking |
| 3.3.7 | Write UI tests for Calendar plugin | Android Developer | 2h | 6+ UI test cases |

**Milestone Exit Criteria**:
- [ ] Calendar events display correctly
- [ ] Meeting notes persist
- [ ] Action items extractable and convertible to tasks

### Milestone 3.4: Daily Briefings
**Goal**: AI-generated morning and evening summaries  
**Owner**: Backend Engineer + Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 3.4.1 | Implement BriefingGenerator | Backend Engineer | 4h | Generates briefing from tasks, calendar, goals |
| 3.4.2 | Implement Morning Briefing screen | Android Developer | 3h | Today's priorities, schedule, goal check-ins |
| 3.4.3 | Implement Evening Summary screen | Android Developer | 3h | Completed vs planned, moved items, insights |
| 3.4.4 | Implement briefing notifications | Android Developer | 2h | Configurable morning/evening notification times |
| 3.4.5 | Implement Today/Dashboard screen | Android Developer | 4h | Combined view: briefing + top tasks + calendar + goals |
| 3.4.6 | Write tests for briefing generation | Backend Engineer | 2h | Unit tests with various data scenarios |

**Milestone Exit Criteria**:
- [ ] Briefings generate in <3 seconds
- [ ] Briefings are contextual and actionable
- [ ] Notifications trigger at configured times

### Milestone 3.5: Analytics & Insights
**Goal**: Productivity analytics with 80/20 insights  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 3.5.1 | Implement analytics data collection | Android Developer | 3h | Track task creation, completion, timing |
| 3.5.2 | Implement Insights Dashboard | Android Developer | 4h | Weekly/monthly views, key metrics |
| 3.5.3 | Implement task completion chart | Android Developer | 3h | Bar/line chart of completions over time |
| 3.5.4 | Implement goal progress chart | Android Developer | 2h | Progress visualization per goal |
| 3.5.5 | Implement missed deadline analysis | Android Developer | 3h | Identify patterns in missed deadlines |
| 3.5.6 | Implement 80/20 insights | Backend Engineer | 3h | Identify which 20% of activities drive 80% of goal progress |
| 3.5.7 | Write tests for analytics | Android Developer | 2h | Calculation accuracy tests |

**Milestone Exit Criteria**:
- [ ] Key metrics displayed accurately
- [ ] Charts render correctly
- [ ] 80/20 insights identify high-impact activities

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

### Milestone 4.2: Notifications & Widgets
**Goal**: Proactive engagement through notifications and widgets  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 4.2.1 | Implement notification channels | Android Developer | 2h | Channels for reminders, briefings, nudges |
| 4.2.2 | Implement smart nudge system | Android Developer | 3h | Contextual nudges for overdue/important tasks |
| 4.2.3 | Implement home screen widget (Glance) | Android Developer | 4h | Shows top 3 tasks, quick capture button |
| 4.2.4 | Implement quick settings tile | Android Developer | 2h | Tile for quick task capture |
| 4.2.5 | Write notification/widget tests | Android Developer | 2h | Verify notification content and timing |

**Milestone Exit Criteria**:
- [ ] Notifications appear correctly on all Android versions
- [ ] Widget displays and updates properly
- [ ] Quick settings tile works

### Milestone 4.3: Performance & Testing
**Goal**: Meet all performance targets, comprehensive test coverage  
**Owner**: Android Developer + Backend Engineer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 4.3.1 | Profile and optimize app performance | Android Developer | 4h | Identify and fix top 5 performance issues |
| 4.3.2 | Optimize LLM memory usage | Backend Engineer | 4h | Peak memory <800MB during inference |
| 4.3.3 | Optimize battery usage | Android Developer | 3h | Background battery <1% per hour |
| 4.3.4 | Optimize cold start time | Android Developer | 3h | Cold start <3s on mid-range device |
| 4.3.5 | Create E2E test suite | Android Developer | 4h | 20+ E2E tests covering main user journeys |
| 4.3.6 | Test on 5+ device configurations | Android Developer | 4h | Test matrix: Pixel, Samsung, Xiaomi, different RAM |
| 4.3.7 | Accessibility audit and fixes | Android Developer | 3h | TalkBack works, contrast passes, touch targets ≥48dp |

**Milestone Exit Criteria**:
- [ ] Cold start <3s
- [ ] LLM inference <2s
- [ ] E2E tests pass
- [ ] 5+ devices tested

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
| 6.1.1 | Set up crash monitoring dashboard | Backend Engineer | 2h | Sentry/Crashlytics dashboard configured |
| 6.1.2 | Establish weekly release cadence | Android Developer | 2h | Release process documented |
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

## Post-MVP Roadmap: Cloud AI & Custom Agents

After MVP launch, these features are prioritized for the Pro tier.

### Phase 7: Cloud AI Integration (Post-MVP, Weeks 16-20)

#### Milestone 7.1: AI Gateway Backend
**Goal**: Build secure AI gateway to route requests to cloud providers  
**Owner**: Backend Engineer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 7.1.1 | Design AI Gateway API specification | Backend Engineer | 3h | OpenAPI spec with endpoints for all providers |
| 7.1.2 | Implement authentication middleware | Backend Engineer | 3h | JWT validation, rate limiting working |
| 7.1.3 | Create OpenAI adapter (GPT-4o, GPT-4o-mini) | Backend Engineer | 4h | Working adapter with streaming support |
| 7.1.4 | Create Anthropic adapter (Claude family) | Backend Engineer | 4h | Sonnet, Haiku support with streaming |
| 7.1.5 | Create Google adapter (Gemini 1.5) | Backend Engineer | 4h | Pro and Flash variants working |
| 7.1.6 | Create xAI adapter (Grok) | Backend Engineer | 3h | Grok-2 support |
| 7.1.7 | Implement model router with failover | Backend Engineer | 3h | Automatic failover when provider down |
| 7.1.8 | Implement usage tracking and cost calculation | Backend Engineer | 4h | Per-request cost tracking, usage reports |
| 7.1.9 | Implement quota management | Backend Engineer | 3h | Monthly limits, overage handling |
| 7.1.10 | Load testing and optimization | Backend Engineer | 3h | Gateway handles 100 req/s |

#### Milestone 7.2: Android Cloud AI Integration
**Goal**: Integrate cloud AI selection into Android app  
**Owner**: Android Developer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 7.2.1 | Create HybridAiRouter implementation | Android Developer | 4h | Routes to on-device or cloud based on settings |
| 7.2.2 | Implement model selection UI | Android Developer | 3h | Settings screen with model picker |
| 7.2.3 | Implement feature-specific model defaults | Android Developer | 2h | Different models per feature type |
| 7.2.4 | Implement usage dashboard | Android Developer | 4h | Show credits used, remaining, costs |
| 7.2.5 | Implement offline fallback | Android Developer | 2h | Graceful degradation when offline |
| 7.2.6 | Write tests for hybrid routing | Android Developer | 2h | Unit and integration tests |

### Phase 8: Custom AI Agents (Post-MVP, Weeks 20-24)

#### Milestone 8.1: Agent System Backend
**Goal**: Build agent storage, execution, and marketplace infrastructure  
**Owner**: Backend Engineer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 8.1.1 | Design agent data model schema | Backend Engineer | 2h | PostgreSQL schema for agents |
| 8.1.2 | Implement agent CRUD API | Backend Engineer | 3h | REST endpoints for agent management |
| 8.1.3 | Implement agent sync service | Backend Engineer | 3h | Sync agents across devices |
| 8.1.4 | Create built-in agent templates (7 templates) | Backend Engineer | 4h | PM, Career, Fitness, Learning, Finance, Writing, Home |
| 8.1.5 | Implement agent execution service | Backend Engineer | 4h | Build prompts, execute via AI gateway |
| 8.1.6 | Implement agent context injection | Backend Engineer | 3h | Inject user data respecting permissions |
| 8.1.7 | Design marketplace API | Backend Engineer | 2h | API for browsing/downloading community agents |

#### Milestone 8.2: Android Agent Builder
**Goal**: Create intuitive agent builder wizard in Android app  
**Owner**: Android Developer + UX Designer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 8.2.1 | Write UX spec for Agent Builder wizard | UX Designer | 3h | 5-step wizard spec with all states |
| 8.2.2 | Implement Step 1: Purpose selection | Android Developer | 3h | Category/custom purpose picker |
| 8.2.3 | Implement Step 2: Personality sliders | Android Developer | 3h | 3 sliders with preview text |
| 8.2.4 | Implement Step 3: Expertise & instructions | Android Developer | 3h | Domain tags, custom text input |
| 8.2.5 | Implement Step 4: Permissions | Android Developer | 2h | Checkbox permissions UI |
| 8.2.6 | Implement Step 5: Test & refine | Android Developer | 4h | Interactive chat test |
| 8.2.7 | Implement agent list and management | Android Developer | 3h | CRUD operations for agents |
| 8.2.8 | Implement goal-to-agent linking | Android Developer | 2h | Link agent when creating/editing goal |
| 8.2.9 | Implement agent chat UI | Android Developer | 4h | Dedicated chat screen per agent |
| 8.2.10 | Write tests for agent builder | Android Developer | 3h | UI and integration tests |

#### Milestone 8.3: Built-in Templates & UX
**Goal**: Polish built-in agents and template system  
**Owner**: Android Developer + UX Designer

| ID | Task | Owner | Duration | Measurable Outcome |
|----|------|-------|----------|-------------------|
| 8.3.1 | Design agent card and list UI | UX Designer | 2h | Agent card spec, list layout |
| 8.3.2 | Design agent chat UI | UX Designer | 3h | Chat bubble styles, action buttons |
| 8.3.3 | Implement template browsing | Android Developer | 3h | Browse/preview/create from template |
| 8.3.4 | Refine system prompts for all 7 templates | Backend Engineer | 4h | Quality prompts with testing |
| 8.3.5 | Implement proactive agent suggestions | Android Developer | 3h | Agents suggest tasks/reminders |
| 8.3.6 | End-to-end agent testing | Android Developer | 4h | Full workflow tests |

---

## Summary

### Timeline Overview

| Phase | Weeks | Key Deliverables |
|-------|-------|------------------|
| 0: Research | 1-2 | Market analysis, LLM selection, MVP PRD |
| 1: Design & Setup | 2-3 | UX specs, project architecture |
| 2: Core | 3-5 | Data layer, AI engine, design system |
| 3: Features | 5-10 | Tasks, Goals, Calendar, Briefings, Analytics |
| 4: Polish | 10-12 | Onboarding, notifications, widgets, testing |
| 5: Launch | 12-14 | Beta testing, Play Store launch |
| 6: Post-Launch | 14+ | Stabilization, iteration |
| **7: Cloud AI** | **16-20** | **AI Gateway, model selection, hybrid routing** |
| **8: Custom Agents** | **20-24** | **Agent builder, templates, marketplace** |

### Resource Allocation

| Role | Phase 0-1 | Phase 2-3 | Phase 4-5 | Phase 6 | Phase 7-8 |
|------|-----------|-----------|-----------|---------|-----------|
| Product Manager | 100% | 50% | 80% | 60% | 50% |
| UX Designer | 80% | 30% | 20% | 10% | 40% |
| Android Developer | 60% | 100% | 100% | 80% | 80% |
| Backend Engineer | 80% | 80% | 40% | 20% | 100% |
| Marketing | 40% | 20% | 60% | 60% | 30% |
| Security Expert | 10% | 10% | 30% | 10% | 20% |

### Total Task Count

- Phase 0: 21 tasks (+2 for quick validation & metrics)
- Phase 1: 24 tasks (+3 for quick design validation)
- Phase 2: 32 tasks
- Phase 3: 42 tasks
- Phase 4: 19 tasks
- Phase 5: 16 tasks (+1 for in-app feedback)
- Phase 6: 9 tasks
- **Phase 7: 16 tasks (Post-MVP)**
- **Phase 8: 17 tasks (Post-MVP)**
- **Total: 163 MVP tasks + 33 Post-MVP tasks = 196 tasks**

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
