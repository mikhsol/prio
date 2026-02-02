# Jeeves - Technical Architecture

## Overview

Jeeves is designed as an **offline-first, privacy-first** personal assistant with an Android-first architecture. The system prioritizes on-device processing, minimal backend dependency, and a pluggable module system. Core AI features run locally using on-device LLMs, enabling full functionality without internet connectivity.

---

## Architecture Principles

### Core Design Philosophy

1. **Offline-First**: All MVP features work without internet connectivity
2. **On-Device AI**: LLM inference runs locally on the smartphone
3. **Privacy-First**: Personal data never leaves the device unless user opts in
4. **Pluggable Architecture**: Features are modular plugins that can be added/removed
5. **Cost-Efficient**: Minimal backend infrastructure = minimal operational costs
6. **Cross-Platform Ready**: Kotlin Multiplatform for future iOS support

### 12-Factor App Compliance (Adapted for Mobile)
1. **Codebase**: Single repo with modular structure
2. **Dependencies**: Explicitly declared via Gradle catalogs
3. **Config**: BuildConfig and local properties, never hardcoded
4. **Backing Services**: Local Room DB as primary, cloud as optional
5. **Build, Release, Run**: CI/CD with signed releases
6. **Processes**: Single-process Android app with background workers
7. **Port Binding**: N/A for mobile (API server optional)
8. **Concurrency**: Kotlin Coroutines for async operations
9. **Disposability**: Fast app startup, graceful state restoration
10. **Dev/Prod Parity**: Debug/Release builds mirror production behavior
11. **Logs**: Structured logging with Timber
12. **Admin Processes**: Developer tools in debug builds only

---

## System Architecture

### High-Level Architecture (Offline-First)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ANDROID APPLICATION                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         PRESENTATION LAYER                              │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │   Jetpack    │  │   Widgets    │  │ Notifications │  │  Quick     │  │ │
│  │  │   Compose    │  │   (Glance)   │  │    Service    │  │  Settings  │  │ │
│  │  │   Screens    │  │              │  │               │  │   Tile     │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│  ┌────────────────────────────────┴───────────────────────────────────────┐ │
│  │                         DOMAIN LAYER                                    │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │  ViewModels  │  │   Use Cases  │  │   Domain     │  │  Plugin    │  │ │
│  │  │   (MVVM)     │  │              │  │   Models     │  │  Manager   │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│  ┌────────────────────────────────┴───────────────────────────────────────┐ │
│  │                          AI LAYER (ON-DEVICE)                           │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │   LLM        │  │   NLP        │  │   Task       │  │  Intent    │  │ │
│  │  │   Engine     │  │   Processor  │  │   Classifier │  │  Parser    │  │ │
│  │  │ (llama.cpp)  │  │              │  │              │  │            │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│  ┌────────────────────────────────┴───────────────────────────────────────┐ │
│  │                          DATA LAYER                                     │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │ Repositories │  │   Room DB    │  │  DataStore   │  │  Calendar  │  │ │
│  │  │              │  │  (SQLite)    │  │ (Preferences)│  │  Provider  │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│  ┌────────────────────────────────┴───────────────────────────────────────┐ │
│  │                          PLUGIN SYSTEM                                  │ │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐          │ │
│  │  │   Tasks    │ │   Goals    │ │  Calendar  │ │  [Future]  │          │ │
│  │  │   Plugin   │ │   Plugin   │ │   Plugin   │ │  Plugins   │          │ │
│  │  └────────────┘ └────────────┘ └────────────┘ └────────────┘          │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                          ┌──────────┴──────────┐
                          │   OPTIONAL CLOUD    │
                          │   (Sync & Backup)   │
                          └─────────────────────┘
