package dev.rotalex.rotablock.core.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextRangeTest {
    @Test
    fun reportsHalfOpenLengthBetweenStartAndEnd() {
        val range = TextRange(2, 5)
        assertEquals(2, range.start)
        assertEquals(5, range.end)
        assertEquals(3, range.length)
        assertFalse(range.isEmpty)
    }

    @Test
    fun emptyRangeHasZeroLength() {
        val range = TextRange(4, 4)
        assertEquals(0, range.length)
        assertTrue(range.isEmpty)
    }

    @Test
    fun containsItsStartAndExcludesItsEnd() {
        val range = TextRange(2, 5)
        assertTrue(range.contains(2))
        assertTrue(range.contains(4))
        assertFalse(range.contains(5))
        assertFalse(range.contains(1))
    }

    @Test
    fun emptyRangeContainsNoOffset() {
        assertFalse(TextRange(3, 3).contains(3))
    }

    @Test
    fun rejectsNegativeStart() {
        assertFailsWith<IllegalArgumentException> { TextRange(-1, 0) }
    }

    @Test
    fun rejectsEndBeforeStart() {
        assertFailsWith<IllegalArgumentException> { TextRange(4, 3) }
    }

    @Test
    fun rangesWithTheSameBoundsAreEqual() {
        assertEquals(TextRange(1, 2), TextRange(1, 2))
    }
}
