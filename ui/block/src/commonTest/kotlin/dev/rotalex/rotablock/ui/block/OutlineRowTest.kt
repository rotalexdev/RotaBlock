package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.SlotCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutlineRowTest {
    @Test
    fun theOutlineListsTheBlocksBelowTheRoot() {
        val first = leaf("a", id = "a")
        val second = leaf("b", id = "b")

        val rows = outlineRows(treeOf(listOf(first, second)).root)

        assertEquals(listOf(first, second), rows.map { it.block })
        assertTrue(rows.all { it.children.isEmpty() })
    }

    @Test
    fun aRowCountsItsWholeSubtree() {
        val rows = outlineRows(treeOf(listOf(owner("statement", listOf(leaf("a", id = "a"), leaf("b", id = "b"))))).root)

        assertEquals(3, rows.single().descendantCount)
    }

    @Test
    fun nestedRowsFollowTheirOwner() {
        val nested = leaf("nested", id = "nested")
        val rows = outlineRows(treeOf(listOf(owner("outer", listOf(owner("inner", listOf(nested)))))).root)

        val outer = rows.single()
        val inner = outer.children.single()
        assertEquals(listOf(nested), inner.children.map { it.block })
        assertEquals(3, outer.descendantCount)
        assertEquals(2, inner.descendantCount)
    }

    @Test
    fun aLabelPrefersTheBlocksOwnLabel() {
        val block = withParts(
            id = "labelled",
            parts = listOf(BlockPart.Field(field("visible text"))),
            label = "own label",
        )

        assertEquals("own label", displayLabel(block))
    }

    @Test
    fun aLabelFallsBackToEditableText() {
        val block = withParts(
            id = "editable",
            parts = listOf(
                BlockPart.Field(field("syntax", editable = false)),
                BlockPart.Field(field("editable text")),
            ),
        )

        assertEquals("editable text", displayLabel(block))
    }

    @Test
    fun aLabelFallsBackToTheKind() {
        val block = withParts(
            id = "bare",
            parts = listOf(BlockPart.Slot(slotOf(SlotCategory.STATEMENT, emptyList()))),
            kind = "json.member",
        )

        assertEquals("member", displayLabel(block))
    }

    @Test
    fun aLongLabelIsCutToTwentyFourCharacters() {
        val block = withParts(id = "long", parts = listOf(BlockPart.Field(field("a".repeat(30)))))

        assertEquals("a".repeat(24) + "…", displayLabel(block))
    }

    @Test
    fun anEmptyDocumentHasNoRows() {
        assertTrue(outlineRows(treeOf(emptyList()).root).isEmpty())
    }
}
