# CLAUDE.md — Agent Orchestration for Prio

> This file configures Claude (Anthropic) as the primary AI agent for Prio development.
> It is loaded automatically by Claude Code, Cursor, Windsurf, and other tools that respect `CLAUDE.md`.

---

## Project Overview

**Prio** is a privacy-first Android personal assistant built around the Eisenhower Matrix. It uses on-device AI (Gemini Nano + llama.cpp + rule-based fallback) to classify tasks, generate briefings, and help users prioritize — all without cloud dependency.

- **Language**: Kotlin 2.2+ (K2 compiler)
- **UI**: Jetpack Compose (BOM 2025.01.01)
- **Architecture**: MVVM + Clean Architecture, multi-module
- **DI**: Hilt (Dagger 2.56)
- **Database**: Room 2.7 with SQLCipher
- **AI**: Gemini Nano (ML Kit), llama.cpp (JNI), rule-based fallback
- **Min SDK**: 29 (Android 10) | **Target SDK**: 34 | **Compile SDK**: 35

---

## Essential Context Files

Read these before making significant changes:

| Priority | File | When to Read |
|----------|------|-------------|
| 🔴 Always | `TEAM_AGENT.md` | Before any task — defines your role and constraints |
| 🔴 Always | `docs/CONVENTIONS.md` | Before writing any Kotlin code |
| 🟡 Feature work | `docs/ARCHITECTURE.md` | When touching architecture, adding modules |
| 🟡 Feature work | `docs/UX_DESIGN_SYSTEM.md` | When creating or modifying UI |
| 🟡 Feature work | `docs/ACTION_PLAN.md` | When prioritizing or planning work |
| 🟢 As needed | `docs/SECURITY_GUIDELINES.md` | When touching data, auth, or AI prompts |
| 🟢 As needed | `docs/DECISIONS.md` | When facing an architectural choice |
| 🟢 As needed | `docs/TODO.md` | When looking for bugs to fix or next tasks |
| 🟢 As needed | `docs/E2E_TEST_PLAN.md` | When writing or debugging E2E tests |

---

## Build & Test Commands

```bash
# IMPORTANT: Always run from android/ directory
cd android/

# Build
./gradlew :app:assembleDebug

# Unit tests
./gradlew :app:test

# All instrumented/E2E tests (requires connected device)
./gradlew :app:connectedDebugAndroidTest

# Single test class
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.prio.app.e2e.scenarios.SmokeE2ETest

# Lint
./gradlew :app:lintDebug

# Clean build (if gradle cache issues)
./gradlew clean :app:assembleDebug --no-configuration-cache
```

---

## Code Patterns

### Creating a New Feature

1. **UiState** — Data class in `feature/{name}/`
2. **ViewModel** — `@HiltViewModel` with `StateFlow<UiState>`
3. **Screen** — `@Composable` that collects state via `collectAsStateWithLifecycle()`
4. **Navigation** — Add route to `PrioNavigation.kt` and destination to `PrioNavHost.kt`
5. **Tests** — Unit test for ViewModel, Robot for E2E if needed

```kotlin
// ── Pattern: ViewModel ──
@HiltViewModel
class NewFeatureViewModel @Inject constructor(
    private val repository: SomeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewFeatureUiState())
    val uiState: StateFlow<NewFeatureUiState> = _uiState.asStateFlow()
    
    fun onEvent(event: NewFeatureEvent) {
        when (event) {
            is NewFeatureEvent.LoadData -> loadData()
            // ...
        }
    }
}

// ── Pattern: Screen ──
@Composable
fun NewFeatureScreen(
    viewModel: NewFeatureViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NewFeatureContent(uiState = uiState, onEvent = viewModel::onEvent)
}
```

### Creating a New Repository

1. **Entity** — Room entity in `core/data/local/entity/`
2. **DAO** — Room DAO in `core/data/local/dao/`
3. **Repository** — In `core/data/repository/`, inject DAO
4. **DI** — Bind in `core/data/di/RepositoryModule.kt`
5. **Migration** — Add Room migration if schema changes

### Using AI Providers

```kotlin
// ✅ CORRECT: Use AiProviderRouter
@Inject lateinit var aiRouter: AiProviderRouter
val response = aiRouter.complete(AiRequest(prompt = sanitizedInput))

// ❌ WRONG: Never call a provider directly
val response = geminiNanoProvider.complete(...)
```

---

## Key Architectural Rules

