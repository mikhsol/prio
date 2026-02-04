package com.prio.core.ai.registry

import com.prio.core.ai.provider.AiCapability
import com.prio.core.ai.provider.ModelDefinition
import com.prio.core.ai.provider.PredefinedModels
import com.prio.core.ai.provider.PromptTemplate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for ModelRegistry.
 * 
 * Note: These tests focus on pure logic that doesn't require Android context.
 * Integration tests requiring DataStore and file system are in androidTest.
 */
class ModelRegistryTest {
    
    @Nested
    inner class PredefinedModelsTests {
        
        @Test
        fun `PredefinedModels contains expected models`() {
            val models = PredefinedModels.ALL
            
            assertEquals(4, models.size)
            assertTrue(models.any { it.id == "phi-3-mini-4k-instruct-q4" })
            assertTrue(models.any { it.id == "mistral-7b-instruct-v0.2-q4" })
            assertTrue(models.any { it.id == "gemma-2-2b-it-q4" })
            assertTrue(models.any { it.id == "rule-based" })
        }
        
        @Test
        fun `Phi-3 Mini has correct specifications`() {
            val phi3 = PredefinedModels.PHI3_MINI_4K
            
            assertEquals("phi-3-mini-4k-instruct-q4", phi3.id)
            assertEquals("Phi-3 Mini (Recommended)", phi3.displayName)
            assertEquals("Phi-3-mini-4k-instruct-q4.gguf", phi3.fileName)
            assertEquals(2_300_000_000L, phi3.sizeBytes)
            assertEquals(4096, phi3.contextLength)
            assertEquals(4, phi3.minRamGb)
            assertEquals(PromptTemplate.PHI3, phi3.promptTemplate)
            assertTrue(phi3.capabilities.contains(AiCapability.CLASSIFICATION))
            assertTrue(phi3.capabilities.contains(AiCapability.EXTRACTION))
            assertTrue(phi3.capabilities.contains(AiCapability.GENERATION))
        }
        
        @Test
        fun `Mistral 7B has correct specifications`() {
            val mistral = PredefinedModels.MISTRAL_7B
            
            assertEquals("mistral-7b-instruct-v0.2-q4", mistral.id)
            assertEquals("Mistral 7B", mistral.displayName)
            assertEquals(4_100_000_000L, mistral.sizeBytes)
            assertEquals(8192, mistral.contextLength)
            assertEquals(6, mistral.minRamGb)
            assertEquals(PromptTemplate.MISTRAL, mistral.promptTemplate)
            assertTrue(mistral.capabilities.contains(AiCapability.LONG_CONTEXT))
        }
        
        @Test
        fun `Gemma 2B has correct specifications`() {
            val gemma = PredefinedModels.GEMMA_2B
            
            assertEquals("gemma-2-2b-it-q4", gemma.id)
            assertEquals("Gemma 2 2B (Smaller)", gemma.displayName)
            assertEquals(1_700_000_000L, gemma.sizeBytes)
            assertEquals(3, gemma.minRamGb)
            assertEquals(PromptTemplate.GEMMA, gemma.promptTemplate)
        }
        
        @Test
        fun `Rule-based model has correct specifications`() {
            val ruleBased = PredefinedModels.RULE_BASED
            
            assertEquals("rule-based", ruleBased.id)
            assertEquals("Fast Mode (Offline)", ruleBased.displayName)
            assertEquals("", ruleBased.fileName)
            assertEquals(0L, ruleBased.sizeBytes)
            assertEquals(0, ruleBased.minRamGb)
            assertEquals(PromptTemplate.RAW, ruleBased.promptTemplate)
            assertTrue(ruleBased.capabilities.contains(AiCapability.CLASSIFICATION))
            assertTrue(ruleBased.capabilities.contains(AiCapability.EXTRACTION))
        }
        
        @Test
        fun `Default model is Phi-3 Mini`() {
            assertEquals(PredefinedModels.PHI3_MINI_4K, PredefinedModels.DEFAULT)
        }
    }
    
