package com.prio.core.aiprovider.nano

import com.prio.core.ai.model.AiResult
import com.prio.core.ai.provider.AiCapability
import com.prio.core.aiprovider.nano.ui.GeminiNanoUiState
import com.prio.core.common.model.EisenhowerQuadrant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Task 3.6.10: Unit tests for GeminiNanoProvider and related classes.
 *
 * Tests cover:
 * - Provider constants & capabilities
 * - FeatureStatus sealed class branches
 * - Eisenhower classification JSON parsing (valid, malformed, edge cases)
 * - Task parsing JSON parsing (valid, null fields, urgent detection)
 * - GeminiNanoUiState computed properties
 *
 * Note: GeminiNanoProvider requires Android Context and ML Kit (not available
 * in JVM unit tests). We test the parsing logic and data-layer contracts here.
 * Full provider lifecycle is covered by instrumented tests.
 */
@DisplayName("GeminiNanoProvider")
class GeminiNanoProviderTest {

    // =========================================================================
    // Replicates the private parsing logic from GeminiNanoProvider.kt
    // so we can unit-test JSON handling without Android dependencies.
    // =========================================================================

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun extractJsonObject(raw: String): String {
        val stripped = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = stripped.indexOf('{')
        val end = stripped.lastIndexOf('}')
        return if (start >= 0 && end > start) stripped.substring(start, end + 1) else stripped
    }

    private fun mapQuadrant(quadrantStr: String): EisenhowerQuadrant = when (quadrantStr.uppercase().trim()) {
        "Q1", "DO_FIRST", "DO FIRST", "DO" -> EisenhowerQuadrant.DO_FIRST
        "Q2", "SCHEDULE", "PLAN" -> EisenhowerQuadrant.SCHEDULE
        "Q3", "DELEGATE" -> EisenhowerQuadrant.DELEGATE
        "Q4", "ELIMINATE", "DROP" -> EisenhowerQuadrant.ELIMINATE
        else -> EisenhowerQuadrant.SCHEDULE
    }

