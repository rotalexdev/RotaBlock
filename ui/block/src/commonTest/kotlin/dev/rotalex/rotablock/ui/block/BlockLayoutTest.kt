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
    fun aLeafIsOneBarSizedByItsChip() {
        val layout = layoutBlocks(treeOf(listOf(leaf("abcdefghij"))), charSizer, testMetrics)

        val block = layout.blocks.single()
        assertEquals(128f, block.rect.width)
        assertEquals(48f, block.rect.height)
        assertEquals(128f, layout.width)
        assertEquals(16.5f, block.rect.top)
        assertEquals(64.5f, layout.height)
    }

    @Test
    fun topLevelBlocksMeetAtTheNotch() {
        val layout = layoutBlocks(
            treeOf(listOf(leaf("a", id = "a"), leaf("b", id = "b"))),
            charSizer,
            testMetrics,
        )

        val first = layout.blocks[0]
        val second = layout.blocks[1]
        assertEquals(first.rect.bottom, second.rect.top)
        assertEquals(true, second.notch)
        assertEquals(false, first.notch)
        assertEquals(true, first.tab)
        assertEquals(false, second.tab)
        assertEquals(112.5f, layout.height)
    }

    @Test
    fun theRootBlockCarriesAHat() {
        val layout = layoutBlocks(
            treeOf(listOf(leaf("a", id = "a"), leaf("b", id = "b"))),
            charSizer,
            testMetrics,
        )

        assertEquals(true, layout.blocks[0].hat)
        assertEquals(false, layout.blocks[1].hat)
    }

    @Test
    fun nestedBlocksSitInsideTheMouthOfTheirOwner() {
        val inner = owner("inner", listOf(leaf("abcd", id = "child")))
        val layout = layoutBlocks(treeOf(listOf(owner("statement", listOf(inner)))), charSizer, testMetrics)

        val block = layout.blocks.single()
        val nested = block.children.single()
        assertEquals(testMetrics.mouthIndent, nested.rect.left - block.rect.left)
        assertEquals(block.rect.top + block.openHeight, nested.rect.top)
        assertEquals(144f, nested.rect.height)
        assertEquals(false, nested.notch)
    }

    @Test
    fun onlyChildrenBelowTheFirstCarryANotch() {
        val layout = layoutBlocks(
            treeOf(listOf(owner("statement", listOf(leaf("a", id = "a"), leaf("b", id = "b"))))),
            charSizer,
            testMetrics,
        )

        val children = layout.blocks.single().children
        assertEquals(false, children[0].notch)
        assertEquals(true, children[1].notch)
    }

    @Test
    fun anOwnerClosesItsMouthWithABar() {
        val layout = layoutBlocks(
            treeOf(listOf(owner("statement", listOf(leaf("a", id = "a"), leaf("b", id = "b"))))),
            charSizer,
            testMetrics,
        )

        val block = layout.blocks.single()
        val nested = block.children
        assertEquals(48f, block.openHeight)
        assertEquals(48f, block.closeHeight)
        assertEquals(nested.last().rect.bottom, block.rect.bottom - block.closeHeight)
        assertEquals(48f + 48f + 48f + 48f, block.rect.height)
    }

    @Test
    fun anEmptySlotPlacesAGhostOnItsBar() {
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
        assertEquals(testMetrics.chipHeight, layout.height)
        assertEquals(ghost.rect.right, layout.width)
    }

    @Test
    fun aSingleLeafValueSharesItsOwnersBar() {
        val value = leaf("\"/greeting\"", id = "value", valueKind = ValueKind.STRING)
        val pair = withParts(
            id = "pair",
            category = SlotCategory.DECLARATION,
            parts = listOf(
                BlockPart.Field(field("path")),
                BlockPart.Field(field("\":\"", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, listOf(value), ValueKind.STRING)),
            ),
        )

        val layout = layoutBlocks(treeOf(listOf(pair)), charSizer, testMetrics)

        val placed = layout.blocks.single()
        assertTrue(placed.children.isEmpty())
        assertEquals(3, placed.tokens.size)
        assertTrue(placed.tokens.all { it.rect.center.y == placed.tokens[0].rect.center.y })
        assertTrue(placed.tokens[2].chip)
        assertEquals(48f, placed.rect.height)
    }

    @Test
    fun aMemberLendsItsLabelToTheContainerItIntroduces() {
        val member = withParts(
            id = "member",
            category = SlotCategory.DECLARATION,
            parts = listOf(
                BlockPart.Field(field("inner")),
                BlockPart.Field(field(":", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, listOf(leaf("1", id = "value", valueKind = ValueKind.NUMBER)))),
            ),
        )
        val container = withParts(
            id = "object",
            category = SlotCategory.EXPRESSION,
            valueKind = ValueKind.OBJECT,
            parts = listOf(
                BlockPart.Field(field("{", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.DECLARATION, listOf(member), multiple = true)),
                BlockPart.Field(field("}", editable = false)),
            ),
        )
        val pair = withParts(
            id = "pair",
            category = SlotCategory.DECLARATION,
            parts = listOf(
                BlockPart.Field(field("body")),
                BlockPart.Field(field(":", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, listOf(container), ValueKind.OBJECT)),
            ),
        )

        val layout = layoutBlocks(treeOf(listOf(pair)), charSizer, testMetrics)

        val placed = layout.blocks.single()
        assertEquals("object", placed.block.id.value)
        assertEquals(3, placed.tokens.size)
        assertEquals("{", placed.tokens.last().text)
        assertEquals(1, placed.children.size)
        assertEquals(setOf("}"), placed.closingTokens.map { it.text }.toSet())
    }

    @Test
    fun stackedMembersFillEachOthersNotches() {
        val container = withParts(
            id = "object",
            category = SlotCategory.EXPRESSION,
            valueKind = ValueKind.OBJECT,
            parts = listOf(
                BlockPart.Field(field("{", editable = false)),
                BlockPart.Slot(
                    slotOf(
                        SlotCategory.DECLARATION,
                        listOf(leaf("1", id = "a", valueKind = ValueKind.NUMBER), leaf("2", id = "b", valueKind = ValueKind.NUMBER)),
                        multiple = true,
                    ),
                ),
                BlockPart.Field(field("}", editable = false)),
            ),
        )

        val layout = layoutBlocks(treeOf(listOf(container)), charSizer, testMetrics)
        val placed = layout.blocks.single()
        val first = placed.children[0]
        val second = placed.children[1]

        assertEquals(first.rect.bottom, second.rect.top)
        assertEquals(true, first.tab)
        assertEquals(false, second.tab)
        assertEquals(placed.rect.bottom - placed.closeHeight, second.rect.bottom)
    }

    @Test
    fun aBlockIsNeverNarrowerThanTheMinimum() {
        val layout = layoutBlocks(treeOf(listOf(withParts(id = "bare", parts = emptyList()))), charSizer, testMetrics)

        val block = layout.blocks.single()
        assertEquals(testMetrics.minWidth, block.rect.width)
        assertEquals(testMetrics.minBarHeight, block.rect.height)
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

    @Test
    fun aBooleanEndsInAPoint() {
        val layout = layoutBlocks(
            treeOf(listOf(leaf("true", valueKind = ValueKind.BOOLEAN))),
            charSizer,
            testMetrics,
        )

        val block = layout.blocks.single()
        assertEquals(96f, block.rect.width)
        val corners = outlineOf(block, testMetrics)
        assertEquals(6, corners.size)
        assertEquals(block.rect.left + testMetrics.hexCut, corners[0].point.x)
        assertEquals(block.rect.right, corners[2].point.x)
        assertEquals(block.rect.center.y, corners[2].point.y)
        assertEquals(block.rect.left, corners[5].point.x)
    }

    @Test
    fun aBooleanSlotDrawsAHexagonalGhost() {
        val block = withParts(
            id = "hole",
            parts = listOf(
                BlockPart.Field(field("[", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, emptyList(), ValueKind.BOOLEAN)),
            ),
        )

        val layout = layoutBlocks(treeOf(listOf(block)), charSizer, testMetrics)

        val ghost = assertNotNull(layout.blocks.single().ghost)
        assertEquals("boolean", ghost.label)
        assertTrue(ghost.boolean)
    }

    @Test
    fun aNotchedBlockCutsItsTopEdgeByOneGridStep() {
        val layout = layoutBlocks(
            treeOf(listOf(leaf("a", id = "a"), leaf("b", id = "b"))),
            charSizer,
            testMetrics,
        )

        val notched = layout.blocks[1]
        val corners = outlineOf(notched, testMetrics)
        assertEquals(8, corners.size)
        val cut = corners.filter { it.point.y > notched.rect.top && it.point.y < notched.rect.bottom }
        assertEquals(2, cut.size)
        assertEquals(testMetrics.notchHeight, cut[0].point.y - notched.rect.top)
        assertEquals(testMetrics.notchWidth, cut[1].point.x - cut[0].point.x)
    }

    @Test
    fun anInlineBooleanKeepsItsPointedShape() {
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

        val layout = layoutBlocks(treeOf(listOf(pair)), charSizer, testMetrics)

        val placed = layout.blocks.single()
        assertEquals(false, placed.boolean)
        val booleanToken = placed.tokens.last()
        assertEquals(testMetrics.hexCut, booleanToken.lead)
        assertEquals(booleanToken.rect.left + booleanToken.rect.width, placed.rect.right - testMetrics.barPadding)
    }
}