### DO
- ✅ Use `StateFlow` for UI state (never `LiveData`)
- ✅ Use `collectAsStateWithLifecycle()` in Compose
- ✅ Use Hilt `@Inject` for all dependencies
- ✅ Use `kotlinx.datetime` for date/time (never `java.util.Date`)
- ✅ Use `kotlinx.serialization` for JSON (never Gson/Moshi)
- ✅ Use Timber for logging: `Timber.d("message")`
- ✅ Handle errors gracefully — show `EmptyErrorState` composable
- ✅ Use version catalog (`libs.versions.toml`) for all dependencies
- ✅ Sanitize user input before passing to AI providers

### DON'T
- ❌ Add new dependencies without checking `libs.versions.toml` first
- ❌ Use `GlobalScope` or unstructured coroutines
- ❌ Store sensitive data in SharedPreferences (use DataStore + Keystore)
- ❌ Log PII (names, emails, task content) — even in debug
- ❌ Call AI providers directly — always go through `AiProviderRouter`
- ❌ Use `java.time` — use `kotlinx.datetime` for multiplatform readiness
- ❌ Create God-classes — keep classes < 300 lines
- ❌ Use magic numbers — define constants in companion objects or config

---

## Module Boundaries

```
:app          CAN depend on → :core:common, :core:ui, :core:data, :core:domain, :core:ai, :core:ai-provider
:core:domain  CAN depend on → :core:common, :core:data, :core:ai
:core:data    CAN depend on → :core:common
:core:ui      CAN depend on → :core:common
:core:ai      CAN depend on → :core:common
:core:ai-provider CAN depend on → :core:common, :core:ai

:core:data    MUST NOT depend on → :core:ui, :core:domain, :app
:core:ui      MUST NOT depend on → :core:data, :core:domain, :app
:core:domain  MUST NOT depend on → :core:ui, :app
```

---

## Common Tasks

### "Fix a bug from TODO.md"
1. Read `docs/TODO.md` to understand the bug
2. Search for relevant code using the bug's `[FEATURE]` tag
3. Write a failing unit test or E2E test that reproduces the bug
4. Fix the bug with minimal changes
5. Verify the test passes
6. Update `docs/TODO.md` to mark as `[FIXED]` and move to §TO VERIFY

### "Add a new feature"
1. Check `docs/ACTION_PLAN.md` for the relevant milestone and user story
2. Read the UX spec in `docs/results/1.1/` if it exists
3. Check `docs/DECISIONS.md` for any resolved debates about this feature
4. Follow the "Creating a New Feature" pattern above
5. Write unit tests (≥80% coverage for new code)
6. Add E2E test scenarios if it's a user-facing feature
7. Update `docs/ACTION_PLAN.md` status

### "Improve performance"
1. Read `docs/results/4.3/` for existing performance baselines
2. Profile with Android Studio Profiler
3. Focus on: startup time, scroll jank, memory leaks, AI latency
4. Targets: cold start <3s, AI response <2s, 60fps scrolling

---

## Session Management

### Starting a Session
1. State your role: "Acting as Principal Android Engineer"
2. Read the relevant context files (see table above)
3. Check `docs/TODO.md` for current priorities
4. Confirm the task with the user before starting

### During a Session
- Work on one task at a time
- Commit logical units (don't batch unrelated changes)
- Run tests after every change
- Update documentation as you go

### Ending a Session
- Summarize what was done
- List any follow-up tasks
- Update `docs/TODO.md` if new bugs/tasks were discovered
- Update `docs/ACTION_PLAN.md` if milestones progressed

---

## Git Conventions

```bash
# Commit format
<type>(<scope>): <short description>

# Types: feat, fix, refactor, test, docs, chore, perf, style
# Scopes: app, tasks, goals, calendar, briefing, capture, ai, data, ui, settings

# Examples
feat(tasks): add recurring task creation UI
fix(goals): archived goals now visible in goals list
test(e2e): add milestone regression tests
docs: update TODO with new bug findings
perf(ai): reduce Gemini Nano cold start by 200ms
refactor(data): extract TaskRepository from monolithic DAO
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Gradle sync fails | `cd android/ && ./gradlew clean --no-configuration-cache` |
| E2E tests fail to start | Ensure device connected: `adb devices` |
| Compose preview broken | Rebuild: `./gradlew :app:assembleDebug` |
| Hilt injection error | Check `@AndroidEntryPoint` on Activity/Fragment |
| Room migration crash | Add migration in `PrioDatabase.kt`, increment version |
| AI provider returns null | Check `AiProviderRouter` fallback chain |
| NDK build error (llama.cpp) | Verify NDK installed, check `CMakeLists.txt` paths |

---

*For team roles and responsibilities, see `TEAM_AGENT.md`.  
For coding conventions, see `docs/CONVENTIONS.md`.  
For architectural decisions, see `docs/DECISIONS.md`.*