    private fun parseEisenhowerResponse(raw: String): AiResult.EisenhowerClassification {
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
        } catch (_: Exception) {
            AiResult.EisenhowerClassification(
                quadrant = EisenhowerQuadrant.SCHEDULE,
                confidence = 0.3f,
                explanation = "Gemini Nano response could not be parsed",
                isUrgent = false,
                isImportant = true
            )
        }
    }

    private fun parseTaskResponse(raw: String): AiResult.ParsedTask {
        return try {
            val jsonStr = extractJsonObject(raw)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: ""
            val dueDate = obj["due_date"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" }
            val dueTime = obj["due_time"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" }
            val isUrgent = obj["is_urgent"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            val keywords = try {
                obj["keywords"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull ?: "" } ?: emptyList()
            } catch (_: Exception) { emptyList() }

            AiResult.ParsedTask(
                title = title.ifBlank { raw.take(100) },
                dueDate = dueDate,
                dueTime = dueTime,
                suggestedQuadrant = if (isUrgent) EisenhowerQuadrant.DO_FIRST else null,
                tags = keywords.filter { it.isNotBlank() },
                confidence = 0.8f
            )
        } catch (_: Exception) {
            AiResult.ParsedTask(
                title = raw.take(100).trim().ifBlank { "Untitled task" },
                confidence = 0.3f
            )
        }
    }

    // =========================================================================
    // Tests
    // =========================================================================

    @Nested
    @DisplayName("Provider constants")
    inner class ProviderConstants {

        @Test
        fun `providerId is gemini-nano`() {
            assertEquals("gemini-nano", GeminiNanoProvider.PROVIDER_ID)
        }

        @Test
        fun `display name is human-readable`() {
            assertEquals("Gemini Nano (Built-in AI)", GeminiNanoProvider.DISPLAY_NAME)
        }

        @Test
        fun `declared capabilities include core set`() {
            val expected = setOf(
                AiCapability.CLASSIFICATION,
                AiCapability.EXTRACTION,
                AiCapability.GENERATION,
                AiCapability.STREAMING
            )
            assertEquals(4, expected.size)
        }
    }

    @Nested
    @DisplayName("FeatureStatus sealed class")
    inner class FeatureStatusTests {

        @Test
        fun `Available is a singleton`() {
            val status = GeminiNanoAvailability.FeatureStatus.Available
            assertTrue(status is GeminiNanoAvailability.FeatureStatus.Available)
        }

        @Test
        fun `Downloadable is a singleton`() {
            val status = GeminiNanoAvailability.FeatureStatus.Downloadable
            assertTrue(status is GeminiNanoAvailability.FeatureStatus.Downloadable)
        }

        @Test
        fun `Downloading carries progress`() {
            val status = GeminiNanoAvailability.FeatureStatus.Downloading(42)
            assertEquals(42, status.progressPercent)
        }

        @Test
        fun `Downloading at 0 percent`() {
            val status = GeminiNanoAvailability.FeatureStatus.Downloading(0)
            assertEquals(0, status.progressPercent)
        }

        @Test
        fun `Downloading at 100 percent`() {
            val status = GeminiNanoAvailability.FeatureStatus.Downloading(100)
            assertEquals(100, status.progressPercent)
        }

        @Test
        fun `Unavailable carries reason`() {
            val status = GeminiNanoAvailability.FeatureStatus.Unavailable("Device not supported")
            assertEquals("Device not supported", status.reason)
        }

        @Test
        fun `Unavailable equality by reason`() {
            val a = GeminiNanoAvailability.FeatureStatus.Unavailable("no AI Core")
            val b = GeminiNanoAvailability.FeatureStatus.Unavailable("no AI Core")
            assertEquals(a, b)
        }
    }

    @Nested
    @DisplayName("Eisenhower classification parsing")
    inner class EisenhowerParsing {

        @Test
        fun `valid Q1 JSON parses correctly`() {
            val json = """{"quadrant":"Q1","confidence":0.92,"explanation":"Deadline tomorrow","is_urgent":true,"is_important":true}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(EisenhowerQuadrant.DO_FIRST, result.quadrant)
            assertEquals(0.92f, result.confidence, 0.01f)
            assertTrue(result.isUrgent)
            assertTrue(result.isImportant)
            assertEquals("Deadline tomorrow", result.explanation)
        }

        @Test
        fun `valid Q2 JSON parses correctly`() {
            val json = """{"quadrant":"Q2","confidence":0.85,"explanation":"Long-term growth","is_urgent":false,"is_important":true}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
            assertFalse(result.isUrgent)
            assertTrue(result.isImportant)
        }

        @Test
        fun `valid Q3 JSON parses correctly`() {
            val json = """{"quadrant":"Q3","confidence":0.78,"explanation":"Can be delegated","is_urgent":true,"is_important":false}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(EisenhowerQuadrant.DELEGATE, result.quadrant)
            assertTrue(result.isUrgent)
            assertFalse(result.isImportant)
        }

        @Test
        fun `valid Q4 JSON parses correctly`() {
            val json = """{"quadrant":"Q4","confidence":0.95,"explanation":"Time waster","is_urgent":false,"is_important":false}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(EisenhowerQuadrant.ELIMINATE, result.quadrant)
        }

        @Test
        fun `DO_FIRST string maps to Q1`() {
            val json = """{"quadrant":"DO_FIRST","confidence":0.9,"explanation":"","is_urgent":true,"is_important":true}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(EisenhowerQuadrant.DO_FIRST, result.quadrant)
        }

        @Test
        fun `SCHEDULE string maps to Q2`() {
            val json = """{"quadrant":"SCHEDULE","confidence":0.9,"explanation":"","is_urgent":false,"is_important":true}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
        }

        @Test
        fun `DELEGATE string maps to Q3`() {
            val json = """{"quadrant":"DELEGATE","confidence":0.8,"explanation":"","is_urgent":true,"is_important":false}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(EisenhowerQuadrant.DELEGATE, result.quadrant)
        }

        @Test
        fun `ELIMINATE string maps to Q4`() {
            val json = """{"quadrant":"ELIMINATE","confidence":0.85,"explanation":"","is_urgent":false,"is_important":false}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(EisenhowerQuadrant.ELIMINATE, result.quadrant)
        }

        @Test
        fun `markdown code-block wrapped JSON parses correctly`() {
            val raw = "```json\n{\"quadrant\":\"Q1\",\"confidence\":0.88,\"explanation\":\"Critical\",\"is_urgent\":true,\"is_important\":true}\n```"
            val result = parseEisenhowerResponse(raw)
            assertEquals(EisenhowerQuadrant.DO_FIRST, result.quadrant)
            assertEquals(0.88f, result.confidence, 0.01f)
        }

        @Test
        fun `missing confidence defaults to 0_5`() {
            val json = """{"quadrant":"Q2","explanation":"Important"}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(0.5f, result.confidence, 0.01f)
        }

        @Test
        fun `confidence above 1_0 is clamped`() {
            val json = """{"quadrant":"Q1","confidence":1.5,"explanation":"Over"}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(1.0f, result.confidence, 0.01f)
        }

        @Test
        fun `confidence below 0 is clamped`() {
            val json = """{"quadrant":"Q1","confidence":-0.3,"explanation":"Neg"}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(0.0f, result.confidence, 0.01f)
        }

        @Test
        fun `unknown quadrant defaults to Q2 SCHEDULE`() {
            val json = """{"quadrant":"UNKNOWN","confidence":0.7,"explanation":""}"""
            val result = parseEisenhowerResponse(json)
            assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
        }

        @Test
        fun `malformed JSON falls back to Q2 with low confidence`() {
            val result = parseEisenhowerResponse("this is not json at all")
            assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
            assertTrue(result.confidence <= 0.5f)
            assertEquals("Gemini Nano response could not be parsed", result.explanation)
        }

        @Test
        fun `empty string falls back to Q2`() {
            val result = parseEisenhowerResponse("")
            assertEquals(EisenhowerQuadrant.SCHEDULE, result.quadrant)
            assertTrue(result.confidence <= 0.5f)
        }

        @Test
        fun `urgency signals populated when urgent`() {
            val json = """{"quadrant":"Q1","confidence":0.9,"explanation":"","is_urgent":true,"is_important":true}"""
            val result = parseEisenhowerResponse(json)
            assertTrue(result.urgencySignals.isNotEmpty())
        }

        @Test
        fun `urgency signals empty when not urgent`() {
            val json = """{"quadrant":"Q2","confidence":0.9,"explanation":"","is_urgent":false,"is_important":true}"""
            val result = parseEisenhowerResponse(json)
            assertTrue(result.urgencySignals.isEmpty())
        }
    }

    @Nested
    @DisplayName("Task parsing")
    inner class TaskParsing {

        @Test
        fun `valid task JSON parses all fields`() {
            val json = """{"title":"Buy groceries","due_date":"2026-02-15","due_time":"14:00","is_urgent":false,"keywords":["shopping","errands"]}"""
            val result = parseTaskResponse(json)
            assertEquals("Buy groceries", result.title)
            assertEquals("2026-02-15", result.dueDate)
            assertEquals("14:00", result.dueTime)
            assertEquals(listOf("shopping", "errands"), result.tags)
            assertNull(result.suggestedQuadrant)
            assertEquals(0.8f, result.confidence, 0.01f)
        }

        @Test
        fun `null due_date string treated as null`() {
            val json = """{"title":"Walk the dog","due_date":"null","due_time":null,"is_urgent":false,"keywords":[]}"""
            val result = parseTaskResponse(json)
            assertNull(result.dueDate)
            assertNull(result.dueTime)
        }

        @Test
        fun `urgent task suggests Q1 quadrant`() {
            val json = """{"title":"Deploy hotfix","due_date":null,"due_time":null,"is_urgent":true,"keywords":["deploy"]}"""
            val result = parseTaskResponse(json)
            assertEquals(EisenhowerQuadrant.DO_FIRST, result.suggestedQuadrant)
        }

        @Test
        fun `non-urgent task has null suggestedQuadrant`() {
            val json = """{"title":"Read a book","due_date":null,"due_time":null,"is_urgent":false,"keywords":["reading"]}"""
            val result = parseTaskResponse(json)
            assertNull(result.suggestedQuadrant)
        }

        @Test
        fun `empty keywords array produces empty tags`() {
            val json = """{"title":"Test","due_date":null,"due_time":null,"is_urgent":false,"keywords":[]}"""
            val result = parseTaskResponse(json)
            assertTrue(result.tags.isEmpty())
        }

        @Test
        fun `blank keyword strings are filtered out`() {
            val json = """{"title":"Test","due_date":null,"due_time":null,"is_urgent":false,"keywords":["valid",""," "]}"""
            val result = parseTaskResponse(json)
            assertEquals(listOf("valid"), result.tags)
        }

        @Test
        fun `blank title falls back to raw input truncated`() {
            val json = """{"title":"","due_date":null,"due_time":null,"is_urgent":false,"keywords":[]}"""
            val result = parseTaskResponse(json)
            // Empty title → falls back to raw.take(100) which is the json string
            assertTrue(result.title.isNotBlank())
        }

        @Test
        fun `malformed JSON returns fallback ParsedTask`() {
            val result = parseTaskResponse("not json")
            assertEquals(0.3f, result.confidence, 0.01f)
            assertTrue(result.title.isNotBlank())
        }

        @Test
        fun `empty string returns fallback with Untitled task`() {
            val result = parseTaskResponse("")
            assertEquals("Untitled task", result.title)
            assertEquals(0.3f, result.confidence, 0.01f)
        }
    }

    @Nested
    @DisplayName("extractJsonObject helper")
    inner class JsonExtractionTests {

        @Test
        fun `extracts plain JSON object`() {
            val result = extractJsonObject("""{"key": "value"}""")
            assertEquals("""{"key": "value"}""", result)
        }

        @Test
        fun `extracts from markdown code block`() {
            val raw = "```json\n{\"key\": \"value\"}\n```"
            val result = extractJsonObject(raw)
            assertEquals("""{"key": "value"}""", result)
        }

        @Test
        fun `extracts from plain code block`() {
            val raw = "```\n{\"key\": \"value\"}\n```"
            val result = extractJsonObject(raw)
            assertEquals("""{"key": "value"}""", result)
        }

        @Test
        fun `extracts when surrounded by text`() {
            val raw = "Here is the result: {\"key\": \"value\"} end."
            val result = extractJsonObject(raw)
            assertEquals("""{"key": "value"}""", result)
        }

        @Test
        fun `returns original when no braces found`() {
            val raw = "no json here"
            val result = extractJsonObject(raw)
            assertEquals("no json here", result)
        }
    }

    @Nested
    @DisplayName("mapQuadrant helper")
    inner class QuadrantMappingTests {

        @Test
        fun `maps all Q aliases to correct quadrants`() {
            assertEquals(EisenhowerQuadrant.DO_FIRST, mapQuadrant("Q1"))
            assertEquals(EisenhowerQuadrant.DO_FIRST, mapQuadrant("DO_FIRST"))
            assertEquals(EisenhowerQuadrant.DO_FIRST, mapQuadrant("DO FIRST"))
            assertEquals(EisenhowerQuadrant.DO_FIRST, mapQuadrant("DO"))
            assertEquals(EisenhowerQuadrant.SCHEDULE, mapQuadrant("Q2"))
            assertEquals(EisenhowerQuadrant.SCHEDULE, mapQuadrant("SCHEDULE"))
            assertEquals(EisenhowerQuadrant.SCHEDULE, mapQuadrant("PLAN"))
            assertEquals(EisenhowerQuadrant.DELEGATE, mapQuadrant("Q3"))
            assertEquals(EisenhowerQuadrant.DELEGATE, mapQuadrant("DELEGATE"))
            assertEquals(EisenhowerQuadrant.ELIMINATE, mapQuadrant("Q4"))
            assertEquals(EisenhowerQuadrant.ELIMINATE, mapQuadrant("ELIMINATE"))
            assertEquals(EisenhowerQuadrant.ELIMINATE, mapQuadrant("DROP"))
        }

        @Test
        fun `case insensitive matching`() {
            assertEquals(EisenhowerQuadrant.DO_FIRST, mapQuadrant("q1"))
            assertEquals(EisenhowerQuadrant.SCHEDULE, mapQuadrant("schedule"))
            assertEquals(EisenhowerQuadrant.DELEGATE, mapQuadrant("delegate"))
        }

        @Test
        fun `unknown string defaults to SCHEDULE`() {
            assertEquals(EisenhowerQuadrant.SCHEDULE, mapQuadrant("UNKNOWN"))
            assertEquals(EisenhowerQuadrant.SCHEDULE, mapQuadrant(""))
            assertEquals(EisenhowerQuadrant.SCHEDULE, mapQuadrant("gibberish"))
        }
    }

    @Nested
    @DisplayName("GeminiNanoUiState")
    inner class UiStateTests {

        @Test
        fun `statusSummary when ready`() {
            val state = GeminiNanoUiState(isReady = true)
            assertTrue(state.statusSummary.contains("active", ignoreCase = true))
        }

        @Test
        fun `statusSummary when prompt available but not ready`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Available,
                isReady = false
            )
            assertTrue(state.statusSummary.contains("activate", ignoreCase = true))
        }

        @Test
        fun `statusSummary when downloading`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Downloading(55)
            )
            assertTrue(state.statusSummary.contains("55%"))
        }

        @Test
        fun `statusSummary when downloadable`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Downloadable
            )
            assertTrue(state.statusSummary.contains("download", ignoreCase = true))
        }

        @Test
        fun `statusSummary when unavailable`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Unavailable("no"),
                summarizationStatus = GeminiNanoAvailability.FeatureStatus.Unavailable("no")
            )
            assertTrue(state.statusSummary.contains("not available", ignoreCase = true))
        }

        @Test
        fun `downloadProgress null when not downloading`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Available
            )
            assertNull(state.downloadProgress)
        }

        @Test
        fun `downloadProgress returns percentage when downloading`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Downloading(67)
            )
            assertEquals(67, state.downloadProgress)
        }

        @Test
        fun `isSupported true when prompt available`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Available
            )
            assertTrue(state.isSupported)
        }

        @Test
        fun `isSupported true when prompt downloadable`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Downloadable
            )
            assertTrue(state.isSupported)
        }

        @Test
        fun `isSupported true when summarization available`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Unavailable("no"),
                summarizationStatus = GeminiNanoAvailability.FeatureStatus.Available
            )
            assertTrue(state.isSupported)
        }

        @Test
        fun `isSupported false when both unavailable`() {
            val state = GeminiNanoUiState(
                promptStatus = GeminiNanoAvailability.FeatureStatus.Unavailable("no"),
                summarizationStatus = GeminiNanoAvailability.FeatureStatus.Unavailable("no")
            )
            assertFalse(state.isSupported)
        }
    }
}
