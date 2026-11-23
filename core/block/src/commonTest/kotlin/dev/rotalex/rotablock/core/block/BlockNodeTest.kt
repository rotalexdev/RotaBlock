package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlockNodeTest {
    private val range = TextRange(0, 4)
    private val source = "f(42)"

    @Test
    fun keepsIdKindAnchorRangePartsLabelAndValueKind() {
        val anchor = DomNode(NodeKind.ROOT, range, source)
        val node = BlockNode(
            id = BlockId("call"),
            kind = NodeKind("CallExpression"),
            anchor = anchor,
            range = range,
            parts = emptyList(),
            label = "call",
            valueKind = ValueKind.NUMBER,
        )
        assertEquals(BlockId("call"), node.id)
        assertEquals(NodeKind("CallExpression"), node.kind)
        assertEquals(anchor, node.anchor)
        assertEquals(range, node.range)
        assertEquals("call", node.label)
        assertEquals(ValueKind.NUMBER, node.valueKind)
    }

    @Test
    fun aSyntheticChromeBlockHasNoAnchor() {
        val node = BlockNode(BlockId("chrome"), NodeKind.ROOT, null, range, emptyList())
        assertNull(node.anchor)
    }

    @Test
    fun labelIsAbsentByDefault() {
        assertNull(BlockNode(BlockId("node"), NodeKind.ROOT, null, range, emptyList()).label)
    }

    @Test
    fun valueKindDefaultsToUnknown() {
        assertEquals(ValueKind.UNKNOWN, BlockNode(BlockId("node"), NodeKind.ROOT, null, range, emptyList()).valueKind)
    }

    @Test
    fun fieldsAndSlotsAreViewsOverPartsInOrder() {
        val first = BlockField.chrome("open", "(")
        val slot = BlockSlot(SlotCategory.EXPRESSION, emptyList(), false, null, ValueKind.NUMBER)
        val second = BlockField.chrome("close", ")")
        val node = BlockNode(
            BlockId("call"),
            NodeKind.ROOT,
            null,
            range,
            listOf(BlockPart.Field(first), BlockPart.Slot(slot), BlockPart.Field(second)),
        )
        assertEquals(listOf(first, second), node.fields)
        assertEquals(listOf(slot), node.slots)
        assertEquals(3, node.parts.size)
    }
}
