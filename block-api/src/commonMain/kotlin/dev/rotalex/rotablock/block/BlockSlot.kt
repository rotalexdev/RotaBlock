package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.DomNode
import dev.rotalex.rotablock.language.dom.NodeKind
import dev.rotalex.rotablock.language.dom.TextRange

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
