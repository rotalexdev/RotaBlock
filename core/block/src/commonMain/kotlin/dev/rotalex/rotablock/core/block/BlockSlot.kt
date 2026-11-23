package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

/**
 * One child position of a block: the grammar it accepts, its children and the shape it is rendered with.
 *
 * A slot does not police its children on construction, because a block carries no category of its own; the
 * engine applies [SlotCategory.accepts] when it places a candidate.
 */
class BlockSlot(
    val category: SlotCategory,
    val children: List<BlockNode>,
    val multiple: Boolean,
    val range: TextRange?,
    val valueKind: ValueKind,
)
