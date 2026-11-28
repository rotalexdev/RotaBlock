package dev.rotalex.rotablock.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockTree
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

    private companion object {
        const val INITIAL_VERSION = 1L
    }
}
