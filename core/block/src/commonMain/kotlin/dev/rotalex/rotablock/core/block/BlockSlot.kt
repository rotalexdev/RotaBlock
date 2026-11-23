package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

/**
 * One child position of a block: the grammar it accepts, its children and the shape it is rendered with.
 *
 * A slot carries the category it accepts and the ranges of the tree it was projected from, so a placed child
 * can be checked with [SlotCategory.accepts] and an edit can address a child without the source text.
 */
class BlockSlot(
    val category: SlotCategory,
    val children: List<BlockNode>,
    val multiple: Boolean,
    val range: TextRange?,
    val valueKind: ValueKind,
)
