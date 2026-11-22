package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.DomNode
import dev.rotalex.rotablock.language.dom.NodeKind
import dev.rotalex.rotablock.language.dom.TextRange

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockSlotTest {
    @Test
    fun keepsCategoryChildrenMultiplicityRangeAndValueKind() {
        val slot = BlockSlot(
            category = SlotCategory.STATEMENT,
            children = emptyList(),
            multiple = true,
            range = TextRange(0, 4),
            valueKind = ValueKind.OBJECT,
        )
        assertEquals(SlotCategory.STATEMENT, slot.category)
        assertTrue(slot.children.isEmpty())
        assertTrue(slot.multiple)
        assertEquals(TextRange(0, 4), slot.range)
        assertEquals(ValueKind.OBJECT, slot.valueKind)
    }

    @Test
    fun aSyntheticSlotMayHaveNoRange() {
        val slot = BlockSlot(SlotCategory.OPAQUE, emptyList(), multiple = false, range = null, valueKind = ValueKind.UNKNOWN)
        assertNull(slot.range)
        assertFalse(slot.multiple)
    }

    @Test
    fun keepsItsChildrenInOrder() {
        val first = syntheticBlock("first")
        val second = syntheticBlock("second")
        val slot = BlockSlot(SlotCategory.STATEMENT, listOf(first, second), true, null, ValueKind.UNKNOWN)
        assertEquals(listOf(first, second), slot.children)
    }
}

private fun syntheticBlock(id: String) = BlockNode(
    id = BlockId(id),
    kind = NodeKind.ROOT,
    anchor = null,
    range = TextRange(0, 0),
    parts = emptyList(),
)
