package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LayoutPolicyTest {
    private val element = TextRange(1, 2)

    @Test
    fun theNaivePolicyHasNoSeparatorIndentOrEdits() {
        assertEquals(SeparatorStyle.None, LayoutPolicy.NONE.separator)
        assertEquals(IndentStyle.None, LayoutPolicy.NONE.indent)
        assertTrue(!LayoutPolicy.NONE.trailingSeparator)
        assertTrue(LayoutPolicy.NONE.onDelete(element, LayoutContext(element, null, null)).isEmpty())
        assertTrue(LayoutPolicy.NONE.onInsert(1, "x", LayoutContext(element, null, null)).isEmpty())
    }

    @Test
    fun aCustomSeparatorCarriesBothSides() {
        val custom = SeparatorStyle.Custom(leading = "", trailing = " -> ")
        assertEquals("", custom.leading)
        assertEquals(" -> ", custom.trailing)
    }

    @Test
    fun anIndentUnitIsPositive() {
        assertEquals(4, IndentStyle.Spaces(4).unit)
        assertFailsWith<IllegalArgumentException> { IndentStyle.Spaces(0) }
    }

    @Test
    fun theContextReportsTheNeighboursItWasGiven() {
        val previous = TextRange(0, 1)
        val next = TextRange(3, 4)
        val context = LayoutContext(element, previous, next)
        assertEquals(element, context.element)
        assertEquals(previous, context.previous)
        assertEquals(next, context.next)
    }

    @Test
    fun aPolicyAnswersForOwnedSyntaxWithAbsoluteEdits() {
        val policy = object : LayoutPolicy {
            override val separator: SeparatorStyle = SeparatorStyle.Comma
            override val trailingSeparator: Boolean = false
            override val indent: IndentStyle = IndentStyle.None
            override fun onDelete(elementRange: TextRange, context: LayoutContext): List<DocumentEdit> =
                listOf(DocumentEdit(elementRange.end, 1, ""))
            override fun onInsert(offset: Int, elementText: String, context: LayoutContext): List<DocumentEdit> =
                listOf(DocumentEdit(offset + elementText.length, 0, ", "))
        }
        val context = LayoutContext(element, null, null)
        assertEquals(listOf(DocumentEdit(2, 1, "")), policy.onDelete(element, context))
        assertEquals(listOf(DocumentEdit(3, 0, ", ")), policy.onInsert(1, "xy", context))
    }
}
