# Prio — Coding Conventions

> Source of truth for code style, naming, patterns, and file organization.
> All agents and contributors must follow these conventions.
> Last Updated: February 13, 2026

---

## Language & Style

### Kotlin
- **Version**: 2.2+ with K2 compiler
- **Style**: [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) + Android-specific rules below
- **Nullability**: Prefer non-null types. Use `?` only when null is a meaningful state.
- **Immutability**: Prefer `val` over `var`. Use `copy()` on data classes for mutations.
- **String templates**: Use `"$variable"` and `"${expression}"` — never `"" + variable`
- **Trailing commas**: Always use trailing commas in parameter lists and collections

### Formatting
- **Indentation**: 4 spaces (no tabs)
- **Max line length**: 120 characters
- **Blank lines**: One blank line between functions, two between sections
- **Imports**: No wildcard imports. Group by: Kotlin stdlib → Android → third-party → project

---

## Architecture Patterns

### MVVM + MVI Hybrid

The app uses **MVVM with MVI-style event/effect channels**:

```
Screen (Composable) → collects StateFlow<UiState>
                    → sends events via ViewModel.onEvent(Event)
                    → handles one-time Effects via LaunchedEffect

ViewModel           → holds MutableStateFlow<UiState>
                    → exposes StateFlow<UiState> and Flow<Effect>
                    → delegates to Repositories and UseCases

Repository          → wraps DAOs and returns Flow<Entity>
                    → suspend functions for mutations

DAO                 → Room interface with SQL queries
                    → Flow<> for reactive, suspend for one-shot
```

### File Organization

#### ViewModel (`feature/{name}/`)

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: SomeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    private val _effect = Channel<FeatureEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeData()
    }

    fun onEvent(event: FeatureEvent) {
        when (event) {
            is FeatureEvent.OnItemClick -> handleItemClick(event.id)
            is FeatureEvent.OnRefresh -> handleRefresh()
            // Each event dispatches to a private handler
        }
    }

    private fun handleItemClick(id: Long) {
        viewModelScope.launch {
            try {
                // business logic
                _effect.send(FeatureEffect.NavigateToDetail(id))
            } catch (e: Exception) {
                _effect.send(FeatureEffect.ShowSnackbar("Something went wrong"))
            }
        }
    }
}
```

**Rules:**
- `@HiltViewModel` + `@Inject constructor` — always
- `_uiState` / `uiState` naming convention (private mutable → public read-only)
- `_effect` / `effect` via `Channel<Effect>(Channel.BUFFERED)` + `.receiveAsFlow()`
- Single `onEvent(event)` entry point (MVI pattern)
- Private `handle*()` methods for each event
- `_uiState.update { it.copy(...) }` for state mutations
- `viewModelScope.launch` for coroutine work
- Inject `Clock` for testable time (never use `Clock.System` directly)

#### UiState File (`feature/{name}/FeatureUiState.kt`)

All UI-related types co-located in one file:

```kotlin
// ═══ State ═══
data class FeatureUiState(
    val items: List<ItemUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilter: Filter = Filter.All,
) {
    // Computed properties
    val isEmpty: Boolean get() = items.isEmpty()
}

// ═══ UI Model ═══
data class ItemUiModel(
    val id: Long,
    val title: String,
    val subtitle: String?,
)

// ═══ Events (user actions) ═══
sealed interface FeatureEvent {
    data class OnItemClick(val id: Long) : FeatureEvent       // parameterized
    data object OnRefresh : FeatureEvent                       // parameterless
}

// ═══ Effects (one-time side effects) ═══
sealed interface FeatureEffect {
    data class NavigateToDetail(val id: Long) : FeatureEffect
    data class ShowSnackbar(val message: String) : FeatureEffect
    data object ShowConfetti : FeatureEffect
}

