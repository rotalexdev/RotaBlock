package dev.rotalex.rotablock.core.block

/**
 * The identity of one block inside a single projection.
 *
 * An id is never blank, is unique within the tree that produced it and is never persisted, so a stored
 * document can never depend on one.
 */
@JvmInline
value class BlockId(val value: String) {
    init {
        require(value.isNotBlank()) { "A block id must not be blank." }
    }
}
