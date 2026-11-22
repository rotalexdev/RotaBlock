package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.TextRange

/**
 * One inline token of a block: either syntax the reader cannot edit or a value they can.
 *
 * An editable field always carries the [range] it occupies; chrome may carry no backing text at all.
 */
class BlockField(
    val role: String,
    val text: String,
    val editable: Boolean,
    val range: TextRange?,
) {
    init {
        require(role.isNotBlank()) { "A field role must not be blank." }
        require(!editable || range != null) { "An editable field must carry a range." }
    }

    /** A read-only field holding syntax, optionally anchored to the text it was carved from. */
    companion object {
        fun chrome(role: String, text: String, range: TextRange? = null): BlockField =
            BlockField(role = role, text = text, editable = false, range = range)
    }
}
