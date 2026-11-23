package dev.rotalex.rotablock.core.engine

import dev.rotalex.rotablock.core.language.DocumentEdit

/**
 * The engine's own gate on an edit list: it merges what can be merged, orders what remains and refuses a
 * list that could not be applied.
 *
 * Applying a normalized list to the source is possible by construction, because the spans descend and never
 * overlap, so text outside them is byte-identical and no offset is invalidated by an earlier edit.
 */
internal object TextEdits {
    /**
     * Returns [edits] as one minimal list ordered by descending offset.
     *
     * Empty edits are dropped, edits that target the same offset are merged in their original order, and
     * adjacent deletions collapse into one. A list that overlaps or addresses an offset outside [limit] is
     * a compiler defect and throws instead of producing a half-applied document.
     */
    fun normalize(edits: List<DocumentEdit>, limit: Int): List<DocumentEdit> {
        edits.forEach { edit ->
            if (edit.offset + edit.oldLength > limit) {
                throw IllegalStateException(
                    "An edit over ${edit.offset}..${edit.offset + edit.oldLength} leaves the tree that ends at $limit.",
                )
            }
        }
        val byOffset = mutableMapOf<Int, DocumentEdit>()
        val order = mutableListOf<Int>()
        edits.forEach { edit ->
            if (edit.oldLength == 0 && edit.newText.isEmpty()) return@forEach
            val seen = byOffset[edit.offset]
            if (seen == null) order += edit.offset
            byOffset[edit.offset] = if (seen == null) {
                edit
            } else {
                DocumentEdit(edit.offset, maxOf(seen.oldLength, edit.oldLength), seen.newText + edit.newText)
            }
        }
        val descending = order.sortedDescending().map { byOffset.getValue(it) }
        val merged = mutableListOf<DocumentEdit>()
        descending.forEach { edit ->
            val previous = merged.lastOrNull()
            if (
                previous != null &&
                previous.oldLength > 0 &&
                edit.oldLength > 0 &&
                previous.newText.isEmpty() &&
                edit.newText.isEmpty() &&
                previous.offset == edit.offset + edit.oldLength
            ) {
                merged[merged.lastIndex] = DocumentEdit(edit.offset, previous.oldLength + edit.oldLength, "")
            } else {
                merged += edit
            }
        }
        merged.zipWithNext().forEach { (higher, lower) ->
            if (lower.offset + lower.oldLength > higher.offset) {
                throw IllegalStateException(
                    "An edit over ${lower.offset}..${lower.offset + lower.oldLength} overlaps ${higher.offset}..${higher.offset + higher.oldLength}.",
                )
            }
        }
        return merged
    }
}
