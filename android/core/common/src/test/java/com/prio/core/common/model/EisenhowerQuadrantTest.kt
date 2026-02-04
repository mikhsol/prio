package com.prio.core.common.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for EisenhowerQuadrant enum.
 */
class EisenhowerQuadrantTest {
    
    @Test
    fun `fromFlags returns DO_FIRST for urgent and important`() {
        val result = EisenhowerQuadrant.fromFlags(isUrgent = true, isImportant = true)
        assertEquals(EisenhowerQuadrant.DO_FIRST, result)
    }
    
    @Test
    fun `fromFlags returns SCHEDULE for important but not urgent`() {
        val result = EisenhowerQuadrant.fromFlags(isUrgent = false, isImportant = true)
        assertEquals(EisenhowerQuadrant.SCHEDULE, result)
    }
    
    @Test
    fun `fromFlags returns DELEGATE for urgent but not important`() {
        val result = EisenhowerQuadrant.fromFlags(isUrgent = true, isImportant = false)
        assertEquals(EisenhowerQuadrant.DELEGATE, result)
    }
    
    @Test
    fun `fromFlags returns ELIMINATE for neither urgent nor important`() {
        val result = EisenhowerQuadrant.fromFlags(isUrgent = false, isImportant = false)
        assertEquals(EisenhowerQuadrant.ELIMINATE, result)
    }
    
    @Test
    fun `DO_FIRST has correct color hex`() {
        assertEquals(0xFFDC2626, EisenhowerQuadrant.DO_FIRST.colorHex)
    }
    
    @Test
    fun `SCHEDULE has correct color hex`() {
        assertEquals(0xFFF59E0B, EisenhowerQuadrant.SCHEDULE.colorHex)
    }
    
    @Test
    fun `all quadrants have display names`() {
        EisenhowerQuadrant.entries.forEach { quadrant ->
            assertTrue(quadrant.displayName.isNotBlank())
        }
    }
    
    @Test
    fun `all quadrants have emojis`() {
        EisenhowerQuadrant.entries.forEach { quadrant ->
            assertTrue(quadrant.emoji.isNotBlank())
        }
    }
}