// ═══ Enums ═══
enum class Filter(val displayName: String) { All("All"), Active("Active") }
```

**Rules:**
- All UiState fields have defaults → allows `FeatureUiState()`
- Computed properties via `val ... get() =` inside data class body
- Events: `sealed interface` with `On` prefix naming
- Effects: `sealed interface` for navigation, snackbars, animations
- `data class` for parameterized, `data object` for parameterless

#### Screen Composable (`feature/{name}/FeatureScreen.kt`)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-time effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FeatureEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
                is FeatureEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = { FeatureTopBar(...) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingContent()
            state.error != null -> EmptyErrorState(...)
            state.isEmpty -> EmptyContent(...)
            else -> FeatureContent(state, viewModel::onEvent)
        }
    }
}

// Private sub-composables
@Composable
private fun FeatureContent(
    state: FeatureUiState,
    onEvent: (FeatureEvent) -> Unit,
) { /* ... */ }
```

**Rules:**
- Top-level Screen: `fun FeatureScreen(viewModel = hiltViewModel(), onNavigate*: lambdas)`
- State: `val state by viewModel.uiState.collectAsStateWithLifecycle()`
- Effects: `LaunchedEffect(Unit) { viewModel.effect.collectLatest { ... } }`
- Navigation via lambda callbacks (never call NavController from Screen)
- Material 3 Scaffold with `paddingValues`
- Loading → Error → Empty → Content state handling
- Sub-composables are `private` functions in the same file
- Accessibility: `Modifier.semantics { }`, `Modifier.testTag(...)` on key elements

---

## Data Layer Patterns

### Entity (`core/data/local/entity/`)

```kotlin
@Entity(
    tableName = "items",  // snake_case, plural
    foreignKeys = [
        ForeignKey(
            entity = ParentEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index(value = ["parent_id"]), Index(value = ["created_at"])],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
```

**Rules:**
- Table names: `snake_case`, plural (`tasks`, `goals`, `milestones`)
- Column names: `snake_case` via `@ColumnInfo(name = "...")`
- Property names: `camelCase`
- Primary key: `@PrimaryKey(autoGenerate = true) val id: Long = 0`
- Timestamps: `kotlinx.datetime.Instant` (never `Long` or `java.time`)
- Foreign keys: explicit `onDelete` behavior
- Indices on frequently queried / joined columns

### DAO (`core/data/local/dao/`)

```kotlin
@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET title = :title, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updatedAt: Instant)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Reactive query → Flow (no suspend)
    @Query("SELECT * FROM items ORDER BY created_at DESC")
    fun getAll(): Flow<List<ItemEntity>>

    // One-shot query → suspend
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    // Flow variant for observing single item
    @Query("SELECT * FROM items WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<ItemEntity?>
}
```

**Rules:**
- `@Dao interface` (not abstract class)
- `@Insert(onConflict = OnConflictStrategy.REPLACE)` as default
- Reactive reads: non-suspend `fun` → `Flow<...>`
- One-shot reads: `suspend fun` → `Entity?` or `List<Entity>`
- Writes: always `suspend fun`
- Targeted updates: `@Query("UPDATE ...")` for partial field changes

### Repository (`core/data/repository/`)

```kotlin
@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val clock: Clock = Clock.System,
) {
    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAll()
    suspend fun getItemById(id: Long): ItemEntity? = itemDao.getById(id)
    
    suspend fun createItem(title: String): Long {
        val now = clock.now()
        return itemDao.insert(ItemEntity(title = title, createdAt = now, updatedAt = now))
    }
}
```

**Rules:**
- `@Singleton` with `@Inject constructor`
- Inject `Clock` for testable time
- Thin wrapper over DAO — no complex business logic (that goes in domain)
- Comment section headers for organization: `// ══ Query ══`, `// ══ Mutations ══`

---

## Domain Layer Patterns

### Use Cases / Engine (`core/domain/`)

```kotlin
@Singleton
class FeatureEngine @Inject constructor(
    private val clock: Clock,
) {
    companion object {
        private const val TAG = "FeatureEngine"
        const val SOME_THRESHOLD = 0.65f

        object Thresholds {
            const val HIGH = 0.75f
            const val MEDIUM = 0.50f
            const val LOW = 0.25f
        }
    }

    fun classify(input: String): Classification { /* pure logic */ }
}
```

