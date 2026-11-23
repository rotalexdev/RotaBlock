package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class BlockTreeTest {
    private val source = "a(b)c"

    private fun leaf(id: String, start: Int, end: Int) = BlockNode(
        id = BlockId(id),
        kind = NodeKind("Leaf"),
        anchor = DomNode(NodeKind("Leaf"), TextRange(start, end), source),
        range = TextRange(start, end),
        parts = emptyList(),
    )

    private fun wrapping(rootRange: TextRange, child: BlockNode) = BlockNode(
        id = BlockId("root"),
        kind = NodeKind.ROOT,
        anchor = DomNode(NodeKind.ROOT, rootRange, source),
        range = rootRange,
        parts = listOf(BlockPart.Slot(BlockSlot(SlotCategory.STATEMENT, listOf(child), true, null, ValueKind.UNKNOWN))),
    )

    @Test
    fun exposesItsRootAndVersion() {
        val root = leaf("root", 0, 5)
        val tree = BlockTree(root, 9L)
        assertSame(root, tree.root)
        assertEquals(9L, tree.version)
    }

    @Test
    fun findsABlockById() {
        val child = leaf("inner", 1, 4)
        val tree = BlockTree(wrapping(TextRange(0, 5), child), 1L)
        assertSame(child, tree.block(BlockId("inner")))
    }

    @Test
    fun findsNestedBlocksAtAnyDepth() {
        val deepest = leaf("deep", 2, 3)
        val middle = BlockNode(
            id = BlockId("middle"),
            kind = NodeKind("Middle"),
            anchor = DomNode(NodeKind("Middle"), TextRange(1, 4), source),
            range = TextRange(1, 4),
            parts = listOf(BlockPart.Slot(BlockSlot(SlotCategory.OPAQUE, listOf(deepest), false, null, ValueKind.UNKNOWN))),
        )
        val tree = BlockTree(wrapping(TextRange(0, 5), middle), 1L)
        assertSame(middle, tree.block(BlockId("middle")))
        assertSame(deepest, tree.block(BlockId("deep")))
    }

    @Test
    fun anUnknownIdResolvesToNothing() {
        assertNull(BlockTree(leaf("root", 0, 5), 1L).block(BlockId("absent")))
    }

    @Test
    fun blockAtReturnsTheDeepestContainingBlock() {
        val child = leaf("inner", 1, 4)
        val tree = BlockTree(wrapping(TextRange(0, 5), child), 1L)
        assertSame(child, tree.blockAt(2))
        assertSame(tree.root, tree.blockAt(0))
    }

    @Test
    fun blockAtReturnsNothingOutsideTheRoot() {
        assertNull(BlockTree(leaf("root", 0, 3), 1L).blockAt(3))
    }

    @Test
    fun rejectsDuplicateBlockIds() {
        val first = leaf("same", 1, 2)
        val second = leaf("same", 3, 4)
        val root = BlockNode(
            id = BlockId("root"),
            kind = NodeKind.ROOT,
            anchor = DomNode(NodeKind.ROOT, TextRange(0, 5), source),
            range = TextRange(0, 5),
            parts = listOf(
                BlockPart.Slot(BlockSlot(SlotCategory.STATEMENT, listOf(first), true, null, ValueKind.UNKNOWN)),
                BlockPart.Slot(BlockSlot(SlotCategory.STATEMENT, listOf(second), true, null, ValueKind.UNKNOWN)),
            ),
        )
        assertFailsWith<IllegalArgumentException> { BlockTree(root, 1L) }
    }
}
