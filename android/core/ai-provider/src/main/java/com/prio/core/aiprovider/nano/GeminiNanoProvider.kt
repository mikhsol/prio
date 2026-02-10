package com.prio.core.aiprovider.nano

import android.content.Context
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.SummarizationResult
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import com.prio.core.ai.model.AiRequest
import com.prio.core.ai.model.AiRequestType
import com.prio.core.ai.model.AiResponse
import com.prio.core.ai.model.AiResponseMetadata
import com.prio.core.ai.model.AiResult
import com.prio.core.ai.model.AiStreamChunk
import com.prio.core.ai.provider.AiCapability
import com.prio.core.ai.provider.AiProvider
import com.prio.core.ai.provider.ModelInfo
import com.prio.core.common.model.EisenhowerQuadrant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini Nano AI Provider — on-device inference via Android AI Core / ML Kit GenAI.
 *
 * Tasks 3.6.2–3.6.5: Implements [AiProvider] interface using ML Kit Prompt API
 * for classification/parsing and Summarization API for briefing generation.
 *
 * Key properties:
 * - **Zero APK size**: Model managed by Google Play Services, not bundled
 * - **Privacy-first**: All inference on-device, data never leaves the phone
 * - **Offline-capable**: Works without internet after initial model download
 * - **Auto-updated**: Google manages model updates via Play Services
 *
 * Routing priority (in [AiProviderRouter]):
 *   1. Rule-based (<50ms)  →  2. **Gemini Nano** (~1-2s)  →  3. llama.cpp (~2-3s)
 */