    @Nested
    inner class DeviceTierTests {
        
        @Test
        fun `Models sorted by RAM requirement`() {
            val sortedByRam = PredefinedModels.ALL.sortedBy { it.minRamGb }
            
            assertEquals("rule-based", sortedByRam[0].id)  // 0 GB
            assertEquals("gemma-2-2b-it-q4", sortedByRam[1].id)  // 3 GB
            assertEquals("phi-3-mini-4k-instruct-q4", sortedByRam[2].id)  // 4 GB
            assertEquals("mistral-7b-instruct-v0.2-q4", sortedByRam[3].id)  // 6 GB
        }
        
        @Test
        fun `Low RAM device gets limited models`() {
            // Simulate 4GB device
            val available = PredefinedModels.ALL.filter { it.minRamGb <= 4 }
            
            assertEquals(3, available.size)
            assertTrue(available.any { it.id == "rule-based" })
            assertTrue(available.any { it.id == "gemma-2-2b-it-q4" })
            assertTrue(available.any { it.id == "phi-3-mini-4k-instruct-q4" })
            assertFalse(available.any { it.id == "mistral-7b-instruct-v0.2-q4" })
        }
        
        @Test
        fun `High RAM device gets all models`() {
            // Simulate 8GB device
            val available = PredefinedModels.ALL.filter { it.minRamGb <= 8 }
            
            assertEquals(4, available.size)
        }
        
        @Test
        fun `Very low RAM device gets only rule-based`() {
            // Simulate 2GB device
            val available = PredefinedModels.ALL.filter { it.minRamGb <= 2 }
            
            assertEquals(1, available.size)
            assertEquals("rule-based", available[0].id)
        }
    }
    
    @Nested
    inner class ModelCapabilitiesTests {
        
        @Test
        fun `All LLM models support classification`() {
            val llmModels = PredefinedModels.ALL.filter { it.id != "rule-based" }
            
            assertTrue(llmModels.all { AiCapability.CLASSIFICATION in it.capabilities })
        }
        
        @Test
        fun `Only Mistral supports long context`() {
            val longContextModels = PredefinedModels.ALL.filter { 
                AiCapability.LONG_CONTEXT in it.capabilities 
            }
            
            assertEquals(1, longContextModels.size)
            assertEquals("mistral-7b-instruct-v0.2-q4", longContextModels[0].id)
        }
        
        @Test
        fun `All models have valid download URLs except rule-based`() {
            for (model in PredefinedModels.ALL) {
                if (model.id == "rule-based") {
                    assertEquals("", model.downloadUrl)
                } else {
                    assertTrue(model.downloadUrl.startsWith("https://"), 
                        "Model ${model.id} should have HTTPS URL")
                }
            }
        }
    }
    
    @Nested
    inner class PromptTemplateTests {
        
        @Test
        fun `Each model family has correct template`() {
            assertEquals(PromptTemplate.PHI3, PredefinedModels.PHI3_MINI_4K.promptTemplate)
            assertEquals(PromptTemplate.MISTRAL, PredefinedModels.MISTRAL_7B.promptTemplate)
            assertEquals(PromptTemplate.GEMMA, PredefinedModels.GEMMA_2B.promptTemplate)
            assertEquals(PromptTemplate.RAW, PredefinedModels.RULE_BASED.promptTemplate)
        }
        
        @Test
        fun `All prompt templates are defined`() {
            val templates = PromptTemplate.values()
            
            assertTrue(templates.size >= 7)
            assertTrue(templates.contains(PromptTemplate.PHI3))
            assertTrue(templates.contains(PromptTemplate.CHATML))
            assertTrue(templates.contains(PromptTemplate.MISTRAL))
            assertTrue(templates.contains(PromptTemplate.LLAMA2))
            assertTrue(templates.contains(PromptTemplate.LLAMA3))
            assertTrue(templates.contains(PromptTemplate.GEMMA))
            assertTrue(templates.contains(PromptTemplate.RAW))
        }
    }
    
    @Nested
    inner class StorageCalculationTests {
        
        @Test
        fun `Total storage for all models`() {
            val totalBytes = PredefinedModels.ALL.sumOf { it.sizeBytes }
            
            // Phi-3 (2.3GB) + Mistral (4.1GB) + Gemma (1.7GB) + Rule-based (0) = 8.1GB
            assertEquals(8_100_000_000L, totalBytes)
        }
        
        @Test
        fun `Storage formatted correctly`() {
            // Test that model sizes are in expected ranges
            assertTrue(PredefinedModels.PHI3_MINI_4K.sizeBytes > 2_000_000_000L)
            assertTrue(PredefinedModels.PHI3_MINI_4K.sizeBytes < 3_000_000_000L)
            
            assertTrue(PredefinedModels.MISTRAL_7B.sizeBytes > 4_000_000_000L)
            assertTrue(PredefinedModels.MISTRAL_7B.sizeBytes < 5_000_000_000L)
            
            assertTrue(PredefinedModels.GEMMA_2B.sizeBytes > 1_500_000_000L)
            assertTrue(PredefinedModels.GEMMA_2B.sizeBytes < 2_000_000_000L)
        }
    }
}
