package dev.rotalex.rotablock.block

/**
 * A reference to a block, as an edit carries it.
 *
 * Identity is [BlockId]; a reference names a block and is only valid against the tree it was taken from.
 */
@JvmInline
value class BlockRef(val id: BlockId)