```

### Optional Backend Architecture (Minimal)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          OPTIONAL BACKEND SERVICES                           │
│                        (Only for Premium Features)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   Auth Service   │  │   Sync Service   │  │   Backup Service  │          │
│  │      (Go)        │  │     (Rust)       │  │       (Go)        │          │
│  │                  │  │                  │  │                   │          │
│  │  • OAuth 2.0     │  │  • E2E Encrypted │  │  • Encrypted      │          │
│  │  • JWT tokens    │  │  • CRDT sync     │  │    blob storage   │          │
│  │  • Device mgmt   │  │  • Conflict res  │  │  • Point-in-time  │          │
│  └────────┬─────────┘  └────────┬─────────┘  └─────────┬─────────┘          │
│           │                     │                       │                    │
│  ┌────────┴─────────┐          │                       │                    │
│  │   AI Gateway     │          │                       │                    │
│  │     (Rust)       │──────────┴───────────────────────┘                    │
│  │                  │                                                        │
│  │  • Model routing │                                                        │
│  │  • Usage tracking│                                                        │
│  │  • Rate limiting │                                                        │
│  │  • Cost control  │                                                        │
│  └────────┬─────────┘                                                        │
│           │                                                                  │
│           ├─────────────────────┬───────────────────────┐                    │
│           ▼                     ▼                       ▼                    │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   OpenAI API     │  │  Anthropic API   │  │   Google API     │          │
│  │   (GPT-4o)       │  │  (Claude family) │  │   (Gemini)       │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│           │                                                                  │
│           ├───────────────────────────────────────────────┐                  │
│           ▼                                               ▼                  │
│  ┌──────────────────┐                         ┌──────────────────┐          │
│  │    xAI API       │                         │     Redis        │          │
│  │    (Grok)        │                         │  (Usage cache,   │          │
│  └──────────────────┘                         │   rate limits)   │          │
│                                               └──────────────────┘          │
│                                 │                                            │
│                     ┌───────────┴───────────┐                                │
│                     │     PostgreSQL        │                                │
│                     │   (User accounts,     │                                │
│                     │    sync metadata,     │                                │
│                     │    AI usage, agents)  │                                │
│                     └───────────────────────┘                                │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## On-Device LLM Architecture

### LLM Engine Design

The on-device LLM is the core intelligence layer, enabling natural language understanding without cloud dependencies.

```
┌─────────────────────────────────────────────────────────────────┐
│                      ON-DEVICE AI ENGINE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    LLM INFERENCE ENGINE                    │   │
│  │                                                            │   │
│  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────┐  │   │
│  │  │  Model Loader  │  │   Tokenizer    │  │  Inference  │  │   │
│  │  │  (GGUF format) │  │                │  │   Runtime   │  │   │
│  │  └────────────────┘  └────────────────┘  └─────────────┘  │   │
│  │                                                            │   │
│  │  Runtime Options:                                          │   │
│  │  • llama.cpp (via JNI) - Primary                          │   │
│  │  • MediaPipe LLM Inference - Alternative                  │   │
│  │  • ONNX Runtime - Fallback                                │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│  ┌──────────────────────────┴───────────────────────────────┐   │
│  │                    TASK-SPECIFIC MODELS                    │   │
│  │                                                            │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │   │
│  │  │   Main LLM   │  │ Intent Model │  │  Entity Extract  │ │   │
│  │  │  (Phi-3-mini │  │   (DistilBERT│  │  (NER model,     │ │   │
│  │  │   or Gemma)  │  │    fine-tuned│  │   ~10MB)         │ │   │
│  │  │   ~1.5-2GB   │  │    ~100MB)   │  │                  │ │   │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘ │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│  ┌──────────────────────────┴───────────────────────────────┐   │
│  │                    PROMPT ENGINEERING                      │   │
│  │                                                            │   │
│  │  • System prompts for task categorization                 │   │
│  │  • Eisenhower matrix classification prompts               │   │
│  │  • Goal decomposition templates                           │   │
│  │  • Meeting summary extraction                             │   │
│  │  • Action item identification                             │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Model Selection Criteria

| Model | Size | RAM Required | Speed (token/s) | Quality | Recommendation |
|-------|------|--------------|-----------------|---------|----------------|
| Phi-3-mini (Q4) | 1.8GB | 3GB | 15-25 | Good | ✅ Primary choice |
| Gemma 2B (Q4) | 1.5GB | 2.5GB | 20-30 | Good | ✅ Alternative |
| TinyLlama (Q4) | 0.6GB | 1.5GB | 30-40 | Medium | Fallback for low-end |
| Qwen2-0.5B | 0.5GB | 1GB | 40+ | Basic | Ultra-low-end |

### Model Download Strategy

```kotlin
sealed class ModelState {
    object NotDownloaded : ModelState()
    data class Downloading(val progress: Float) : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

class ModelManager {
    // Download in background, non-blocking
    // Resume support for interrupted downloads
    // Model integrity verification (SHA256)
    // Fallback to smaller model if space constrained
}
```

### Fallback Strategy

For devices that cannot run LLM inference:

1. **Rule-based NLP**: Regex + keyword matching for task parsing
2. **Local ML Kit**: Google ML Kit for entity extraction
3. **Simplified UI**: Direct input fields instead of natural language

---

## Cloud AI Architecture (Post-MVP - Premium)

For users who want access to more powerful models, Jeeves offers optional cloud AI integration.

### AI Gateway Design

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AI GATEWAY (Rust)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        REQUEST HANDLER                                 │   │
│  │  • JWT authentication                                                 │   │
│  │  • Rate limiting (per user, per minute)                              │   │
│  │  • Usage quota enforcement                                           │   │
│  │  • Request validation & sanitization                                 │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        MODEL ROUTER                                    │   │
│  │                                                                        │   │
│  │  • Route to user-selected model provider                              │   │
│  │  • Automatic failover if provider unavailable                         │   │
│  │  • Cost optimization (prefer cheaper models when appropriate)         │   │
│  │  • Response streaming support                                         │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌───────────┬───────────┬────────┴───┬─────────────┬──────────────────┐   │
│  │  OpenAI   │ Anthropic │   Google   │    xAI      │   Fallback       │   │
│  │  Adapter  │  Adapter  │   Adapter  │   Adapter   │   (On-device)    │   │
│  └───────────┴───────────┴────────────┴─────────────┴──────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Supported Cloud Models