**Rules:**
- `@Singleton` with `@Inject constructor`
- Constants in `companion object` (no magic numbers anywhere)
- Nested `object` for grouped constants
- Pure Kotlin logic — no Android dependencies
- Only `kotlinx.datetime` for time

---

## Navigation

### Routes (`navigation/PrioNavigation.kt`)

```kotlin
sealed interface PrioRoute {
    @Serializable data object Today : PrioRoute           // parameterless
    @Serializable data object Tasks : PrioRoute
    @Serializable data class TaskDetail(val taskId: Long) : PrioRoute  // parameterized
    @Serializable data class GoalDetail(val goalId: Long) : PrioRoute
}
```

**Rules:**
- Type-safe navigation via `kotlinx.serialization`
- `sealed interface PrioRoute` as root
- `@Serializable data object` for screens without parameters
- `@Serializable data class` for screens with parameters
- All routes in `PrioNavigation.kt`
- Destinations registered in `PrioNavHost.kt`

---

## Dependency Injection

### Hilt Modules (`*/di/`)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SomeModule {
    @Provides @Singleton
    fun provideSomething(): Something = Something()
}
```

**Rules:**
- Modules are `object` (not `class`)
- `@InstallIn(SingletonComponent::class)` for app-scoped
- ViewModels: `@HiltViewModel` + `@Inject constructor` (no module needed)
- Domain classes: `@Singleton` + `@Inject constructor` (no module needed)
- Database/DAOs: explicit `@Provides` in `DatabaseModule`
- Repositories: explicit `@Provides` in `RepositoryModule`

---

## Testing

### Unit Tests (JUnit 5 + MockK + Turbine)

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("FeatureViewModel")
class FeatureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: SomeRepository = mockk(relaxed = true)
    private val testClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-04-15T10:00:00Z")
    }
    private lateinit var viewModel: FeatureViewModel

    private fun createItem(id: Long = 1L, title: String = "Test") = ItemEntity(...)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getAllItems() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = FeatureViewModel(repository, testClock)
    }

    @Nested
    @DisplayName("Loading")
    inner class Loading {
        @Test
        @DisplayName("starts with empty state")
        fun startsWithEmptyState() = runTest {
            createViewModel()
            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.items.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
```

**Rules:**
- **JUnit 5** — `@Test`, `@BeforeEach`, `@AfterEach`, `@Nested`, `@DisplayName`
- **MockK** — `mockk(relaxed = true)`, `every { }`, `coEvery { }`
- **Turbine** — `flow.test { awaitItem(), cancelAndIgnoreRemainingEvents() }`
- **Coroutines test** — `StandardTestDispatcher()`, `runTest { }`, `advanceUntilIdle()`
- `Dispatchers.setMain(testDispatcher)` in setup, `resetMain()` in teardown
- `createViewModel()` as lazy factory (not in `@BeforeEach`)
- `create*()` helper functions for test data with defaults
- `testClock` as anonymous `object : Clock` for deterministic time
- `@Nested` inner classes to group related tests
- Method names: descriptive camelCase (`startsWithEmptyState`)

### E2E Tests (Compose UI Testing + Robot Pattern)

```kotlin
// Robot
class TaskListRobot(private val rule: ComposeTestRule) {
    fun assertTaskVisible(title: String) {
        rule.onNodeWithText(title).assertIsDisplayed()
    }
    fun clickTask(title: String) {
        rule.onNodeWithText(title).performClick()
    }
}

// Scenario
class TaskListE2ETest : BaseE2ETest() {
    @Test
    fun createAndCompleteTask() {
        taskListRobot.assertScreenVisible()
        quickCaptureRobot.createTask("Buy groceries")
        taskListRobot.assertTaskVisible("Buy groceries")
    }
}
```

