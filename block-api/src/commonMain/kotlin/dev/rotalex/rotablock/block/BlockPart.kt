package dev.rotalex.rotablock.block

/**
 * One element of a block's ordered content.
 *
 * The order of parts is both render order and serialize order, so it is never sorted or regrouped.
 */
sealed interface BlockPart {
    /** A field part wrapping one [BlockField]. */
    data class Field(val field: BlockField) : BlockPart

    /** A slot part wrapping one [BlockSlot]. */
    data class Slot(val slot: BlockSlot) : BlockPart
}
