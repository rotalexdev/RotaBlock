package dev.rotalex.rotablock.block

/**
 * One intent against a projection.
 *
 * The set is closed. Every variant is a pure value addressed by references, so it carries no range and
 * no tree: compilation against the tree it was derived from is the projection engine's work, and a
 * reference that no longer resolves produces an empty edit list there rather than an exception here.
 */
sealed interface BlockEdit {
    /**
     * Replace the text of one editable field.
     *
     * @property target the block that owns the field.
     * @property fieldIndex the position of the field in the block's field list.
     * @property text the new text of the field.
     */
    data class SetField(val target: BlockRef, val fieldIndex: Int, val text: String) : BlockEdit {
        init {
            require(fieldIndex >= 0) { "A field index must be non-negative, but was $fieldIndex." }
        }
    }

    /**
     * Replace the content at a slot position with text that is reparsed afterwards.
     *
     * @property slot the position whose content is replaced.
     * @property text the text that replaces it.
     */
    data class ReplaceWithText(val slot: SlotRef, val text: String) : BlockEdit

    /**
     * Remove a block together with the syntax that existed only to hold it.
     *
     * @property block the block to remove.
     */
    data class Delete(val block: BlockRef) : BlockEdit

    /**
     * Insert a template at a slot position.
     *
     * @property slot the position the template is inserted at.
     * @property template the template whose insertion text is used.
     */
    data class InsertTemplate(val slot: SlotRef, val template: BlockTemplate) : BlockEdit

    /**
     * Wrap a block in a control template.
     *
     * @property block the block that becomes the content of the template.
     * @property template the template wrapped around it.
     */
    data class Wrap(val block: BlockRef, val template: BlockTemplate) : BlockEdit

    /**
     * Move a block to another slot position.
     *
     * @property block the block that moves.
     * @property target the position it moves to, where the append position is legal.
     */
    data class Move(val block: BlockRef, val target: SlotRef) : BlockEdit
}
