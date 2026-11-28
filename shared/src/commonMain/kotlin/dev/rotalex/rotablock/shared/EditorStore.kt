package dev.rotalex.rotablock.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.BlockSlot
import dev.rotalex.rotablock.core.block.BlockTemplate
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.block.SlotRef
import dev.rotalex.rotablock.ui.block.BlockFocus
import dev.rotalex.rotablock.ui.block.reanchor

/**
 * One consistent view of the document: the text, the version it was parsed at, and the projection of that
 * exact pair.
 *
 * The three travel together because every edit is expressed against the projection and resolved against the
 * text behind it. Keeping them in one value makes it impossible to compile an intent against a tree that
 * belongs to another text.
 *
 * @property text the document source, the only stored representation of the document.
 * @property version the number of commits that produced [text], which the parser records in [tree].
 * @property tree the projection of [text] at [version].
 */
data class Snapshot(
    val text: String,
    val version: Long,
    val tree: BlockTree,
)

/**
 * The editing state of one document: its committed [snapshot], the draft an open field is typing, and where
 * the user is.
 *
 * An insert advances the snapshot on its own: unlike a keystroke it is not a change to a field that is
 * already there, so there is no draft to fold it into.
 *
 * A field that is being typed into does not advance the snapshot. Every keystroke is compiled against the
 * snapshot the field was opened on and replaces the field's whole text in a draft, so a value that is
 * temporarily unparsable cannot move its block, change its identity or drop the caret. [commit] folds the
 * draft into a new snapshot; moving the focus anywhere folds it too, so no keystroke is ever left behind.
 *
 * The two projections are injectable so the same state logic can be exercised without a parser or a screen.
 *
 * @property text the document the store opens on.
 * @property project projects a text at a version into the block tree it describes.
 * @property apply resolves one edit against the text and the tree it was projected from.
 */
class EditorStore(
    text: String,
    private val project: (text: String, version: Long) -> BlockTree = JsonWorkspace::project,
    private val apply: (edit: BlockEdit, tree: BlockTree, text: String) -> String = JsonWorkspace::apply,
) {
    /**
     * The committed document. Reading it inside a composition subscribes that composition to every commit.
     */
    var snapshot by mutableStateOf(Snapshot(text, INITIAL_VERSION, project(text, INITIAL_VERSION)))
        private set

    /**
     * Where the user is, kept anchored to the committed snapshot. Reading it inside a composition
     * subscribes that composition to every focus move.
     */
    var focus by mutableStateOf<BlockFocus?>(null)
        private set

    /**
     * The text an open field has typed, or `null` when nothing is being typed.
     */
    var draftText: String? = null
        private set

    /**
     * Requests [edit] without advancing the snapshot, replacing whatever the open field had typed.
     *
     * An edit that no longer resolves leaves the draft equal to the committed text, which [commit] then
     * treats as no change.
     */
    fun edit(edit: BlockEdit) {
        draftText = apply(edit, snapshot.tree, snapshot.text)
    }

    /**
     * Folds the draft into a new snapshot and re-anchors the focus to it.
     *
     * A draft that matches the committed text is dropped without a version bump, so opening and closing a
     * field costs no repaint.
     */
    fun commit() {
        val pending = draftText ?: return
        draftText = null
        if (pending == snapshot.text) return
        val version = snapshot.version + 1
        val tree = project(pending, version)
        snapshot = Snapshot(pending, version, tree)
        focus = focus?.let { current -> reanchor(current, tree) }
    }

    /**
     * Moves the focus to [next], folding whatever the field that is being left had typed.
     */
    fun focusOn(next: BlockFocus?) {
        if (next != focus) commit()
        focus = next
    }

    /**
     * Inserts the next sibling of [block] and focuses what it created, as `Enter` on a block does.
     *
     * Nothing happens when the block has no position in the tree, when its slot holds a single child, or
     * when that slot offers no template: a position that cannot take another child has nothing to insert.
     */
    fun insertAfter(block: BlockId) {
        commit()
        val position = positionOf(block) ?: return
        if (!position.slot.multiple) return
        val template = position.slot.templates.firstOrNull() ?: return
        insert(SlotRef(BlockRef(position.owner), position.slotIndex, position.index + 1), template)
    }

    /**
     * Appends the template of the slot at [slotIndex] of [owner] and focuses what it created, as `Enter` on
     * an empty slot does.
     *
     * Nothing happens when the slot does not exist or offers no template.
     */
    fun insertInto(owner: BlockId, slotIndex: Int) {
        commit()
        val slot = snapshot.tree.block(owner)?.slots?.getOrNull(slotIndex) ?: return
        val template = slot.templates.firstOrNull() ?: return
        insert(SlotRef(BlockRef(owner), slotIndex, slot.children.size), template)
    }

    /**
     * Deletes [block] and the syntax that existed only to hold it, then focuses the nearest survivor.
     *
     * A block that is the only content of a slot holding a single child is not deletable on its own, because
     * removing it would leave behind the syntax that needed it. The delete then applies to the nearest
     * ancestor whose slot holds several children; the root has no such ancestor and is never deleted.
     */
    fun delete(block: BlockId) {
        commit()
        val target = deletable(block) ?: return
        val position = positionOf(target) ?: return
        val tree = advance(BlockEdit.Delete(BlockRef(target))) ?: return
        focusOn(survivorOf(tree, position))
    }

    private fun deletable(block: BlockId): BlockId? {
        var current = block
        while (true) {
            val position = positionOf(current) ?: return null
            if (position.slot.multiple) return current
            current = position.owner
        }
    }

    private fun survivorOf(tree: BlockTree, position: Position): BlockFocus? {
        val siblings = tree.block(position.owner)?.slots?.getOrNull(position.slotIndex)?.children
            ?: return null
        val neighbour = siblings.getOrNull(position.index - 1) ?: siblings.getOrNull(position.index)
        if (neighbour != null) return focusInside(neighbour)
        return tree.block(position.owner)?.let { owner -> BlockFocus.Block(owner.id) }
    }

    private fun insert(reference: SlotRef, template: BlockTemplate) {
        val tree = advance(BlockEdit.InsertTemplate(reference, template)) ?: return
        val created = tree.block(reference.owner.id)?.slots?.getOrNull(reference.slotIndex)
            ?.children?.getOrNull(reference.index)
        focusOn(created?.let(::focusInside))
    }

    private fun advance(edit: BlockEdit): BlockTree? {
        val next = apply(edit, snapshot.tree, snapshot.text)
        if (next == snapshot.text) return null
        val version = snapshot.version + 1
        val tree = project(next, version)
        snapshot = Snapshot(next, version, tree)
        return tree
    }

    private fun focusInside(block: BlockNode): BlockFocus =
        block.fields.indexOfFirst { field -> field.editable }
            .takeIf { index -> index >= 0 }
            ?.let { index -> BlockFocus.Field(block.id, index) }
            ?: BlockFocus.Block(block.id)

    private fun positionOf(block: BlockId): Position? {
        var found: Position? = null
        fun visit(node: BlockNode) {
            node.slots.forEachIndexed { slotIndex, slot ->
                slot.children.forEachIndexed { index, child ->
                    if (child.id == block) found = Position(node.id, slotIndex, index, slot)
                    visit(child)
                }
            }
        }
        visit(snapshot.tree.root)
        return found
    }

    private class Position(val owner: BlockId, val slotIndex: Int, val index: Int, val slot: BlockSlot)

    private companion object {
        const val INITIAL_VERSION = 1L
    }
}
