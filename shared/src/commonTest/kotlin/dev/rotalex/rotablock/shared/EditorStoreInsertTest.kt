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

class EditorStoreInsertTest {
    private fun document(tree: BlockTree): BlockNode = tree.root.slots.flatMap { slot -> slot.children }.single()

    private fun members(tree: BlockTree): List<BlockNode> =
        document(tree).slots.flatMap { slot -> slot.children }

    private fun BlockNode.keyIndex(): Int = fields.indexOfFirst { field -> field.editable }

    @Test
    fun aMemberInsertsItsSiblingAfter() {
        val store = EditorStore("""{"a": 1}""")
        val first = members(store.snapshot.tree).single()

        store.insertAfter(first.id)

        assertEquals("""{"a": 1, "key": null}""", store.snapshot.text)
        assertEquals(2L, store.snapshot.version)
        assertEquals(2, members(store.snapshot.tree).size)
    }

    @Test
    fun theCreatedMemberTakesTheFocusOnItsKey() {
        val store = EditorStore("""{"a": 1}""")
        store.insertAfter(members(store.snapshot.tree).single().id)

        val created = members(store.snapshot.tree).last()
        val focus = assertIs<BlockFocus.Field>(store.focus)
        assertEquals(created.id, focus.id)
        assertEquals(created.keyIndex(), focus.index)
        assertEquals("key", created.fields[focus.index].text)
    }

    @Test
    fun anEmptySlotAppendsTheTemplate() {
        val store = EditorStore("{}")

        store.insertInto(document(store.snapshot.tree).id, 0)

        assertEquals("""{"key": null}""", store.snapshot.text)
        assertEquals(2L, store.snapshot.version)
    }

    @Test
    fun aSlotThatCannotHoldSeveralChildrenInsertsNothing() {
        val store = EditorStore("""{"a": 1}""")
        val value = members(store.snapshot.tree).single().slots.single().children.single()

        store.insertAfter(value.id)

        assertEquals("""{"a": 1}""", store.snapshot.text)
        assertEquals(1L, store.snapshot.version)
        assertNull(store.focus)
    }

    @Test
    fun insertingFoldsTheOpenDraftFirst() {
        val store = EditorStore("""{"a": 1, "b": 2}""")
        val first = members(store.snapshot.tree).first()
        val second = members(store.snapshot.tree).last()
        store.focusOn(BlockFocus.Field(first.id, first.keyIndex()))
        store.edit(BlockEdit.SetField(BlockRef(first.id), first.keyIndex(), "aa"))

        store.insertAfter(second.id)

        assertEquals("""{"aa": 1, "b": 2, "key": null}""", store.snapshot.text)
        assertNull(store.draftText)
    }
}
