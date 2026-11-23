package dev.rotalex.rotablock.core.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class DomNodeTest {
    private val source = "(a b)"

    @Test
    fun textReturnsTheExactSliceOfItsRange() {
        val node = DomNode(NodeKind.ROOT, TextRange(1, 2), source)
        assertEquals("a", node.text())
    }

    @Test
    fun textOfAnEmptyRangeIsEmpty() {
        assertEquals("", DomNode(NodeKind.ROOT, TextRange(2, 2), source).text())
    }

    @Test
    fun keepsChildrenInSourceOrder() {
        val first = DomNode(NodeKind("Identifier"), TextRange(1, 2), source)
        val second = DomNode(NodeKind("Identifier"), TextRange(3, 4), source)
        val root = DomNode(NodeKind.ROOT, TextRange(0, 5), source, listOf(first, second))
        assertEquals(listOf(first, second), root.children)
    }

    @Test
    fun assignsItselfAsTheParentOfEachChild() {
        val child = DomNode(NodeKind("Identifier"), TextRange(1, 2), source)
        val root = DomNode(NodeKind.ROOT, TextRange(0, 5), source, listOf(child))
        assertSame(root, child.parent)
    }

    @Test
    fun aDetachedNodeHasNoParent() {
        assertNull(DomNode(NodeKind.ROOT, TextRange(0, 5), source).parent)
    }

    @Test
    fun rejectsARangeBeyondTheSource() {
        assertFailsWith<IllegalArgumentException> { DomNode(NodeKind.ROOT, TextRange(0, 6), source) }
    }

    @Test
    fun rejectsAChildOutsideItsOwnRange() {
        val child = DomNode(NodeKind("Identifier"), TextRange(4, 5), source)
        assertFailsWith<IllegalArgumentException> {
            DomNode(NodeKind.ROOT, TextRange(0, 3), source, listOf(child))
        }
    }
}