| Provider | Models | Best For | Pricing Tier |
|----------|--------|----------|--------------|
| **OpenAI** | GPT-4o, GPT-4o-mini | General purpose, fast | $$ |
| **Anthropic** | Claude 3.5 Sonnet, Claude 3 Haiku | Analysis, safety | $$-$$$ |
| **Google** | Gemini 1.5 Pro, Gemini 1.5 Flash | Multimodal, long context | $$ |
| **xAI** | Grok-2, Grok-2 mini | Creative, real-time info | $$ |

### Hybrid AI Routing

```kotlin
interface AiRouter {
    suspend fun route(request: AiRequest): AiResponse
}

class HybridAiRouter(
    private val onDeviceEngine: LlmEngine,
    private val cloudGateway: CloudAiGateway,
    private val preferences: UserPreferences
) : AiRouter {
    
    override suspend fun route(request: AiRequest): AiResponse {
        // Decision logic for on-device vs cloud
        val useCloud = when {
            // Always use on-device if offline
            !networkAvailable -> false
            
            // User explicitly disabled cloud
            !preferences.cloudAiEnabled -> false
            
            // User has no quota remaining
            preferences.cloudQuotaRemaining <= 0 -> false
            
            // Complex requests -> cloud (if user allows)
            request.complexity == Complexity.HIGH -> true
            
            // Custom agent requests -> cloud
            request.agentId != null -> true
            
            // Simple requests -> on-device (faster, free)
            else -> false
        }
        
        return if (useCloud) {
            cloudGateway.invoke(request, preferences.preferredModel)
        } else {
            onDeviceEngine.invoke(request)
        }
    }
}

data class AiRequest(
    val prompt: String,
    val taskType: TaskType,
    val complexity: Complexity,
    val agentId: String? = null,  // For custom agents
    val context: Map<String, Any> = emptyMap()
)

enum class Complexity { LOW, MEDIUM, HIGH }
enum class TaskType { CATEGORIZE, SUMMARIZE, GENERATE, ANALYZE, CHAT }
```

### Cost Control

```kotlin
data class UsageQuota(
    val monthlyLimitUsd: Float = 5.00f,  // Default $5/month
    val currentUsageUsd: Float = 0f,
    val requestsThisMonth: Int = 0,
    val resetDate: LocalDate
)

class CostController(
    private val usageRepository: UsageRepository
) {
    suspend fun canMakeRequest(estimatedCostUsd: Float): Boolean {
        val quota = usageRepository.getCurrentQuota()
        return quota.currentUsageUsd + estimatedCostUsd <= quota.monthlyLimitUsd
    }
    
    suspend fun recordUsage(request: AiRequest, response: AiResponse) {
        val cost = calculateCost(request, response)
        usageRepository.addUsage(cost)
    }
    
    private fun calculateCost(request: AiRequest, response: AiResponse): Float {
        val inputTokens = request.prompt.tokenCount()
        val outputTokens = response.text.tokenCount()
        return getPricing(request.model).calculate(inputTokens, outputTokens)
    }
}
```

---

## Custom AI Agents Architecture (Post-MVP - Premium)

### Agent System Design

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CUSTOM AI AGENTS SYSTEM                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        AGENT REGISTRY                                  │   │
│  │                                                                        │   │
│  │  • Store agent configurations (local + sync)                          │   │
│  │  • Manage agent lifecycle (create, update, delete)                    │   │
│  │  • Link agents to goals                                               │   │
│  │  • Track agent usage and effectiveness                                │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        AGENT EXECUTOR                                  │   │
│  │                                                                        │   │
│  │  • Build prompts from agent configuration                             │   │
│  │  • Inject user context (goals, tasks, history)                        │   │
│  │  • Execute via AI Router (cloud or on-device)                         │   │
│  │  • Parse and validate agent responses                                 │   │
│  │                                                                        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────┬──────────────┬──────────────┬────────────────────────┐   │
│  │ Built-in     │ User-Created │ Marketplace  │ Goal-Linked            │   │
│  │ Agents       │ Agents       │ Agents       │ Agents                 │   │
│  └──────────────┴──────────────┴──────────────┴────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Agent Data Model

