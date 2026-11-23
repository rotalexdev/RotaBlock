package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.NodeKind

/**
 * A block shape a host can insert or wrap with: an identity, the kind of block it produces, and the
 * syntax it places around the content it takes.
 *
 * Inserting uses both sides with nothing between them; wrapping splices the block's own text between
 * them. A template with empty sides therefore changes nothing, which is the only value that is legal for
 * a host that has no syntax of its own.
 *
 * @property id the identity a host uses to name this template.
 * @property kind the kind of block the produced text is expected to project as.
 * @property prefix the syntax placed before the content.
 * @property suffix the syntax placed after the content.
 */
class BlockTemplate(
    val id: String,
    val kind: NodeKind,
    val prefix: String,
    val suffix: String,
) {
    init {
        require(id.isNotBlank()) { "A template id must not be blank." }
    }

    /** The text [BlockEdit.InsertTemplate] inserts, which carries no content of its own. */
    val insertionText: String get() = prefix + suffix

    /** The text [BlockEdit.Wrap] produces around [content]. */
    fun wrapped(content: String): String = prefix + content + suffix
}
