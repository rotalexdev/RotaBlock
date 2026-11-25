package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.SlotCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BlockLineTest {
    @Test
    fun consecutiveFieldsShareOneLine() {
        val block = withParts(
            id = "pair",
            parts = listOf(BlockPart.Field(field("a")), BlockPart.Field(field("b"))),
        )

        val lines = linesOf(block)

        assertEquals(1, lines.size)
        assertEquals(listOf("a", "b"), assertIs<BlockLine.Tokens>(lines.single()).fields.map { it.text })
    }

    @Test
    fun anEmptySlotBecomesAHole() {
        val block = withParts(
            id = "statement",
            parts = listOf(
                BlockPart.Field(field("if", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, emptyList())),
            ),
        )

        val lines = linesOf(block)

        assertEquals(2, lines.size)
        assertIs<BlockLine.Tokens>(lines[0])
        val hole = assertIs<BlockLine.Hole>(lines[1])
        assertEquals(0, hole.slotIndex)
        assertEquals("expression", hole.ghost.label)
        assertEquals(SlotCategory.EXPRESSION, hole.ghost.category)
    }

    @Test
    fun aFilledSlotBecomesItsChildren() {
        val child = leaf("x", id = "child")
        val block = withParts(
            id = "statement",
            parts = listOf(BlockPart.Slot(slotOf(SlotCategory.STATEMENT, listOf(child)))),
        )

        val lines = linesOf(block)

        val children = assertIs<BlockLine.Children>(lines.single())
        assertEquals(0, children.slotIndex)
        assertEquals(listOf(child), children.blocks)
    }

    @Test
    fun linesKeepPartOrderAroundSlots() {
        val nested = leaf("nested", id = "nested")
        val block = withParts(
            id = "mixed",
            parts = listOf(
                BlockPart.Field(field("open", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.STATEMENT, listOf(nested))),
                BlockPart.Field(field("close", editable = false)),
            ),
        )

        val lines = linesOf(block)

        assertEquals(3, lines.size)
        assertEquals("open", assertIs<BlockLine.Tokens>(lines[0]).fields.single().text)
        assertIs<BlockLine.Children>(lines[1])
        assertEquals("close", assertIs<BlockLine.Tokens>(lines[2]).fields.single().text)
    }

    @Test
    fun slotIndexesCountSlotsOnly() {
        val block = withParts(
            id = "twoSlots",
            parts = listOf(
                BlockPart.Field(field("open", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.STATEMENT, emptyList())),
                BlockPart.Field(field("separator", editable = false)),
                BlockPart.Slot(slotOf(SlotCategory.EXPRESSION, listOf(leaf("y", id = "y")))),
            ),
        )

        val lines = linesOf(block).filterIsInstance<BlockLine.Hole>() +
            linesOf(block).filterIsInstance<BlockLine.Children>()

        assertEquals(listOf(0, 1), lines.map { line ->
            when (line) {
                is BlockLine.Hole -> line.slotIndex
                is BlockLine.Children -> line.slotIndex
                is BlockLine.Tokens -> error("A field line carries no slot index.")
            }
        })
        assertTrue(lines.all { it !is BlockLine.Tokens })
    }
}