```kotlin
@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val iconEmoji: String,  // e.g., "🏃", "💼", "📚"
    
    // Agent Configuration
    val systemPrompt: String,  // Core personality and instructions
    val expertise: List<String>,  // Domain tags
    val personality: AgentPersonality,
    
    // Behavior Settings
    val proactivityLevel: ProactivityLevel,  // How often agent reaches out
    val communicationStyle: CommunicationStyle,
    
    // Data Access Permissions
    val canReadTasks: Boolean = true,
    val canReadGoals: Boolean = true,
    val canReadCalendar: Boolean = true,
    val canCreateTasks: Boolean = false,
    val canCreateReminders: Boolean = false,
    
    // Linked Goal (optional)
    val linkedGoalId: String? = null,
    
    // AI Model Preference
    val preferredModel: String = "claude-3-sonnet",
    
    // Metadata
    val isBuiltIn: Boolean = false,
    val templateId: String? = null,  // If created from template
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0
)

data class AgentPersonality(
    val formalityLevel: Float,  // 0.0 (casual) to 1.0 (formal)
    val verbosityLevel: Float,  // 0.0 (concise) to 1.0 (detailed)
    val encouragementLevel: Float,  // 0.0 (direct) to 1.0 (encouraging)
    val useEmojis: Boolean = true
)

enum class ProactivityLevel {
    PASSIVE,      // Only responds when asked
    BALANCED,     // Occasional suggestions
    PROACTIVE     // Regular check-ins and suggestions
}

enum class CommunicationStyle {
    COACH,        // Encouraging, supportive
    MENTOR,       // Educational, guiding
    ASSISTANT,    // Helpful, efficient
    ACCOUNTABILITY  // Direct, challenging
}
```

### Agent Builder Flow

```kotlin
class AgentBuilder {
    
    // Step 1: Purpose
    fun setPurpose(purpose: AgentPurpose): AgentBuilder
    
    // Step 2: Personality
    fun setPersonality(personality: AgentPersonality): AgentBuilder
    
    // Step 3: Expertise
    fun setExpertise(domains: List<String>): AgentBuilder
    fun setCustomInstructions(instructions: String): AgentBuilder
    
    // Step 4: Permissions
    fun setPermissions(permissions: AgentPermissions): AgentBuilder
    
    // Step 5: Link to Goal (optional)
    fun linkToGoal(goalId: String): AgentBuilder
    
    // Step 6: Choose AI Model
    fun setPreferredModel(modelId: String): AgentBuilder
    
    // Build
    fun build(): AgentEntity
    
    // Generate system prompt from configuration
    private fun generateSystemPrompt(): String {
        return """
            You are ${config.name}, a personal AI assistant specialized in ${config.expertise.joinToString()}.
            
            Your communication style:
            - Formality: ${describeFormality(config.personality.formalityLevel)}
            - Detail level: ${describeVerbosity(config.personality.verbosityLevel)}
            - Tone: ${describeTone(config.personality.encouragementLevel)}
            ${if (config.personality.useEmojis) "- Use emojis appropriately" else "- Do not use emojis"}
            
            Your role: ${config.communicationStyle.description}
            
            ${config.customInstructions ?: ""}
            
            Always be helpful, respectful, and focused on the user's goals.
        """.trimIndent()
    }
}
```

### Built-in Agent Templates

```kotlin
object AgentTemplates {
    
    val PROJECT_MANAGER = AgentTemplate(
        id = "project-manager",
        name = "Project Manager",
        iconEmoji = "📊",
        description = "Helps with sprint planning, deadline tracking, and blocker identification",
        expertise = listOf("project-management", "deadlines", "planning"),
        defaultPersonality = AgentPersonality(
            formalityLevel = 0.7f,
            verbosityLevel = 0.5f,
            encouragementLevel = 0.5f
        ),
        communicationStyle = CommunicationStyle.ASSISTANT,
        systemPromptTemplate = """
            You are a project management assistant. Help users:
            - Break down large projects into manageable tasks
            - Identify dependencies and blockers
            - Track progress and deadlines
            - Suggest priorities using Eisenhower matrix principles
        """
    )
    
    val CAREER_COACH = AgentTemplate(
        id = "career-coach",
        name = "Career Coach",
        iconEmoji = "💼",
        description = "Guides career development, goal setting, and skill building",
        expertise = listOf("career", "networking", "skills", "goals"),
        defaultPersonality = AgentPersonality(
            formalityLevel = 0.5f,
            verbosityLevel = 0.6f,
            encouragementLevel = 0.8f
        ),
        communicationStyle = CommunicationStyle.MENTOR,
        systemPromptTemplate = """
            You are a career development coach. Help users:
            - Set and achieve career goals
            - Identify skill gaps and learning opportunities
            - Plan networking activities
            - Prepare for interviews and reviews
        """
    )
    
    val FITNESS_COACH = AgentTemplate(
        id = "fitness-coach",
        name = "Fitness Coach",
        iconEmoji = "🏃",
        description = "Motivates workouts, tracks habits, and provides accountability",
        expertise = listOf("fitness", "health", "habits", "motivation"),
        defaultPersonality = AgentPersonality(
            formalityLevel = 0.3f,
            verbosityLevel = 0.4f,
            encouragementLevel = 0.9f
        ),
        communicationStyle = CommunicationStyle.COACH,
        systemPromptTemplate = """
            You are an enthusiastic fitness coach. Help users:
            - Stay motivated with workouts
            - Build consistent exercise habits
            - Track progress and celebrate wins
            - Overcome obstacles and excuses
        """
    )
    
    // Additional templates: LEARNING_COACH, FINANCE_ADVISOR, WRITING_COACH, HOME_MANAGER
}
```

