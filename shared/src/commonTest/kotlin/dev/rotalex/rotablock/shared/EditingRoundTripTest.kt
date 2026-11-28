package dev.rotalex.rotablock.shared

import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.ui.block.BlockFocus
import dev.rotalex.rotablock.ui.block.reanchor
import kotlin.test.Test
import kotlin.test.assertEquals

class EditingRoundTripTest {
    private val text = """{"path": "/greeting", "cached": true}"""

    private fun tree(): BlockTree = JsonWorkspace.project(text)

    private fun document(tree: BlockTree): BlockNode = tree.root.slots.flatMap { slot -> slot.children }.single()

    private fun members(tree: BlockTree): List<BlockNode> =
        document(tree).slots.flatMap { slot -> slot.children }

    private fun valueOf(member: BlockNode): BlockNode = member.slots.flatMap { slot -> slot.children }.single()

    @Test
    fun renamingAKeyKeepsTheCaretInItsField() {
        val tree = tree()
        val member = members(tree).first()
        val focus = BlockFocus.Field(member.id, member.fields.indexOfFirst { field -> field.editable })

        val next = JsonWorkspace.apply(BlockEdit.SetField(BlockRef(member.id), focus.index, "route"), tree, text)

        assertEquals("""{"route": "/greeting", "cached": true}""", next)
        assertEquals(focus, reanchor(focus, JsonWorkspace.project(next)))
    }

    @Test
    fun changingAValueKeepsTheCaretInTheValue() {
        val tree = tree()
        val value = valueOf(members(tree).first())
        val focus = BlockFocus.Field(value.id, value.fields.indexOfFirst { field -> field.editable })

        val next = JsonWorkspace.apply(BlockEdit.SetField(BlockRef(value.id), focus.index, "ping"), tree, text)

        assertEquals("""{"path": "ping", "cached": true}""", next)
        assertEquals(focus, reanchor(focus, JsonWorkspace.project(next)))
    }

    @Test
    fun anEditThatNoLongerResolvesLeavesTheTextAlone() {
        val tree = tree()

        val next = JsonWorkspace.apply(BlockEdit.SetField(BlockRef(BlockId("absent")), 0, "x"), tree, text)

        assertEquals(text, next)
    }
}
