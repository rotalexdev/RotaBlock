package dev.rotalex.rotablock.shared

import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.ui.block.BlockFocus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class EditorStoreDeleteTest {
    private fun document(tree: BlockTree): BlockNode = tree.root.slots.flatMap { slot -> slot.children }.single()

    private fun members(tree: BlockTree): List<BlockNode> =
        document(tree).slots.flatMap { slot -> slot.children }

    private fun BlockNode.keyIndex(): Int = fields.indexOfFirst { field -> field.editable }

    @Test
    fun aMemberTakesItsSeparatorWithIt() {
        val store = EditorStore("""{"a": 1, "b": 2}""")

        store.delete(members(store.snapshot.tree).first().id)

        assertEquals("""{"b": 2}""", store.snapshot.text)
        assertEquals(2L, store.snapshot.version)
    }

    @Test
    fun deletingTheLastMemberFocusesThePreviousOne() {
        val store = EditorStore("""{"a": 1, "b": 2}""")

        store.delete(members(store.snapshot.tree).last().id)

        assertEquals("""{"a": 1}""", store.snapshot.text)
        val survivor = members(store.snapshot.tree).single()
        val focus = assertIs<BlockFocus.Field>(store.focus)
        assertEquals(survivor.id, focus.id)
        assertEquals(survivor.keyIndex(), focus.index)
    }

    @Test
    fun deletingTheOnlyContentOfASingularSlotDeletesTheOwner() {
        val store = EditorStore("""{"a": 1}""")
        val value = members(store.snapshot.tree).single().slots.single().children.single()

        store.delete(value.id)

        assertEquals("{}", store.snapshot.text)
        val container = document(store.snapshot.tree)
        assertEquals(BlockFocus.Block(container.id), store.focus)
    }

    @Test
    fun theRootIsNeverDeleted() {
        val store = EditorStore("""{"a": 1}""")

        store.delete(store.snapshot.tree.root.id)

        assertEquals("""{"a": 1}""", store.snapshot.text)
        assertEquals(1L, store.snapshot.version)
        assertNull(store.focus)
    }

    @Test
    fun deletingFoldsTheOpenDraftFirst() {
        val store = EditorStore("""{"a": 1, "b": 2}""")
        val first = members(store.snapshot.tree).first()
        val second = members(store.snapshot.tree).last()
        store.focusOn(BlockFocus.Field(first.id, first.keyIndex()))
        store.edit(BlockEdit.SetField(BlockRef(first.id), first.keyIndex(), "aa"))

        store.delete(second.id)

        assertEquals("""{"aa": 1}""", store.snapshot.text)
        assertNull(store.draftText)
    }
}
