package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

/**
 * One block of the projection: its identity, the DOM node it projects, and its ordered content.
 *
 * [anchor] is the node this block was projected from and is `null` only for synthetic chrome blocks.
 * [fields] and [slots] are views over [parts] and never a second source of truth.
 */
class BlockNode(
    val id: BlockId,
    val kind: NodeKind,
    val anchor: DomNode?,
    val range: TextRange,
    val parts: List<BlockPart>,
    val label: String? = null,
    val valueKind: ValueKind = ValueKind.UNKNOWN,
) {
    /** The fields of this block, in part order. */
    val fields: List<BlockField> = parts.filterIsInstance<BlockPart.Field>().map { it.field }

    /** The slots of this block, in part order. */
    val slots: List<BlockSlot> = parts.filterIsInstance<BlockPart.Slot>().map { it.slot }
}
