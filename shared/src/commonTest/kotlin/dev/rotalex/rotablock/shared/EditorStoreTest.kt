package dev.rotalex.rotablock.shared

import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.ui.block.BlockFocus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EditorStoreTest {
    private val text = """{"path": "/greeting", "cached": true}"""

    private fun members(tree: BlockTree): List<BlockNode> {
        val document = tree.root.slots.flatMap { slot -> slot.children }.single()
        return document.slots.flatMap { slot -> slot.children }
    }

    private fun keyOf(store: EditorStore): BlockFocus.Field {
        val member = members(store.snapshot.tree).first()
        return BlockFocus.Field(member.id, member.fields.indexOfFirst { field -> field.editable })
    }

    private fun keyText(tree: BlockTree): String =
        members(tree).first().fields.first { field -> field.editable }.text

    @Test
    fun aStoreStartsWithTheProjectionOfItsText() {
        val store = EditorStore(text)

        assertEquals(text, store.snapshot.text)
        assertEquals(1L, store.snapshot.version)
        assertEquals("path", keyText(store.snapshot.tree))
        assertNull(store.focus)
    }

    @Test
    fun anEditStaysInTheDraftAndLeavesTheSnapshotAlone() {
        val store = EditorStore(text)
        val key = keyOf(store)

        store.edit(BlockEdit.SetField(BlockRef(key.id), key.index, "route"))

        assertEquals("""{"route": "/greeting", "cached": true}""", store.draftText)
        assertEquals(text, store.snapshot.text)
        assertEquals("path", keyText(store.snapshot.tree))
    }

    @Test
    fun aCommitAdvancesTheVersionAndReprojects() {
        val store = EditorStore(text)
        val key = keyOf(store)
        store.edit(BlockEdit.SetField(BlockRef(key.id), key.index, "route"))

        store.commit()

        assertEquals("""{"route": "/greeting", "cached": true}""", store.snapshot.text)
        assertEquals(2L, store.snapshot.version)
        assertEquals("route", keyText(store.snapshot.tree))
        assertNull(store.draftText)
    }

    @Test
    fun aCommitWithoutAChangeKeepsTheVersion() {
        val store = EditorStore(text)

        store.commit()

        assertEquals(1L, store.snapshot.version)
    }

    @Test
    fun theNextKeystrokeCompilesAgainstTheSnapshotTheFieldWasOpenedOn() {
        val store = EditorStore(text)
        val key = keyOf(store)

        store.edit(BlockEdit.SetField(BlockRef(key.id), key.index, "route"))
        store.edit(BlockEdit.SetField(BlockRef(key.id), key.index, "pathx"))
        store.commit()

        assertEquals("""{"pathx": "/greeting", "cached": true}""", store.snapshot.text)
    }

    @Test
    fun anEditThatNoLongerResolvesLeavesTheSnapshotAlone() {
        val store = EditorStore(text)

        store.edit(BlockEdit.SetField(BlockRef(BlockId("absent")), 0, "x"))
        store.commit()

        assertEquals(text, store.snapshot.text)
        assertEquals(1L, store.snapshot.version)
    }

    @Test
    fun movingTheFocusAwayFoldsTheDraft() {
        val store = EditorStore(text)
        val key = keyOf(store)
        store.focusOn(key)
        store.edit(BlockEdit.SetField(BlockRef(key.id), key.index, "route"))

        store.focusOn(BlockFocus.Block(key.id))

        assertEquals("""{"route": "/greeting", "cached": true}""", store.snapshot.text)
    }

    @Test
    fun movingToAnotherFieldFoldsTheDraftToo() {
        val store = EditorStore(text)
        val key = keyOf(store)
        val value = members(store.snapshot.tree).first().slots.flatMap { slot -> slot.children }.single()
        val valueField = BlockFocus.Field(value.id, value.fields.indexOfFirst { field -> field.editable })
        store.focusOn(key)
        store.edit(BlockEdit.SetField(BlockRef(key.id), key.index, "route"))

        store.focusOn(valueField)

        assertEquals("""{"route": "/greeting", "cached": true}""", store.snapshot.text)
        assertEquals(2L, store.snapshot.version)
    }

    @Test
    fun theFocusSurvivesTheCommitOfItsOwnField() {
        val store = EditorStore(text)
        val key = keyOf(store)
        store.focusOn(key)
        store.edit(BlockEdit.SetField(BlockRef(key.id), key.index, "route"))

        store.commit()

        assertEquals(key, store.focus)
        assertNotNull(store.focus)
    }
}
