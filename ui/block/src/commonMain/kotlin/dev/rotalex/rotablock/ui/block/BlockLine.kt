package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.BlockField
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockPart

/**
 * One rendered line of a block: fields read together, or a slot that is either a hole or holds blocks.
 *
 * The lines keep part order, so rendering them in order reproduces the block's own content.
 */
sealed interface BlockLine {
    /** Consecutive fields rendered on one line, in part order. */
    class Tokens(val fields: List<BlockField>) : BlockLine

    /** An empty slot, rendered as the ghost that names what it accepts. */
    class Hole(val slotIndex: Int, val ghost: SlotGhost) : BlockLine

    /** A slot holding blocks, rendered below the containing line. */
    class Children(val slotIndex: Int, val blocks: List<BlockNode>) : BlockLine
}

/** The rendered lines of [block], in part order. */
fun linesOf(block: BlockNode): List<BlockLine> {
    val lines = mutableListOf<BlockLine>()
    val pending = mutableListOf<BlockField>()
    fun flush() {
        if (pending.isNotEmpty()) {
            lines += BlockLine.Tokens(pending.toList())
            pending.clear()
        }
    }
    var slotIndex = 0
    block.parts.forEach { part ->
        when (part) {
            is BlockPart.Field -> pending += part.field
            is BlockPart.Slot -> {
                flush()
                val ghost = ghostOf(part.slot)
                if (ghost == null) {
                    lines += BlockLine.Children(slotIndex, part.slot.children)
                } else {
                    lines += BlockLine.Hole(slotIndex, ghost)
                }
                slotIndex += 1
            }
        }
    }
    flush()
    return lines
}
