package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockLayoutTest {
    @Test
    fun aLeafIsOneCardSizedByItsToken() {
        val layout = layoutBlocks(treeOf(listOf(leaf("abcdefghij"))), charSizer, testMetrics)

        val block = layout.blocks.single()
        assertEquals(112f, block.rect.width)
        assertEquals(30f, block.rect.height)
        assertEquals(112f, layout.width)
        assertEquals(30f, layout.height)
    }

    @Test
    fun topLevelBlocksAreStackedInOrder() {
        val layout = layoutBlocks(
            treeOf(listOf(leaf("a", id = "a"), leaf("b", id = "b"))),
            charSizer,
            testMetrics,
        )

        val first = layout.blocks[0]
        val second = layout.blocks[1]
        assertEquals(0f, first.rect.top)
        assertEquals(first.rect.bottom + testMetrics.rowGap, second.rect.top)
        assertEquals(65f, layout.height)
    }

    @Test
    fun nestedBlocksAreIndentedInsideTheirOwner() {
        val inner = owner("inner", listOf(leaf("abcd", id = "child")))
        val layout = layoutBlocks(treeOf(listOf(owner("statement", listOf(inner)))), charSizer, testMetrics)

        val block = layout.blocks.single()
        val nested = block.children.single()
        assertEquals(block.rect.left + testMetrics.indent, nested.rect.left)
        assertEquals(testMetrics.paddingY + testMetrics.textHeight + testMetrics.rowGap, nested.rect.top)
        assertEquals(testMetrics.paddingY * 2 + testMetrics.textHeight, nested.rect.height)
    }

    @Test
    fun aBlockWithChildrenCarriesATab() {
        val layout = layoutBlocks(
            treeOf(listOf(owner("statement", listOf(owner("inner", listOf(leaf("x", id = "x"))))))),
            charSizer,
            testMetrics,
        )

        val block = layout.blocks.single()
        val tab = assertNotNull(block.tab)
        assertEquals(block.rect.bottom, tab.top)
        assertEquals(testMetrics.tabWidth, tab.width)
        assertEquals(testMetrics.tabHeight, tab.height)
        assertEquals(block.rect.left, tab.left)
    }

    @Test
    fun aLeafCarriesNoTab() {
        val layout = layoutBlocks(treeOf(listOf(leaf("x"))), charSizer, testMetrics)

        assertNull(layout.blocks.single().tab)
    }

    @Test
    fun anOwnerGrowsAroundItsChildren() {
        val layout = layoutBlocks(
            treeOf(listOf(owner("statement", listOf(leaf("a", id = "a"), leaf("b", id = "b"))))),
            charSizer,
            testMetrics,
        )

        val block = layout.blocks.single()
        val nested = block.children
        val nestedBottom = nested.last().rect.bottom
        assertTrue(block.rect.bottom > nestedBottom)
        assertEquals(
            testMetrics.paddingY * 2 + testMetrics.textHeight + testMetrics.rowGap +
                nested[0].rect.height + testMetrics.rowGap + nested[1].rect.height,
            block.rect.height,
        )
        assertEquals(block.rect.bottom + testMetrics.tabHeight, layout.height)
    }

    @Test
    fun anEmptySlotPlacesAGhostInsideItsBlock() {
        val block = withParts(
            id = "hole",
            parts = listOf(
                BlockPart.Field(field("[]", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, emptyList())),
            ),
        )

        val layout = layoutBlocks(treeOf(listOf(block)), charSizer, testMetrics)

        val placed = layout.blocks.single()
        val ghost = assertNotNull(placed.ghost)
        assertEquals("expression", ghost.label)
        assertTrue(placed.rect.contains(ghost.rect.topLeft) && placed.rect.contains(ghost.rect.bottomRight))
        assertNull(layout.ghost)
    }

    @Test
    fun anEmptyDocumentPlacesOneGhost() {
        val layout = layoutBlocks(treeOf(emptyList()), charSizer, testMetrics)

        assertTrue(layout.blocks.isEmpty())
        val ghost = assertNotNull(layout.ghost)
        assertEquals("value", ghost.label)
        assertEquals(testMetrics.textHeight, layout.height)
        assertEquals(ghost.rect.right, layout.width)
    }

    @Test
    fun aSingleLeafValueSharesItsOwnersLine() {
        val value = leaf("\"/greeting\"", id = "value", valueKind = ValueKind.STRING)
        val pair = withParts(
            id = "pair",
            parts = listOf(
                BlockPart.Field(field("path")),
                BlockPart.Field(field("\":\"", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, listOf(value), ValueKind.STRING)),
            ),
        )

        val layout = layoutBlocks(treeOf(listOf(pair)), charSizer, testMetrics)

        val placed = layout.blocks.single()
        assertTrue(placed.children.isEmpty())
        assertNull(placed.tab)
        assertEquals(3, placed.tokens.size)
        assertTrue(placed.tokens.all { it.rect.center.y == placed.tokens[0].rect.center.y })
        assertTrue(placed.tokens[2].chip)
        assertEquals(testMetrics.paddingY * 2 + testMetrics.textHeight, placed.rect.height)
    }

    @Test
    fun aBlockIsNeverNarrowerThanTheMinimum() {
        val layout = layoutBlocks(treeOf(listOf(leaf("a"))), charSizer, testMetrics)

        assertEquals(testMetrics.minBlockWidth, layout.blocks.single().rect.width)
    }

    @Test
    fun theSurfaceCoversItsWidestBlock() {
        val layout = layoutBlocks(
            treeOf(listOf(leaf("a", id = "short"), leaf("abcdefghijklmnop", id = "long"))),
            charSizer,
            testMetrics,
        )

        assertEquals(layout.blocks.maxOf { it.rect.right }, layout.width)
    }
}