### Agent Execution

```kotlin
class AgentExecutor(
    private val agentRepository: AgentRepository,
    private val aiRouter: AiRouter,
    private val contextBuilder: ContextBuilder
) {
    suspend fun chat(agentId: String, userMessage: String): AgentResponse {
        val agent = agentRepository.getAgent(agentId)
        
        // Build context from user data (respecting permissions)
        val context = contextBuilder.build(
            includeTasks = agent.canReadTasks,
            includeGoals = agent.canReadGoals,
            includeCalendar = agent.canReadCalendar,
            linkedGoalId = agent.linkedGoalId
        )
        
        // Build the full prompt
        val prompt = buildPrompt(agent, userMessage, context)
        
        // Route to appropriate AI (cloud for agents)
        val response = aiRouter.route(
            AiRequest(
                prompt = prompt,
                taskType = TaskType.CHAT,
                complexity = Complexity.MEDIUM,
                agentId = agentId
            )
        )
        
        // Parse response for any actions
        val actions = parseActions(response.text)
        
        return AgentResponse(
            text = response.text,
            suggestedActions = actions,
            model = response.model
        )
    }
    
    private suspend fun buildPrompt(
        agent: AgentEntity,
        userMessage: String,
        context: UserContext
    ): String {
        return """
            ${agent.systemPrompt}
            
            Current context:
            ${context.format()}
            
            User: $userMessage
        """.trimIndent()
    }
}

data class AgentResponse(
    val text: String,
    val suggestedActions: List<SuggestedAction> = emptyList(),
    val model: String
)

sealed class SuggestedAction {
    data class CreateTask(val title: String, val dueDate: LocalDate?) : SuggestedAction()
    data class CreateReminder(val text: String, val time: LocalDateTime) : SuggestedAction()
    data class UpdateGoal(val goalId: String, val progress: Float) : SuggestedAction()
}
```

---

## Plugin Architecture

### Plugin System Design

```
┌─────────────────────────────────────────────────────────────────┐
│                        PLUGIN SYSTEM                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                     PLUGIN MANAGER                          │ │
│  │                                                              │ │
│  │  • Plugin discovery and registration                        │ │
│  │  • Lifecycle management (init, start, stop, destroy)        │ │
│  │  • Dependency injection for plugins                         │ │
│  │  • Event bus for inter-plugin communication                 │ │
│  │  • Permission management                                    │ │
│  │                                                              │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                     PLUGIN INTERFACE                        │ │
│  │                                                              │ │
│  │  interface JeevesPlugin {                                   │ │
│  │    val id: String                                           │ │
│  │    val name: String                                         │ │
│  │    val version: String                                      │ │
│  │    val dependencies: List<String>                           │ │
│  │                                                              │ │
│  │    fun initialize(context: PluginContext)                   │ │
│  │    fun onStart()                                            │ │
│  │    fun onStop()                                             │ │
│  │    fun getNavigationItems(): List<NavigationItem>           │ │
│  │    fun getWidgets(): List<Widget>                           │ │
│  │  }                                                          │ │
│  │                                                              │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────────┐  │
│  │   Tasks  │  Goals   │ Calendar │  Health  │   Email      │  │
│  │  Plugin  │  Plugin  │  Plugin  │  Plugin  │   Plugin     │  │
│  │  (MVP)   │  (MVP)   │  (MVP)   │ (Future) │  (Future)    │  │
│  └──────────┴──────────┴──────────┴──────────┴──────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### MVP Plugins

#### 1. Tasks Plugin (Core)
- Eisenhower matrix view and logic
- Task CRUD operations
- Priority calculation engine
- Smart reminders and nudges

#### 2. Goals Plugin (Core)  
- Goal CRUD with milestones
- Progress tracking
- Task-to-goal linking
- Analytics and insights

#### 3. Calendar Plugin (Core)
- Device calendar integration
- Daily briefings
- Meeting notes
- Time-blocking

---

## Data Architecture

### Local Database (Room)

```kotlin
// Core entities

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val dueTime: Long?,
    
    // Eisenhower Matrix
    val isUrgent: Boolean = false,
    val isImportant: Boolean = false,
    val quadrant: Int = 4, // 1=Do, 2=Schedule, 3=Delegate, 4=Eliminate
    
    // Priority and ordering
    val priority: Int = 0, // Calculated score
    val manualOrder: Int = 0,
    
    // Status
    val status: TaskStatus = TaskStatus.PENDING,
    val completedAt: Long? = null,
    
    // Organization
    val projectId: String? = null,
    val parentTaskId: String? = null,
    val tags: List<String> = emptyList(),
    
    // Goals linkage
    val goalId: String? = null,
    
    // Recurrence
    val recurrenceRule: String? = null, // RRULE format
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0,
    val isDeleted: Boolean = false
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val category: GoalCategory,
    
    // Timeline
    val startDate: Long,
    val targetDate: Long,
    
    // Progress
    val progressType: ProgressType, // PERCENTAGE, COUNT, BOOLEAN
    val currentValue: Float = 0f,
    val targetValue: Float = 100f,
    
    // Status
    val status: GoalStatus = GoalStatus.ACTIVE,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0
)

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val calendarEventId: String, // Link to device calendar
    val title: String,
    val startTime: Long,
    val endTime: Long,
    
    // Meeting content
    val agenda: String?,
    val notes: String?,
    val actionItems: List<String> = emptyList(), // Extracted action items
    val summary: String?, // AI-generated summary
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_analytics")
data class DailyAnalyticsEntity(
    @PrimaryKey val date: Long, // Date only, no time
    val tasksCreated: Int = 0,
    val tasksCompleted: Int = 0,
    val tasksMissedDeadline: Int = 0,
    val goalProgress: Map<String, Float> = emptyMap(), // goalId -> progress
    val focusTimeMinutes: Int = 0,
    val meetingsCount: Int = 0
)

enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED, CANCELLED }
enum class GoalStatus { ACTIVE, COMPLETED, ABANDONED, PAUSED }
enum class GoalCategory { CAREER, HEALTH, PERSONAL, FINANCIAL, LEARNING, RELATIONSHIP }
enum class ProgressType { PERCENTAGE, COUNT, BOOLEAN }
```

### DataStore (Preferences)

```kotlin
data class UserPreferences(
    // Eisenhower defaults
    val defaultUrgentThresholdDays: Int = 3,
    val autoCategorizeTasks: Boolean = true,
    
    // Notifications
    val morningBriefingTime: LocalTime = LocalTime.of(7, 0),
    val eveningSummaryTime: LocalTime = LocalTime.of(21, 0),
    val nudgeFrequency: NudgeFrequency = NudgeFrequency.BALANCED,
    
    // AI settings
    val llmModelId: String = "phi-3-mini",
    val enableVoiceInput: Boolean = true,
    
    // Privacy
    val analyticsEnabled: Boolean = false,
    val syncEnabled: Boolean = false,
    
    // UI
    val theme: Theme = Theme.SYSTEM,
    val startScreen: StartScreen = StartScreen.TODAY
)
```

---

## Android App Architecture

### Module Structure

```
apps/android/
├── app/                          # Main application module
│   ├── src/main/
│   │   ├── kotlin/
│   │   │   └── com/jeeves/app/
│   │   │       ├── JeevesApplication.kt
│   │   │       ├── MainActivity.kt
│   │   │       └── navigation/
│   │   └── res/
│   └── build.gradle.kts
│
├── core/                         # Core shared modules
│   ├── common/                   # Common utilities
│   ├── ui/                       # Design system, components
│   ├── data/                     # Room DB, DataStore
│   ├── domain/                   # Domain models, use cases
│   ├── ai/                       # LLM engine, NLP
│   └── analytics/                # Local analytics
│
├── plugins/                      # Feature plugins
│   ├── tasks/                    # Eisenhower tasks
│   ├── goals/                    # Goals & progress
│   ├── calendar/                 # Smart calendar
│   └── [future]/                 # Post-MVP plugins
│
└── sync/                         # Optional cloud sync
    └── src/main/kotlin/
```

### Dependency Injection (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideLlmEngine(
        @ApplicationContext context: Context,
        modelManager: ModelManager
    ): LlmEngine = LlamaCppEngine(context, modelManager)
    
    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        llmEngine: LlmEngine
    ): TaskRepository = TaskRepositoryImpl(taskDao, llmEngine)
}
```

---

## Smart Features Implementation

### Eisenhower Priority Engine

```kotlin
class EisenhowerEngine(
    private val llmEngine: LlmEngine,
    private val preferences: UserPreferences
) {
    suspend fun classifyTask(task: TaskInput): EisenhowerClassification {
        // Step 1: Extract deadline if present
        val deadline = llmEngine.extractDeadline(task.text)
        
        // Step 2: Calculate urgency based on deadline
        val urgency = calculateUrgency(deadline, preferences.defaultUrgentThresholdDays)
        
        // Step 3: Assess importance using LLM
        val importance = llmEngine.assessImportance(
            taskText = task.text,
            userGoals = getActiveGoals(),
            context = getCurrentContext()
        )
        
        // Step 4: Determine quadrant
        val quadrant = when {
            urgency && importance -> Quadrant.DO          // Q1: Do First
            !urgency && importance -> Quadrant.SCHEDULE    // Q2: Schedule
            urgency && !importance -> Quadrant.DELEGATE    // Q3: Delegate
            else -> Quadrant.ELIMINATE                      // Q4: Don't Do
        }
        
        return EisenhowerClassification(
            quadrant = quadrant,
            urgency = urgency,
            importance = importance,
            suggestedAction = getSuggestedAction(quadrant),
            confidence = calculateConfidence()
        )
    }
    
    private fun getSuggestedAction(quadrant: Quadrant): String = when (quadrant) {
        Quadrant.DO -> "Do this immediately"
        Quadrant.SCHEDULE -> "Block time on your calendar for this"
        Quadrant.DELEGATE -> "Consider delegating or simplifying"
        Quadrant.ELIMINATE -> "Is this really necessary?"
    }
}
```

