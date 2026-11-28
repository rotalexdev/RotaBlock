package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

/**
 * One child position of a block: the grammar it accepts, its children, the shape it is rendered with and the
 * templates a host may insert into it.
 *
 * A slot carries the category it accepts and the ranges of the tree it was projected from, so a placed child
 * can be checked with [SlotCategory.accepts] and an edit can address a child without the source text. Its
 * templates are what lets a host create a child without knowing the language: a slot that offers none is one
 * where nothing may be inserted.
 */
class BlockSlot(
    val category: SlotCategory,
    val children: List<BlockNode>,
    val multiple: Boolean,
    val range: TextRange?,
    val valueKind: ValueKind,
    val templates: List<BlockTemplate> = emptyList(),
)
