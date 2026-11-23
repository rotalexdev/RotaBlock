package dev.rotalex.rotablock.core.block

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlotCategoryTest {
    @Test
    fun theGrammarIsClosed() {
        assertEquals(
            listOf(
                "STATEMENT",
                "EXPRESSION",
                "TYPE",
                "DECLARATION",
                "NAME",
                "MODIFIER",
                "PARAMETER",
                "ARGUMENT",
                "OPAQUE",
            ),
            SlotCategory.entries.map { it.name },
        )
    }

    @Test
    fun opaqueAcceptsEveryCategory() {
        val everything = SlotCategory.entries.filter { SlotCategory.OPAQUE.accepts(it) }
        assertEquals(SlotCategory.entries.toList(), everything)
    }

    @Test
    fun aCategoryAcceptsItself() {
        assertTrue(SlotCategory.STATEMENT.accepts(SlotCategory.STATEMENT))
    }

    @Test
    fun aCategoryRejectsAnotherCategory() {
        assertFalse(SlotCategory.STATEMENT.accepts(SlotCategory.EXPRESSION))
        assertFalse(SlotCategory.NAME.accepts(SlotCategory.OPAQUE))
    }
}
