package dev.rotalex.rotablock.core.block

/**
 * A position inside a slot: the owner block, which slot of it, and where among that slot's children.
 *
 * [index] equal to the number of children means "append here".
 */
class SlotRef(val owner: BlockRef, val slotIndex: Int, val index: Int) {
    init {
        require(slotIndex >= 0) { "A slot index must be non-negative, but was $slotIndex." }
        require(index >= 0) { "A child index must be non-negative, but was $index." }
    }
}