**Rules:**
- Robot Pattern: each screen has a Robot class with assertion/action methods
- Scenarios: describe user journeys, compose Robots
- Never use raw `onNodeWithText()` in scenarios — always via Robot
- `BaseE2ETest` handles Hilt setup and common infrastructure

---

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Package | `lowercase.dot.separated` | `com.prio.app.feature.tasks` |
| Class | `PascalCase` | `TaskListViewModel` |
| Function | `camelCase` | `onTaskClick()` |
| Property | `camelCase` | `isLoading` |
| Constant | `SCREAMING_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Private backing | `_prefixCamelCase` | `_uiState` |
| DB table | `snake_case_plural` | `tasks`, `daily_analytics` |
| DB column | `snake_case` | `due_date`, `created_at` |
| Compose Screen | `FeatureNameScreen` | `TaskListScreen` |
| Compose Content | `FeatureNameContent` | `TaskListContent` |
| ViewModel | `FeatureNameViewModel` | `TaskListViewModel` |
| UiState | `FeatureNameUiState` | `TaskListUiState` |
| Event | `FeatureNameEvent` | `TaskListEvent` |
| Effect | `FeatureNameEffect` | `TaskListEffect` |
| Robot (test) | `FeatureNameRobot` | `TaskListRobot` |
| E2E Test | `FeatureNameE2ETest` | `TaskListE2ETest` |
| Test method | `descriptiveCamelCase` | `startsWithEmptyState` |

---

## Commit Messages

```
<type>(<scope>): <short description>

# Types
feat     — New feature
fix      — Bug fix
refactor — Code change that neither fixes a bug nor adds a feature
test     — Adding or updating tests
docs     — Documentation only
chore    — Build, CI, tooling changes
perf     — Performance improvement
style    — Formatting, whitespace (no logic change)

# Scopes
app, tasks, goals, calendar, briefing, capture, ai, data, ui, settings,
insights, meeting, onboarding, navigation, worker, e2e

# Examples
feat(tasks): add recurring task creation UI
fix(goals): archived goals now visible in goals list
test(e2e): add milestone regression tests
docs: update TODO with new bug findings
perf(ai): reduce Gemini Nano cold start by 200ms
refactor(data): extract analytics queries to dedicated DAO
```

---

## Dependency Management

### Version Catalog (`android/gradle/libs.versions.toml`)

All dependencies MUST be declared in the version catalog:

```toml
[versions]
kotlin = "2.2.0"
compose-bom = "2025.01.01"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**Rules:**
- Never add a dependency directly in `build.gradle.kts` — always via catalog
- Check if a similar dependency already exists before adding a new one
- Group related dependencies into bundles
- Pin exact versions (no `+` or ranges)

---

## Accessibility Checklist

Every screen must satisfy:

- [ ] All interactive elements have `contentDescription`
- [ ] Touch targets ≥ 48dp × 48dp
- [ ] Heading semantics: `Modifier.semantics { heading() }`
- [ ] Screen reader order matches visual order
- [ ] Sufficient color contrast (4.5:1 text, 3:1 large text)
- [ ] Works in both light and dark mode
- [ ] `testTag()` on key elements for E2E testing
- [ ] No information conveyed by color alone

---

## Performance Guidelines

| Metric | Target |
|--------|--------|
| Cold start | < 3 seconds |
| AI classification (rule-based) | < 50ms |
| AI classification (Gemini Nano) | < 2 seconds |
| Screen transition | < 300ms |
| List scroll | 60 fps (no jank) |
| Memory (idle) | < 150 MB |
| Battery (1hr active use) | < 5% |

**Optimization rules:**
- Use `LazyColumn` / `LazyRow` for lists (never `Column` with `forEach`)
- Use `remember` and `derivedStateOf` to avoid recomposition
- Use `key()` in lazy lists
- Minimize `@Composable` function parameters (avoid unstable types)
- Use `Immutable` collections where possible

---

*When in doubt, look at existing code in the codebase and follow the established pattern.
The task list feature (`feature/tasks/`) is the most complete reference implementation.*