### Daily Briefing Generator

```kotlin
class BriefingGenerator(
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val llmEngine: LlmEngine
) {
    suspend fun generateMorningBriefing(): DailyBriefing {
        val today = LocalDate.now()
        
        val tasks = taskRepository.getTasksDueOn(today)
        val overdue = taskRepository.getOverdueTasks()
        val events = calendarRepository.getEventsForDay(today)
        val goals = goalRepository.getActiveGoals()
        
        val summary = llmEngine.generateBriefing(
            prompt = """
                Generate a concise, encouraging morning briefing:
                
                Today's Schedule:
                ${events.format()}
                
                Tasks Due Today (${tasks.size}):
                ${tasks.format()}
                
                Overdue Tasks (${overdue.size}):
                ${overdue.format()}
                
                Active Goals:
                ${goals.format()}
                
                Keep it under 150 words. Be encouraging but realistic.
            """.trimIndent()
        )
        
        return DailyBriefing(
            date = today,
            summary = summary,
            topPriorityTasks = tasks.sortedByDescending { it.priority }.take(3),
            upcomingEvents = events,
            goalReminders = getGoalReminders(goals)
        )
    }
}
```

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| App cold start | < 3s | Firebase Performance |
| LLM first inference | < 5s | Local timing |
| LLM subsequent inference | < 2s | Local timing |
| Task creation (NLP) | < 3s | Local timing |
| UI frame rate | 60fps | GPU profiler |
| Memory usage (idle) | < 200MB | Android Profiler |
| Memory usage (LLM active) | < 800MB | Android Profiler |
| Battery (1hr active use) | < 5% | Battery historian |
| APK size (without model) | < 30MB | Build output |

---

## Cost Analysis

### Operational Costs (Per Month)

| Component | Cost | Notes |
|-----------|------|-------|
| Backend (Auth + Sync) | $50-200 | Minimal usage, scales with premium users |
| Model CDN | $20-50 | Model download bandwidth |
| Database | $50-100 | PostgreSQL for user accounts only |
| Monitoring | $0-50 | Free tier sufficient initially |
| **Total** | **$120-400/mo** | For first 10,000 users |

### Cost Per User

| User Type | Backend Cost/Month | Notes |
|-----------|-------------------|-------|
| Free (offline only) | $0 | No backend usage |
| Free (with sync) | ~$0.02 | Minimal sync data |
| Premium | ~$0.05 | Full sync + backup |

### Comparison with Cloud-First Approach

| Approach | Cost/1000 MAU | Notes |
|----------|---------------|-------|
| **Jeeves (on-device)** | $5-10 | Minimal backend |
| Cloud LLM approach | $500-2000 | API costs dominate |
| Hybrid approach | $100-300 | Selective cloud usage |

---

## Security Architecture

### On-Device Security

```
┌─────────────────────────────────────────────────────────────────┐
│                    MOBILE SECURITY LAYERS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                 APPLICATION LAYER                          │   │
│  │  • ProGuard/R8 obfuscation                                │   │
│  │  • Root detection (optional warning)                       │   │
│  │  • Debugger detection (release builds)                    │   │
│  │  • SSL certificate pinning (for sync)                     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                 DATA LAYER                                  │   │
│  │  • SQLCipher for database encryption (AES-256)            │   │
│  │  • EncryptedSharedPreferences for settings                │   │
│  │  • Android Keystore for encryption keys                   │   │
│  │  • Biometric unlock option                                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                 NETWORK LAYER                               │   │
│  │  • TLS 1.3 only                                           │   │
│  │  • Certificate pinning                                     │   │
│  │  • E2E encryption for sync (user-controlled keys)         │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Development Roadmap

### Phase 1: MVP (Weeks 1-8)
- Core architecture setup
- Room database with all entities
- Basic UI with Jetpack Compose
- LLM integration (llama.cpp)
- Tasks plugin with Eisenhower matrix
- Goals plugin with progress tracking
- Calendar plugin (read-only integration)
- Daily briefings

### Phase 2: Polish (Weeks 9-12)  
- AI tuning and prompt optimization
- Performance optimization
- Analytics and insights views
- Notifications and widgets
- Extensive testing

### Phase 3: Launch (Weeks 13-14)
- Play Store submission
- Beta testing
- Documentation
- Marketing launch

### Phase 4: Post-Launch (Months 4+)
- iOS development
- Email plugin
- Health plugin
- Optional cloud sync
- Community plugins

---

*Document Owner: Principal Backend/Infrastructure Engineer*
*Last Updated: February 2026*
*Status: Approved for MVP Development*

---

## Data Architecture

### Primary Database (PostgreSQL)

```sql
-- Core user data
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    preferences JSONB DEFAULT '{}'
);

