package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.DomNode
import dev.rotalex.rotablock.language.dom.NodeKind
import dev.rotalex.rotablock.language.dom.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProjectionContextTest {
    private val source = "f(42)"
    private val node = DomNode(NodeKind("Call"), TextRange(0, 5), source)

    @Test
    fun exposesTheSourceItProjects() {
        assertEquals(source, FakeProjectionContext(source).source.toString())
    }

    @Test
    fun readsTheTextOfANode() {
        assertEquals("f(42)", FakeProjectionContext(source).textOf(node))
    }

    @Test
    fun carveReportsTheNodeItInterleaves() {
        val context = FakeProjectionContext(source)
        context.carve(node)
        assertEquals(listOf(node), context.carved)
    }

    @Test
    fun childProjectsOneNodeInIsolation() {
        val context = FakeProjectionContext(source)
        val block = context.child(node)
        assertEquals(listOf(node), context.projected)
        assertEquals(NodeKind("Call"), block.kind)
    }

    @Test
    fun theFactoriesKeepWhatTheyAreGiven() {
        val context = FakeProjectionContext(source)
        val block = context.block(NodeKind("Leaf"), TextRange(1, 3), emptyList(), node, "leaf", ValueKind.NUMBER)
        assertEquals("leaf", block.label)
        assertEquals(ValueKind.NUMBER, block.valueKind)
        assertSame(node, block.anchor)
        val slot = context.slot(SlotCategory.EXPRESSION, listOf(block), true, TextRange(1, 3), ValueKind.NUMBER)
        assertEquals(listOf(block), slot.children)
        val field = context.field("value", "42", true, TextRange(2, 4))
        assertTrue(field.editable)
    }

    @Test
    fun eachBlockGetsItsOwnId() {
        val context = FakeProjectionContext(source)
        val first = context.child(node)
        val second = context.child(node)
        assertTrue(first.id != second.id)
    }
}
