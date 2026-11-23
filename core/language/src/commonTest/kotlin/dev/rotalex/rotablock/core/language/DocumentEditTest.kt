package dev.rotalex.rotablock.core.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DocumentEditTest {
    @Test
    fun carriesTheThreeWritePrimitives() {
        val edit = DocumentEdit(offset = 4, oldLength = 2, newText = "xy")
        assertEquals(4, edit.offset)
        assertEquals(2, edit.oldLength)
        assertEquals("xy", edit.newText)
    }

    @Test
    fun acceptsAZeroLengthInsertion() {
        assertEquals(0, DocumentEdit(offset = 0, oldLength = 0, newText = "a").oldLength)
    }

    @Test
    fun acceptsAnEmptyReplacement() {
        assertEquals("", DocumentEdit(offset = 1, oldLength = 1, newText = "").newText)
    }

    @Test
    fun rejectsNegativeOffset() {
        assertFailsWith<IllegalArgumentException> { DocumentEdit(offset = -1, oldLength = 0, newText = "a") }
    }

    @Test
    fun rejectsNegativeOldLength() {
        assertFailsWith<IllegalArgumentException> { DocumentEdit(offset = 0, oldLength = -1, newText = "a") }
    }
}
