package dev.rotalex.rotablock.block

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BlockEditTest {
    private val target = BlockRef(BlockId("b1"))
    private val position = SlotRef(target, slotIndex = 1, index = 2)
    private val template = BlockTemplate("if", dev.rotalex.rotablock.language.dom.NodeKind("IfStatement"), "if (", ") {}")

    @Test
    fun setFieldKeepsTargetFieldIndexAndText() {
        val edit = BlockEdit.SetField(target, fieldIndex = 2, text = "42")
        assertEquals(target, edit.target)
        assertEquals(2, edit.fieldIndex)
        assertEquals("42", edit.text)
    }

    @Test
    fun setFieldRejectsANegativeFieldIndex() {
        assertFailsWith<IllegalArgumentException> { BlockEdit.SetField(target, -1, "42") }
    }

    @Test
    fun replaceWithTextKeepsPositionAndText() {
        val edit = BlockEdit.ReplaceWithText(position, "a > 0")
        assertEquals(position, edit.slot)
        assertEquals("a > 0", edit.text)
    }

    @Test
    fun deleteKeepsTheBlockReference() {
        assertEquals(target, BlockEdit.Delete(target).block)
    }

    @Test
    fun insertTemplateKeepsPositionAndTemplate() {
        val edit = BlockEdit.InsertTemplate(position, template)
        assertEquals(position, edit.slot)
        assertEquals(template, edit.template)
    }

    @Test
    fun wrapKeepsBlockAndTemplate() {
        val edit = BlockEdit.Wrap(target, template)
        assertEquals(target, edit.block)
        assertEquals(template, edit.template)
    }

    @Test
    fun moveKeepsBlockAndTargetPosition() {
        val edit = BlockEdit.Move(target, position)
        assertEquals(target, edit.block)
        assertEquals(position, edit.target)
    }

    @Test
    fun theVocabularyIsAClosedSetOfSixIntents() {
        val edits: List<BlockEdit> = listOf(
            BlockEdit.SetField(target, 0, "1"),
            BlockEdit.ReplaceWithText(position, "1"),
            BlockEdit.Delete(target),
            BlockEdit.InsertTemplate(position, template),
            BlockEdit.Wrap(target, template),
            BlockEdit.Move(target, position),
        )
        assertEquals(6, edits.size)
        assertEquals(1, edits.filterIsInstance<BlockEdit.Move>().size)
        assertTrue(edits[2] is BlockEdit.Delete)
    }

    @Test
    fun variantsWithTheSameContentAreEqual() {
        assertEquals(BlockEdit.Delete(target), BlockEdit.Delete(BlockRef(BlockId("b1"))))
        assertEquals(
            BlockEdit.SetField(target, 0, "1"),
            BlockEdit.SetField(BlockRef(BlockId("b1")), 0, "1"),
        )
    }
}
