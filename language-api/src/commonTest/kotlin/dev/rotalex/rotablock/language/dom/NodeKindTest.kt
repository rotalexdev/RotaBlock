package dev.rotalex.rotablock.language.dom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NodeKindTest {
    @Test
    fun ordersByIdForDeterministicOrdering() {
        val kinds = listOf(NodeKind("zzz"), NodeKind("aaa"), NodeKind.ROOT)
        assertEquals(listOf(NodeKind("aaa"), NodeKind.ROOT, NodeKind("zzz")), kinds.sorted())
    }

    @Test
    fun comparesByIds() {
        assertTrue(NodeKind("aaa") < NodeKind("bbb"))
        assertEquals(0, NodeKind("same").compareTo(NodeKind("same")))
    }

    @Test
    fun kindsWithTheSameIdAreEqual() {
        assertEquals(NodeKind("CallExpression"), NodeKind("CallExpression"))
    }

    @Test
    fun neutralConstantsAreDistinct() {
        assertNotEquals(NodeKind.ROOT, NodeKind.ERROR)
        assertEquals("root", NodeKind.ROOT.id)
        assertEquals("error", NodeKind.ERROR.id)
    }

    @Test
    fun rejectsBlankId() {
        assertFailsWith<IllegalArgumentException> { NodeKind(" ") }
    }
}
