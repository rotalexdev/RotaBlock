package dev.rotalex.rotablock.language.dom

/**
 * The only write primitive of the model: replace [oldLength] characters at [offset] with [newText].
 *
 * Consumers apply a set of edits in descending offset order, so an earlier edit never shifts a later one.
 */
data class DocumentEdit(val offset: Int, val oldLength: Int, val newText: String) {
    init {
        require(offset >= 0) { "An edit offset must be non-negative, but was $offset." }
        require(oldLength >= 0) { "An edit length must be non-negative, but was $oldLength." }
    }
}