@Singleton
class GeminiNanoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val availability: GeminiNanoAvailability
) : AiProvider {

    companion object {
        private const val TAG = "GeminiNanoProvider"
        const val PROVIDER_ID = "gemini-nano"
        const val DISPLAY_NAME = "Gemini Nano (Built-in AI)"
    }

    // =========================================================================
    // AiProvider interface
    // =========================================================================

    override val providerId: String = PROVIDER_ID
    override val displayName: String = DISPLAY_NAME

    override val capabilities: Set<AiCapability> = setOf(
        AiCapability.CLASSIFICATION,
        AiCapability.EXTRACTION,
        AiCapability.GENERATION,
        AiCapability.STREAMING
    )

    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    /** ML Kit Prompt API client — lazy, created on first use after availability check. */
    private var generativeModel: GenerativeModel? = null

    /** ML Kit Summarization client — lazy, created on first use after availability check. */
    private var summarizer: Summarizer? = null

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Initialize provider: check AI Core availability, create ML Kit clients,
     * optionally trigger model download if status is DOWNLOADABLE.
     */
    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("Initializing GeminiNanoProvider")

        try {
            // Step 1: Check feature availability
            availability.checkAll()

            // Step 2: Create clients if available
            val promptReady = initializePromptApi()
            val summaryReady = initializeSummarizationApi()
            val ready = promptReady || summaryReady

            _isAvailable.value = ready
            Timber.tag(TAG).i(
                "GeminiNanoProvider initialized: prompt=$promptReady, summary=$summaryReady"
            )
            ready
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "GeminiNanoProvider initialization failed")
            _isAvailable.value = false
            false
        }
    }

    override suspend fun release() {
        Timber.tag(TAG).i("Releasing GeminiNanoProvider")
        try {
            generativeModel?.close()
            generativeModel = null
            summarizer?.close()
            summarizer = null
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error releasing GeminiNanoProvider clients")
        }
        _isAvailable.value = false
    }

    // =========================================================================
    // Request handling
    // =========================================================================

    /**
     * Route request to the appropriate ML Kit API based on [AiRequestType].
     *
     * - CLASSIFY_EISENHOWER → Prompt API with classification prompt
     * - PARSE_TASK          → Prompt API with entity-extraction prompt
     * - GENERATE_BRIEFING   → Summarization API (article/conversation modes)
     * - SUMMARIZE           → Summarization API
     * - Others              → Prompt API with general prompt
     */
    override suspend fun complete(request: AiRequest): Result<AiResponse> {
        val startTime = System.currentTimeMillis()

        if (!_isAvailable.value) {
            return Result.failure(IllegalStateException("GeminiNanoProvider not available"))
        }

        return try {
            val result = when (request.type) {
                AiRequestType.CLASSIFY_EISENHOWER -> classifyEisenhower(request)
                AiRequestType.PARSE_TASK -> parseTask(request)
                AiRequestType.GENERATE_BRIEFING -> generateBriefing(request)
                AiRequestType.SUMMARIZE -> summarize(request)
                AiRequestType.SUGGEST_SMART_GOAL -> suggestSmartGoal(request)
                AiRequestType.EXTRACT_ACTION_ITEMS -> extractActionItems(request)
                AiRequestType.GENERAL_CHAT -> generalGenerate(request)
            }

            val latencyMs = System.currentTimeMillis() - startTime
            Timber.tag(TAG).d("${request.type} completed in ${latencyMs}ms")

            Result.success(
                AiResponse(
                    success = true,
                    requestId = request.id,
                    result = result,
                    rawText = null,
                    metadata = AiResponseMetadata(
                        provider = PROVIDER_ID,
                        model = "gemini-nano",
                        latencyMs = latencyMs,
                        wasRuleBased = false,
                        confidenceScore = extractConfidence(result)
                    )
                )
            )
        } catch (e: Exception) {
            val latencyMs = System.currentTimeMillis() - startTime
            Timber.tag(TAG).w(e, "${request.type} failed after ${latencyMs}ms")
            Result.failure(e)
        }
    }

    override suspend fun stream(request: AiRequest): Flow<AiStreamChunk> = flow {
        val model = generativeModel
        if (model == null) {
            emit(AiStreamChunk(text = "", isComplete = true))
            return@flow
        }

        val prompt = buildPromptForType(request)
        var tokenIndex = 0

        try {
            model.generateContentStream(prompt).collect { response ->
                val text = response.candidates.firstOrNull()?.text ?: ""
                emit(
                    AiStreamChunk(
                        requestId = request.id,
                        text = text,
                        isComplete = false,
                        tokenIndex = tokenIndex++
                    )
                )
            }
            emit(AiStreamChunk(requestId = request.id, text = "", isComplete = true, tokenIndex = tokenIndex))
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Streaming failed, falling back to complete()")
            val result = complete(request)
            result.onSuccess { response ->
                emit(
                    AiStreamChunk(
                        requestId = request.id,
                        text = response.rawText ?: "",
                        isComplete = true,
                        tokenIndex = tokenIndex
                    )
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getModelInfo(): ModelInfo = ModelInfo(
        modelId = "gemini-nano",
        displayName = DISPLAY_NAME,
        provider = "google-aicore",
        contextLength = 4000,
        capabilities = capabilities,
        sizeBytes = 0L, // Managed by Play Services
        version = "1.0-beta",
        description = "Google Gemini Nano via Android AI Core. Zero APK size, on-device.",
        isDownloaded = _isAvailable.value
    )

    override suspend fun estimateCost(request: AiRequest): Float? = null // Free, on-device

    // =========================================================================
    // 3.6.3: Eisenhower Classification via Prompt API
    // =========================================================================

    /**
     * Classify a task into Eisenhower Q1–Q4 using the Prompt API.
     * Target: ≥80% accuracy, <2s latency.
     */
    private suspend fun classifyEisenhower(request: AiRequest): AiResult {
        val prompt = buildEisenhowerPrompt(request)
        val rawResponse = executePrompt(prompt)
        return parseEisenhowerResponse(rawResponse)
    }

    /**
     * Build the Eisenhower classification prompt per ACTION_PLAN.md spec.
     */
    private fun buildEisenhowerPrompt(request: AiRequest): String {
        val dueDate = request.context?.custom?.get("due_date") ?: "none"
        val goalContext = request.context?.existingGoals?.joinToString(", ") ?: "none"

        return """
            |Classify this task into an Eisenhower Matrix quadrant.
            |
            |Task: "${request.input}"
            |Due date: $dueDate
            |Context: $goalContext
            |
            |Respond in exactly this JSON format:
            |{"quadrant": "Q1", "confidence": 0.85, "explanation": "one sentence", "is_urgent": true, "is_important": true}
            |
            |Rules:
            |- Q1 (Do First): Urgent AND important — deadlines within 48h, critical outcomes
            |- Q2 (Schedule): Important but NOT urgent — long-term goals, planning, growth
            |- Q3 (Delegate): Urgent but NOT important — interruptions, most emails, some meetings
            |- Q4 (Eliminate): Neither urgent NOR important — time wasters, trivial tasks
            |
            |Only output the JSON object, nothing else.
        """.trimMargin()
    }

    /**
     * Parse Gemini Nano's response into [AiResult.EisenhowerClassification].
     * Defensive: falls back to Q2 with low confidence on parse failure.
     */
    private fun parseEisenhowerResponse(raw: String): AiResult {
        return try {
            val jsonStr = extractJsonObject(raw)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            val quadrantStr = obj["quadrant"]?.jsonPrimitive?.contentOrNull ?: "Q2"
            val confidence = obj["confidence"]?.jsonPrimitive?.floatOrNull ?: 0.5f
            val explanation = obj["explanation"]?.jsonPrimitive?.contentOrNull ?: ""
            val isUrgent = obj["is_urgent"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            val isImportant = obj["is_important"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true

            AiResult.EisenhowerClassification(
                quadrant = mapQuadrant(quadrantStr),
                confidence = confidence.coerceIn(0f, 1f),
                explanation = explanation,
                isUrgent = isUrgent,
                isImportant = isImportant,
                urgencySignals = if (isUrgent) listOf("gemini-nano-detected") else emptyList(),
                importanceSignals = if (isImportant) listOf("gemini-nano-detected") else emptyList()
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse Eisenhower response: $raw")
            // Defensive fallback — Q2 with low confidence triggers potential re-escalation
            AiResult.EisenhowerClassification(
                quadrant = EisenhowerQuadrant.SCHEDULE,
                confidence = 0.3f,
                explanation = "Gemini Nano response could not be parsed",
                isUrgent = false,
                isImportant = true
            )
        }
    }

    // =========================================================================
    // 3.6.4: Task Parsing via Prompt API
    // =========================================================================

    /**
     * Extract structured task data from natural language input.
     */
    private suspend fun parseTask(request: AiRequest): AiResult {
        val prompt = buildTaskParsingPrompt(request)
        val rawResponse = executePrompt(prompt)
        return parseTaskResponse(rawResponse)
    }

    private fun buildTaskParsingPrompt(request: AiRequest): String {
        return """
            |Extract structured information from this task input.
            |
            |Input: "${request.input}"
            |
            |Respond in exactly this JSON format:
            |{"title": "clean task title", "due_date": "YYYY-MM-DD or null", "due_time": "HH:MM or null", "is_urgent": false, "keywords": ["keyword1"]}
            |
            |Rules:
            |- title: A clean, concise version of the task (remove dates/times from title)
            |- due_date: Extract any mentioned date (e.g., "tomorrow" → compute date, "Friday" → next Friday)
            |- due_time: Extract any mentioned time (e.g., "3pm" → "15:00")
            |- is_urgent: true if urgency words present (ASAP, urgent, immediately, critical)
            |- keywords: 1-3 topic tags
            |
            |Only output the JSON object, nothing else.
        """.trimMargin()
    }

    private fun parseTaskResponse(raw: String): AiResult {
        return try {
            val jsonStr = extractJsonObject(raw)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: ""
            val dueDate = obj["due_date"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" }
            val dueTime = obj["due_time"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" }
            val isUrgent = obj["is_urgent"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            val keywords = try {
                obj["keywords"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull ?: "" } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            AiResult.ParsedTask(
                title = title.ifBlank { raw.take(100) },
                dueDate = dueDate,
                dueTime = dueTime,
                suggestedQuadrant = if (isUrgent) EisenhowerQuadrant.DO_FIRST else null,
                tags = keywords.filter { it.isNotBlank() },
                confidence = 0.8f
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse task response: $raw")
            AiResult.ParsedTask(
                title = request_input_fallback(raw),
                confidence = 0.3f
            )
        }
    }

    // =========================================================================
    // 3.6.5: Briefing Generation via Summarization API
    // =========================================================================

    /**
     * Generate morning/evening briefing using the ML Kit Summarization API.
     * Uses [SummarizerOptions.InputType.ARTICLE] for task summaries.
     */
    private suspend fun generateBriefing(request: AiRequest): AiResult {
        val sm = summarizer
        if (sm != null) {
            return generateBriefingViaSummarizer(sm, request)
        }
        // Fall back to Prompt API if summarizer not available
        return generateBriefingViaPrompt(request)
    }

    private suspend fun generateBriefingViaSummarizer(
        sm: Summarizer,
        request: AiRequest
    ): AiResult = withContext(Dispatchers.IO) {
        val summaryText = try {
            val summarizationRequest = SummarizationRequest.builder(request.input).build()
            val result: SummarizationResult = sm.runInference(summarizationRequest).get()
            result.summary
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Summarization failed, falling back to prompt")
            ""
        }

        if (summaryText.isBlank()) {
            return@withContext generateBriefingViaPrompt(request)
        }

        // Parse the summary into briefing sections
        val lines = summaryText.lines().filter { it.isNotBlank() }
        AiResult.BriefingContent(
            greeting = "Good ${getDayPeriod()}!",
            summary = summaryText,
            topPriorities = lines.take(3),
            insights = lines.drop(3).take(2),
            briefingType = request.context?.custom?.get("briefing_type") ?: "morning"
        )
    }

    private suspend fun generateBriefingViaPrompt(request: AiRequest): AiResult {
        val briefingType = request.context?.custom?.get("briefing_type") ?: "morning"
        val prompt = """
            |Generate a ${briefingType} briefing summary.
            |
            |Input data:
            |${request.input}
            |
            |Respond in exactly this JSON format:
            |{"greeting": "Good morning!", "summary": "brief overview", "top_priorities": ["priority1", "priority2", "priority3"], "insights": ["insight1"]}
            |
            |Only output the JSON object, nothing else.
        """.trimMargin()

        val rawResponse = executePrompt(prompt)
        return parseBriefingResponse(rawResponse, briefingType)
    }

    private fun parseBriefingResponse(raw: String, briefingType: String): AiResult {
        return try {
            val jsonStr = extractJsonObject(raw)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            AiResult.BriefingContent(
                greeting = obj["greeting"]?.jsonPrimitive?.contentOrNull ?: "Good ${getDayPeriod()}!",
                summary = obj["summary"]?.jsonPrimitive?.contentOrNull ?: "",
                topPriorities = try {
                    obj["top_priorities"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull ?: "" } ?: emptyList()
                } catch (_: Exception) { emptyList() },
                insights = try {
                    obj["insights"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull ?: "" } ?: emptyList()
                } catch (_: Exception) { emptyList() },
                briefingType = briefingType
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse briefing response")
            AiResult.BriefingContent(
                greeting = "Good ${getDayPeriod()}!",
                summary = raw.take(500),
                topPriorities = emptyList(),
                insights = emptyList(),
                briefingType = briefingType
            )
        }
    }

    // =========================================================================
    // Other request types
    // =========================================================================

    private suspend fun summarize(request: AiRequest): AiResult {
        val sm = summarizer
        if (sm != null) {
            val summaryText = withContext(Dispatchers.IO) {
                try {
                    val summarizationRequest = SummarizationRequest.builder(request.input).build()
                    val result: SummarizationResult = sm.runInference(summarizationRequest).get()
                    result.summary
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Summarization failed")
                    ""
                }
            }
            if (summaryText.isNotBlank()) {
                val keyPoints = summaryText.lines().filter { it.isNotBlank() }
                return AiResult.SummaryResult(
                    summary = summaryText,
                    keyPoints = keyPoints,
                    wordCount = summaryText.split("\\s+".toRegex()).size
                )
            }
        }
        // Fallback to prompt
        val rawResponse = executePrompt("Summarize the following text concisely:\n\n${request.input}")
        return AiResult.SummaryResult(
            summary = rawResponse,
            keyPoints = rawResponse.lines().filter { it.isNotBlank() },
            wordCount = rawResponse.split("\\s+".toRegex()).size
        )
    }

    private suspend fun suggestSmartGoal(request: AiRequest): AiResult {
        val prompt = """
            |Refine this goal into a SMART goal (Specific, Measurable, Achievable, Relevant, Time-bound).
            |
            |Goal: "${request.input}"
            |
            |Respond in exactly this JSON format:
            |{"refined_goal": "...", "specific": "...", "measurable": "...", "achievable": "...", "relevant": "...", "time_bound": "...", "suggested_milestones": ["m1", "m2"]}
            |
            |Only output the JSON object, nothing else.
        """.trimMargin()
        val rawResponse = executePrompt(prompt)
        return parseSmartGoalResponse(rawResponse)
    }

    private fun parseSmartGoalResponse(raw: String): AiResult {
        return try {
            val jsonStr = extractJsonObject(raw)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            AiResult.SmartGoalSuggestion(
                refinedGoal = obj["refined_goal"]?.jsonPrimitive?.contentOrNull ?: "",
                specific = obj["specific"]?.jsonPrimitive?.contentOrNull ?: "",
                measurable = obj["measurable"]?.jsonPrimitive?.contentOrNull ?: "",
                achievable = obj["achievable"]?.jsonPrimitive?.contentOrNull ?: "",
                relevant = obj["relevant"]?.jsonPrimitive?.contentOrNull ?: "",
                timeBound = obj["time_bound"]?.jsonPrimitive?.contentOrNull ?: "",
                suggestedMilestones = try {
                    obj["suggested_milestones"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull ?: "" } ?: emptyList()
                } catch (_: Exception) { emptyList() }
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse SMART goal response")
            AiResult.SmartGoalSuggestion(
                refinedGoal = raw.take(200),
                specific = "", measurable = "", achievable = "",
                relevant = "", timeBound = ""
            )
        }
    }

    private suspend fun extractActionItems(request: AiRequest): AiResult {
        val prompt = """
            |Extract action items from these meeting notes.
            |
            |Notes: "${request.input}"
            |
            |Respond in exactly this JSON format:
            |{"items": [{"description": "...", "assignee": null, "due_date": null, "priority": "medium"}]}
            |
            |Only output the JSON object, nothing else.
        """.trimMargin()
        val rawResponse = executePrompt(prompt)
        return try {
            val jsonStr = extractJsonObject(rawResponse)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            val items = obj["items"]?.jsonArray?.map { item ->
                val itemObj = item.jsonObject
                AiResult.ActionItemResult(
                    description = itemObj["description"]?.jsonPrimitive?.contentOrNull ?: "",
                    assignee = itemObj["assignee"]?.jsonPrimitive?.contentOrNull,
                    dueDate = itemObj["due_date"]?.jsonPrimitive?.contentOrNull,
                    priority = itemObj["priority"]?.jsonPrimitive?.contentOrNull
                )
            } ?: emptyList()
            AiResult.ExtractedActionItems(items = items)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse action items response")
            AiResult.ExtractedActionItems(items = emptyList())
        }
    }

    private suspend fun generalGenerate(request: AiRequest): AiResult {
        val rawResponse = executePrompt(request.input)
        return AiResult.GeneratedText(
            text = rawResponse,
            sections = rawResponse.lines().filter { it.isNotBlank() }
        )
    }

    // =========================================================================
    // ML Kit API execution
    // =========================================================================

    /**
     * Execute a prompt via the ML Kit Prompt API and return the raw text.
     */
    private suspend fun executePrompt(prompt: String): String {
        val model = generativeModel
            ?: throw IllegalStateException("GenerativeModel not initialized")

        return try {
            val response = model.generateContent(prompt)
            response.candidates.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Prompt API call failed")
            ""
        }
    }

    // =========================================================================
    // Initialization helpers
    // =========================================================================

    private suspend fun initializePromptApi(): Boolean {
        val status = availability.promptStatus.value
        return when (status) {
            is GeminiNanoAvailability.FeatureStatus.Available -> {
                generativeModel = Generation.getClient()
                warmupPromptManager()
                true
            }
            is GeminiNanoAvailability.FeatureStatus.Downloadable -> {
                Timber.tag(TAG).i("Prompt API downloadable — triggering download")
                val downloaded = availability.downloadPromptModel()
                if (downloaded) {
                    generativeModel = Generation.getClient()
                    warmupPromptManager()
                }
                downloaded
            }
            else -> {
                Timber.tag(TAG).d("Prompt API not available: $status")
                false
            }
        }
    }

    private suspend fun initializeSummarizationApi(): Boolean {
        val status = availability.summarizationStatus.value
        return when (status) {
            is GeminiNanoAvailability.FeatureStatus.Available -> {
                val options = SummarizerOptions.builder(context)
                    .setInputType(SummarizerOptions.InputType.ARTICLE)
                    .build()
                summarizer = Summarization.getClient(options)
                true
            }
            is GeminiNanoAvailability.FeatureStatus.Downloadable -> {
                Timber.tag(TAG).i("Summarization API downloadable — will be available after download")
                // Summarization downloads alongside Prompt API model
                false
            }
            else -> {
                Timber.tag(TAG).d("Summarization API not available: $status")
                false
            }
        }
    }

    /**
     * Warm up the Prompt API to reduce first-inference latency.
     */
    private suspend fun warmupPromptManager() {
        try {
            generativeModel?.generateContent("Hello")
            Timber.tag(TAG).d("Prompt API warmed up")
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Prompt API warmup failed (non-fatal)")
        }
    }

    // =========================================================================
    // Parsing utilities
    // =========================================================================

    /**
     * Extract a JSON object from raw model output.
     * Handles cases where the model wraps JSON in markdown code blocks.
     */
    private fun extractJsonObject(raw: String): String {
        // Try to find JSON object in the response
        val trimmed = raw.trim()

        // Remove markdown code block wrappers
        val stripped = trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // Find first { and last }
        val start = stripped.indexOf('{')
        val end = stripped.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return stripped.substring(start, end + 1)
        }

        return stripped
    }

    /**
     * Map quadrant string (Q1/Q2/Q3/Q4) to [EisenhowerQuadrant] enum.
     */
    private fun mapQuadrant(quadrantStr: String): EisenhowerQuadrant {
        return when (quadrantStr.uppercase().trim()) {
            "Q1", "DO_FIRST", "DO FIRST", "DO" -> EisenhowerQuadrant.DO_FIRST
            "Q2", "SCHEDULE", "PLAN" -> EisenhowerQuadrant.SCHEDULE
            "Q3", "DELEGATE" -> EisenhowerQuadrant.DELEGATE
            "Q4", "ELIMINATE", "DROP" -> EisenhowerQuadrant.ELIMINATE
            else -> EisenhowerQuadrant.SCHEDULE // Safe default
        }
    }

    private fun extractConfidence(result: AiResult): Float {
        return when (result) {
            is AiResult.EisenhowerClassification -> result.confidence
            is AiResult.ParsedTask -> result.confidence
            else -> 0.7f
        }
    }

    private fun getDayPeriod(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "morning"
            hour < 17 -> "afternoon"
            else -> "evening"
        }
    }

    /**
     * Fallback title extractor when JSON parsing fails.
     */
    private fun request_input_fallback(raw: String): String {
        return raw.take(100).trim().ifBlank { "Untitled task" }
    }

    /**
     * Build the appropriate prompt text for a given request type.
     * Used by streaming to construct prompt without going through full handler.
     */
    private fun buildPromptForType(request: AiRequest): String {
        return when (request.type) {
            AiRequestType.CLASSIFY_EISENHOWER -> buildEisenhowerPrompt(request)
            AiRequestType.PARSE_TASK -> buildTaskParsingPrompt(request)
            else -> request.input
        }
    }
}
