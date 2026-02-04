package com.prio.core.ai.provider

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for AiProvider interface types.
 */
class AiProviderTest {
    
    @Nested
    inner class AiCapabilityTests {
        
        @Test
        fun `All capabilities are defined`() {
            val capabilities = AiCapability.values()
            
            assertEquals(6, capabilities.size)
            assertTrue(capabilities.contains(AiCapability.CLASSIFICATION))
            assertTrue(capabilities.contains(AiCapability.EXTRACTION))
            assertTrue(capabilities.contains(AiCapability.GENERATION))
            assertTrue(capabilities.contains(AiCapability.STREAMING))
            assertTrue(capabilities.contains(AiCapability.LONG_CONTEXT))
            assertTrue(capabilities.contains(AiCapability.FUNCTION_CALLING))
        }
    }
    
    @Nested
    inner class AiCapabilitiesDataClassTests {
        
        @Test
        fun `Default capabilities are sensible`() {
            val caps = AiCapabilities()
            
            assertTrue(caps.supportsEisenhowerClassification)
            assertTrue(caps.supportsTaskParsing)
            assertTrue(caps.supportsGoalSuggestion)
            assertTrue(caps.supportsBriefingGeneration)
            assertTrue(caps.supportsActionItemExtraction)
            assertFalse(caps.supportsGeneralChat)
            assertFalse(caps.supportsStreaming)
            assertTrue(caps.isOnDevice)
            assertFalse(caps.requiresNetwork)
            assertEquals(50L, caps.estimatedLatencyMs)
            assertEquals(4096, caps.maxInputTokens)
            assertEquals(4096, caps.contextLength)
        }
        
        @Test
        fun `Cloud provider capabilities`() {
            val cloudCaps = AiCapabilities(
                supportsGeneralChat = true,
                supportsStreaming = true,
                isOnDevice = false,
                requiresNetwork = true,
                estimatedLatencyMs = 500L,
                maxInputTokens = 128_000,
                contextLength = 128_000
            )
            
            assertTrue(cloudCaps.supportsGeneralChat)
            assertTrue(cloudCaps.supportsStreaming)
            assertFalse(cloudCaps.isOnDevice)
            assertTrue(cloudCaps.requiresNetwork)
            assertEquals(500L, cloudCaps.estimatedLatencyMs)
            assertEquals(128_000, cloudCaps.maxInputTokens)
        }
    }
    
    @Nested
    inner class ModelInfoTests {
        
        @Test
        fun `ModelInfo creation`() {
            val info = ModelInfo(
                modelId = "test-model",
                displayName = "Test Model",
                provider = "on-device",
                contextLength = 4096,
                capabilities = setOf(AiCapability.CLASSIFICATION),
                sizeBytes = 1_000_000_000L,
                version = "1.0",
                description = "Test description",
                isDownloaded = true
            )
            
            assertEquals("test-model", info.modelId)
            assertEquals("Test Model", info.displayName)
            assertEquals("on-device", info.provider)
            assertEquals(4096, info.contextLength)
            assertTrue(info.capabilities.contains(AiCapability.CLASSIFICATION))
            assertEquals(1_000_000_000L, info.sizeBytes)
            assertEquals("1.0", info.version)
            assertTrue(info.isDownloaded)
        }
        
        @Test
        fun `ModelInfo with minimal fields`() {
            val info = ModelInfo(
                modelId = "minimal",
                displayName = "Minimal",
                provider = "test",
                contextLength = 2048,
                capabilities = emptySet()
            )
            
            assertNull(info.sizeBytes)
            assertNull(info.version)
            assertEquals("", info.description)
            assertFalse(info.isDownloaded)
        }
    }
    
    @Nested
    inner class ModelDefinitionTests {
        
        @Test
        fun `ModelDefinition creation`() {
            val definition = ModelDefinition(
                id = "test-model",
                displayName = "Test Model",
                fileName = "test.gguf",
                sizeBytes = 2_000_000_000L,
                downloadUrl = "https://example.com/test.gguf",
                sha256 = "abc123",
                capabilities = setOf(AiCapability.CLASSIFICATION, AiCapability.EXTRACTION),
                contextLength = 4096,
                recommendedForTasks = setOf(AiTaskType.CLASSIFY_EISENHOWER),
                description = "Test description",
                minRamGb = 4,
                promptTemplate = PromptTemplate.CHATML
            )
            
            assertEquals("test-model", definition.id)
            assertEquals("test.gguf", definition.fileName)
            assertEquals(2_000_000_000L, definition.sizeBytes)
            assertEquals("abc123", definition.sha256)
            assertEquals(4, definition.minRamGb)
            assertEquals(PromptTemplate.CHATML, definition.promptTemplate)
        }
        
        @Test
        fun `ModelDefinition defaults`() {
            val definition = ModelDefinition(
                id = "minimal",
                displayName = "Minimal",
                fileName = "minimal.gguf",
                sizeBytes = 1_000_000_000L,
                downloadUrl = "https://example.com/minimal.gguf",
                sha256 = "def456",
                capabilities = emptySet(),
                contextLength = 2048
            )
            
            assertTrue(definition.recommendedForTasks.isEmpty())
            assertEquals("", definition.description)
            assertEquals(4, definition.minRamGb)
            assertEquals(PromptTemplate.CHATML, definition.promptTemplate)
        }
    }
    
    @Nested
    inner class AiTaskTypeTests {
        
        @Test
        fun `All task types are defined`() {
            val types = AiTaskType.values()
            
            assertEquals(7, types.size)
            assertTrue(types.contains(AiTaskType.CLASSIFY_EISENHOWER))
            assertTrue(types.contains(AiTaskType.PARSE_TASK))
            assertTrue(types.contains(AiTaskType.SUGGEST_SMART_GOAL))
            assertTrue(types.contains(AiTaskType.GENERATE_BRIEFING))
            assertTrue(types.contains(AiTaskType.EXTRACT_ACTION_ITEMS))
            assertTrue(types.contains(AiTaskType.SUMMARIZE))
            assertTrue(types.contains(AiTaskType.GENERAL_CHAT))
        }
    }
    
    @Nested
    inner class PromptTemplateTests {
        
        @Test
        fun `All prompt templates are defined`() {
            val templates = PromptTemplate.values()
            
            assertEquals(7, templates.size)
        }
        
        @Test
        fun `Prompt templates cover major model families`() {
            // Phi-3 family
            assertTrue(PromptTemplate.values().contains(PromptTemplate.PHI3))
            
            // ChatML (generic)
            assertTrue(PromptTemplate.values().contains(PromptTemplate.CHATML))
            
            // Mistral family
            assertTrue(PromptTemplate.values().contains(PromptTemplate.MISTRAL))
            
            // Llama family (2 and 3)
            assertTrue(PromptTemplate.values().contains(PromptTemplate.LLAMA2))
            assertTrue(PromptTemplate.values().contains(PromptTemplate.LLAMA3))
            
            // Gemma family
            assertTrue(PromptTemplate.values().contains(PromptTemplate.GEMMA))
            
            // Raw (no template)
            assertTrue(PromptTemplate.values().contains(PromptTemplate.RAW))
        }
    }
}
