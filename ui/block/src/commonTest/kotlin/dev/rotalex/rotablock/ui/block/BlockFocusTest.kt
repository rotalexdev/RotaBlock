package dev.rotalex.rotablock.ui.block

import androidx.compose.ui.geometry.Offset
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlockFocusTest {
    private val statement = owner("statement", listOf(leaf("abcd", id = "child")))

    private fun layoutOf(blocks: List<dev.rotalex.rotablock.core.block.BlockNode>) =
        layoutBlocks(treeOf(blocks), charSizer, testMetrics)

    @Test
    fun aClickOnAChipFocusesItsField() {
        val layout = layoutOf(listOf(statement))

        val token = layout.blocks.single().children.single().tokens.single()

        assertEquals(BlockFocus.Field(BlockId("child"), 0), focusAt(layout, token.rect.center))
    }

    @Test
    fun aClickOnChromeFocusesTheBlock() {
        val layout = layoutOf(listOf(statement))

        assertEquals(BlockFocus.Block(BlockId("statement")), focusAt(layout, Offset(8f, 40f)))
    }

    @Test
    fun aClickOnChromeFocusesTheBlockAndNotAReadOnlyField() {
        val layout = layoutOf(listOf(statement))

        val chrome = layout.blocks.single().tokens.single()

        assertEquals(BlockFocus.Block(BlockId("statement")), focusAt(layout, chrome.rect.center))
    }

    @Test
    fun aClickInsideTheMouthFocusesTheChildAndNotItsOwner() {
        val layout = layoutOf(listOf(statement))

        val child = layout.blocks.single().children.single()
        val point = Offset(child.rect.left + 4f, child.rect.top + 4f)

        assertEquals(BlockFocus.Block(BlockId("child")), focusAt(layout, point))
    }

    @Test
    fun aClickOnAGhostFocusesItsSlot() {
        val layout = layoutOf(listOf(owner("statement", listOf(leaf("a")), slots = 2)))

        val ghost = layout.blocks.single().ghost!!

        assertEquals(BlockFocus.Slot(BlockId("statement"), 1), focusAt(layout, ghost.rect.center))
    }

    @Test
    fun aClickOutsideEveryBlockFocusesNothing() {
        val layout = layoutOf(listOf(statement))

        assertNull(focusAt(layout, Offset(4000f, 4000f)))
    }

    @Test
    fun tabWalksTheHolesInOrderAndWraps() {
        val layout = layoutOf(
            listOf(
                owner("first", listOf(leaf("a")), slots = 2),
                owner("second", listOf(leaf("b")), slots = 2),
            ),
        )
        val first = BlockFocus.Slot(BlockId("first"), 1)
        val second = BlockFocus.Slot(BlockId("second"), 1)

        assertEquals(first, nextHole(layout, null))
        assertEquals(second, nextHole(layout, first))
        assertEquals(first, nextHole(layout, second))
        assertEquals(second, nextHole(layout, first, forward = false))
    }

    @Test
    fun tabStartsAtTheBlockItLeaves() {
        val layout = layoutOf(
            listOf(
                owner("first", listOf(leaf("a")), slots = 2),
                owner("second", listOf(leaf("b")), slots = 2),
            ),
        )

        assertEquals(
            BlockFocus.Slot(BlockId("second"), 1),
            nextHole(layout, BlockFocus.Block(BlockId("second"))),
        )
    }

    @Test
    fun arrowsWalkTheBlocksInReadingOrder() {
        val layout = layoutOf(listOf(statement, leaf("z", id = "last")))

        assertEquals(BlockFocus.Block(BlockId("statement")), nextBlock(layout, null))
        assertEquals(BlockFocus.Block(BlockId("child")), nextBlock(layout, BlockFocus.Block(BlockId("statement"))))
        assertEquals(BlockFocus.Block(BlockId("last")), nextBlock(layout, BlockFocus.Block(BlockId("child"))))
        assertEquals(BlockFocus.Block(BlockId("statement")), nextBlock(layout, BlockFocus.Block(BlockId("last"))))
        assertEquals(BlockFocus.Block(BlockId("last")), nextBlock(layout, BlockFocus.Block(BlockId("statement")), forward = false))
    }

    @Test
    fun aFocusThatStillResolvesKeepsItsPlace() {
        val tree = treeOf(listOf(statement))

        assertEquals(BlockFocus.Field(BlockId("child"), 0), reanchor(BlockFocus.Field(BlockId("child"), 0), tree))
        assertEquals(BlockFocus.Slot(BlockId("statement"), 0), reanchor(BlockFocus.Slot(BlockId("statement"), 0), tree))
    }

    @Test
    fun aFocusOnAVanishedBlockFallsBackToTheNearestSurvivingAncestor() {
        val tree = treeOf(
            listOf(
                owner("object#0", listOf(leaf("abcd", id = "object#0/value#0"))),
            ),
        )
        val gone = BlockFocus.Field(BlockId("object#0/value#0/deeper#1"), 0)

        assertEquals(BlockFocus.Block(BlockId("object#0/value#0")), reanchor(gone, tree))
    }

    @Test
    fun aFocusOnAVanishedTreeResolvesToNothing() {
        val tree = treeOf(listOf(statement))

        assertNull(reanchor(BlockFocus.Block(BlockId("elsewhere#0")), tree))
    }

    @Test
    fun aFocusOnChromeFallsBackToTheBlock() {
        val tree = treeOf(listOf(statement))

        assertEquals(BlockFocus.Block(BlockId("statement")), reanchor(BlockFocus.Field(BlockId("statement"), 0), tree))
    }

    @Test
    fun anIndexTheBlockNoLongerOffersFallsBackToTheBlock() {
        val tree = treeOf(listOf(statement))

        assertEquals(BlockFocus.Block(BlockId("child")), reanchor(BlockFocus.Field(BlockId("child"), 9), tree))
        assertEquals(BlockFocus.Block(BlockId("child")), reanchor(BlockFocus.Slot(BlockId("child"), 9), tree))
    }

    @Test
    fun aHoistedKeyKeepsTheBlockItWasProjectedFrom() {
        val container = owner("container", listOf(leaf("x", id = "inner")))
        val pair = withParts(
            id = "pair",
            category = SlotCategory.DECLARATION,
            parts = listOf(
                BlockPart.Field(field("cached")),
                BlockPart.Field(field(":", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, listOf(container))),
            ),
        )

        val layout = layoutOf(listOf(pair))

        val placed = layout.blocks.single()
        assertEquals(BlockId("container"), placed.block.id)
        assertEquals(BlockId("pair"), placed.tokens.first().blockId)
        assertEquals(
            BlockFocus.Field(BlockId("pair"), 0),
            focusAt(layout, placed.tokens.first().rect.center),
        )
    }

    @Test
    fun aBooleanKeepsItsFieldAttribution() {
        val value = leaf("true", id = "value", valueKind = ValueKind.BOOLEAN)
        val pair = withParts(
            id = "pair",
            category = SlotCategory.DECLARATION,
            parts = listOf(
                BlockPart.Field(field("cached")),
                BlockPart.Field(field(":", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, listOf(value), ValueKind.BOOLEAN)),
            ),
        )

        val layout = layoutOf(listOf(pair))

        val booleanToken = layout.blocks.single().tokens.last()
        assertEquals(BlockId("value"), booleanToken.blockId)
        assertEquals(
            BlockFocus.Field(BlockId("value"), 0),
            focusAt(layout, booleanToken.rect.center),
        )
    }
}