-- Tasks
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    title TEXT NOT NULL,
    description TEXT,
    due_date TIMESTAMPTZ,
    priority INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    sync_version BIGINT DEFAULT 0
);

-- Conversations
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    started_at TIMESTAMPTZ DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    context JSONB DEFAULT '{}'
);

-- Messages
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id),
    role VARCHAR(50) NOT NULL, -- user, assistant, system
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'
);
```

### Caching Strategy (Redis)

```
# Session cache
session:{user_id} -> JWT claims (TTL: 1h)

# Rate limiting
ratelimit:{user_id}:{endpoint} -> count (TTL: 1m)

# User preferences cache
preferences:{user_id} -> JSONB (TTL: 15m)

# Active conversation context
context:{user_id} -> conversation state (TTL: 30m)

# Feature flags
flags:{user_id} -> evaluated flags (TTL: 5m)
```

### Vector Database (Pinecone/pgvector)

Used for:
- Semantic search over user's notes and tasks
- RAG for personalized responses
- Similar task/event suggestions
- Knowledge base retrieval

---

## Security Architecture

### Defense in Depth

```
┌─────────────────────────────────────────────────────────────────┐
│                          WAF / DDoS Protection                   │
├─────────────────────────────────────────────────────────────────┤
│                          API Gateway                             │
│                    (Rate Limiting, Auth)                         │
├─────────────────────────────────────────────────────────────────┤
│                          Service Mesh                            │
│                    (mTLS, Traffic Control)                       │
├─────────────────────────────────────────────────────────────────┤
│                    Application Security                          │
│              (Input Validation, OWASP Controls)                  │
├─────────────────────────────────────────────────────────────────┤
│                       Data Security                              │
│              (Encryption at Rest, Field-level)                   │
└─────────────────────────────────────────────────────────────────┘
```

### Encryption

| Data Type | At Rest | In Transit | Method |
|-----------|---------|------------|--------|
| User credentials | AES-256-GCM | TLS 1.3 | Argon2id hashing |
| Personal data | AES-256-GCM | TLS 1.3 | Field-level encryption |
| Conversation history | AES-256-GCM | TLS 1.3 | User-key encryption |
| API keys | AES-256-GCM | TLS 1.3 | HSM-backed |

---

## Infrastructure

### Kubernetes Deployment

```yaml
# Example service deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-engine
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: ai-engine
        image: jeeves/ai-engine:latest
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "2000m"
            memory: "2Gi"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
```

### Observability Stack

- **Metrics**: Prometheus + Grafana
- **Logging**: Loki + Grafana
- **Tracing**: Jaeger / OpenTelemetry
- **Alerting**: Alertmanager + PagerDuty

---

## API Design

### RESTful Endpoints

```
# Authentication
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout

# User
GET    /api/v1/user/profile
PATCH  /api/v1/user/profile
GET    /api/v1/user/preferences
PUT    /api/v1/user/preferences

# Tasks
GET    /api/v1/tasks
POST   /api/v1/tasks
GET    /api/v1/tasks/{id}
PATCH  /api/v1/tasks/{id}
DELETE /api/v1/tasks/{id}

# Conversations
POST   /api/v1/conversations
GET    /api/v1/conversations/{id}
POST   /api/v1/conversations/{id}/messages
GET    /api/v1/conversations/{id}/messages

# Assistant
POST   /api/v1/assistant/query
POST   /api/v1/assistant/voice
GET    /api/v1/assistant/suggestions

# Sync
POST   /api/v1/sync/push
POST   /api/v1/sync/pull
```

### WebSocket Protocol

```json
// Client -> Server
{
  "type": "query",
  "id": "uuid",
  "payload": {
    "text": "What's on my schedule today?"
  }
}

// Server -> Client (streaming response)
{
  "type": "response",
  "id": "uuid",
  "payload": {
    "text": "You have 3 meetings...",
    "complete": false
  }
}
```

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| API p50 latency | < 100ms | Prometheus |
| API p99 latency | < 500ms | Prometheus |
| AI query latency | < 2s | Prometheus |
| App cold start | < 2s | Firebase/Crashlytics |
| Database query | < 50ms | pg_stat_statements |
| Availability | 99.9% | Uptime monitoring |

---

*Document Owner: Principal Backend/Infrastructure Engineer*
*Last Updated: August 2025*
*Status: Living Document*
