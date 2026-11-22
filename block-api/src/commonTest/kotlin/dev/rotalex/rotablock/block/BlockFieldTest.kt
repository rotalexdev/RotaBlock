package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.DomNode
import dev.rotalex.rotablock.language.dom.NodeKind
import dev.rotalex.rotablock.language.dom.TextRange

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockFieldTest {
    @Test
    fun keepsRoleTextEditabilityAndRange() {
        val field = BlockField("value", "42", editable = true, range = TextRange(3, 5))
        assertEquals("value", field.role)
        assertEquals("42", field.text)
        assertTrue(field.editable)
        assertEquals(TextRange(3, 5), field.range)
    }

    @Test
    fun chromeIsReadOnly() {
        assertFalse(BlockField.chrome("open", "(").editable)
    }

    @Test
    fun chromeMayHaveNoBackingText() {
        assertNull(BlockField.chrome("open", "(").range)
    }

    @Test
    fun chromeMayCarryARange() {
        assertEquals(TextRange(0, 1), BlockField.chrome("open", "(", TextRange(0, 1)).range)
    }

    @Test
    fun anEditableFieldMustCarryARange() {
        assertFailsWith<IllegalArgumentException> { BlockField("value", "42", editable = true, range = null) }
    }

    @Test
    fun rejectsBlankRole() {
        assertFailsWith<IllegalArgumentException> { BlockField(" ", "42", editable = false, range = null) }
    }
}
