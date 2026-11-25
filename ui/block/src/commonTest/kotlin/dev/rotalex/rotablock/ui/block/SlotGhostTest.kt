package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SlotGhostTest {
    @Test
    fun aFilledSlotHasNoGhost() {
        assertNull(ghostOf(slotOf(SlotCategory.STATEMENT, listOf(leaf("x", id = "x")))))
    }

    @Test
    fun anEmptySlotOfUnknownShapeNamesItsGrammar() {
        val ghost = ghostOf(slotOf(SlotCategory.EXPRESSION, emptyList()))

        assertEquals("expression", ghost?.label)
        assertEquals(SlotCategory.EXPRESSION, ghost?.category)
    }

    @Test
    fun aSlotShapedByAValueNamesTheShape() {
        assertEquals("text", ghostOf(slotOf(SlotCategory.EXPRESSION, emptyList(), ValueKind.STRING))?.label)
        assertEquals("object", ghostOf(slotOf(SlotCategory.EXPRESSION, emptyList(), ValueKind.OBJECT))?.label)
        assertEquals("array", ghostOf(slotOf(SlotCategory.OPAQUE, emptyList(), ValueKind.ARRAY))?.label)
        assertEquals("number", ghostOf(slotOf(SlotCategory.OPAQUE, emptyList(), ValueKind.NUMBER))?.label)
    }

    @Test
    fun everyCategoryNamesItself() {
        assertEquals("statement", labelOf(SlotCategory.STATEMENT, ValueKind.UNKNOWN))
        assertEquals("declaration", labelOf(SlotCategory.DECLARATION, ValueKind.UNKNOWN))
        assertEquals("type", labelOf(SlotCategory.TYPE, ValueKind.UNKNOWN))
        assertEquals("name", labelOf(SlotCategory.NAME, ValueKind.UNKNOWN))
        assertEquals("modifier", labelOf(SlotCategory.MODIFIER, ValueKind.UNKNOWN))
        assertEquals("parameter", labelOf(SlotCategory.PARAMETER, ValueKind.UNKNOWN))
        assertEquals("argument", labelOf(SlotCategory.ARGUMENT, ValueKind.UNKNOWN))
        assertEquals("value", labelOf(SlotCategory.OPAQUE, ValueKind.UNKNOWN))
    }
}
