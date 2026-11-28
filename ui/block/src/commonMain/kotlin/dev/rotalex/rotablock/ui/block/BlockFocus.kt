package dev.rotalex.rotablock.ui.block

import androidx.compose.ui.geometry.Offset
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockTree

/**
 * Where the user is in a projected document.
 *
 * Focus is one of three things, and it never holds a block: it holds the [BlockId] that names one, plus the
 * index of the field or slot inside it, so a new projection of the same document resolves to the same place
 * without carrying a stale object across an edit.
 */
sealed interface BlockFocus {
    /** The block this focus names. */
    val id: BlockId

    /** A whole block: selectable, draggable and deletable as one unit. */
    data class Block(override val id: BlockId) : BlockFocus

    /** One editable field of a block, named by its position within the block's fields. */
    data class Field(override val id: BlockId, val index: Int) : BlockFocus

    /** One empty slot of a block, named by its position within the block's slots. */
    data class Slot(override val id: BlockId, val index: Int) : BlockFocus
}

/**
 * The focus a click at [point] lands on, or `null` when the point hits nothing.
 *
 * The deepest block under the point wins, so a click inside a mouth addresses the child and not its owner,
 * and inside that block an editable token addresses its field and a ghost addresses its slot. Chrome is not
 * a focus target: a click on the syntax of a block focuses the block, because §9.2 makes a field focusable
 * only when it is editable.
 */
fun focusAt(layout: BlockLayout, point: Offset): BlockFocus? {
    val block = deepestAt(layout.blocks, point)
    if (block != null) {
        block.tokens.firstOrNull { token -> token.chip && token.rect.contains(point) }?.let { token ->
            return BlockFocus.Field(token.blockId, token.fieldIndex)
        }
        block.closingTokens.firstOrNull { token -> token.chip && token.rect.contains(point) }?.let { token ->
            return BlockFocus.Field(token.blockId, token.fieldIndex)
        }
        block.ghost?.takeIf { ghost -> ghost.rect.contains(point) }?.let { ghost ->
            return BlockFocus.Slot(ghost.blockId, ghost.slotIndex)
        }
        return BlockFocus.Block(block.block.id)
    }
    return layout.ghost?.takeIf { ghost -> ghost.rect.contains(point) }?.let { ghost ->
        BlockFocus.Slot(ghost.blockId, ghost.slotIndex)
    }
}

/**
 * What `Enter` asks for at a focus, as the interaction model defines it for every language.
 *
 * A field commits and stays open, a block asks for a sibling after it, and an empty slot asks for a child.
 * A host turns each intent into the edit its language can express, so the key means the same thing
 * everywhere and a language cannot override it.
 */
sealed interface EnterIntent {
    /** Fold the open field into the document and keep it open. */
    data object Commit : EnterIntent

    /** Insert a sibling after the block named by [block]. */
    data class InsertAfter(val block: BlockId) : EnterIntent

    /** Insert a child into the empty slot at [slot] of [owner]. */
    data class InsertInto(val owner: BlockId, val slot: Int) : EnterIntent

    /** Nothing is focused, so the key belongs to whatever holds the surface. */
    data object None : EnterIntent
}

/** The intent `Enter` carries at [focus]. */
fun enterIntent(focus: BlockFocus?): EnterIntent = when (focus) {
    null -> EnterIntent.None
    is BlockFocus.Field -> EnterIntent.Commit
    is BlockFocus.Block -> EnterIntent.InsertAfter(focus.id)
    is BlockFocus.Slot -> EnterIntent.InsertInto(focus.id, focus.index)
}

/** The empty slots of the surface, in the order a reader walks them. */
fun holesOf(layout: BlockLayout): List<BlockFocus.Slot> {
    val holes = mutableListOf<BlockFocus.Slot>()
    fun visit(block: PlacedBlock) {
        block.ghost?.let { ghost -> holes += BlockFocus.Slot(ghost.blockId, ghost.slotIndex) }
        block.children.forEach(::visit)
    }
    layout.blocks.forEach(::visit)
    layout.ghost?.let { ghost -> holes += BlockFocus.Slot(ghost.blockId, ghost.slotIndex) }
    return holes
}

/** The blocks of the surface, in the order a reader walks them. */
fun blocksOf(layout: BlockLayout): List<BlockFocus.Block> {
    val blocks = mutableListOf<BlockFocus.Block>()
    fun visit(block: PlacedBlock) {
        blocks += BlockFocus.Block(block.block.id)
        block.children.forEach(::visit)
    }
    layout.blocks.forEach(::visit)
    return blocks
}

/**
 * The hole `Tab` reaches from [focus], or `null` when the surface offers none.
 *
 * A hole steps to the next or previous hole and wraps around. Any other focus starts the walk at its own
 * block, so leaving a field lands on the first hole that block still offers instead of jumping to the top of
 * the document.
 */
fun nextHole(layout: BlockLayout, focus: BlockFocus?, forward: Boolean = true): BlockFocus.Slot? {
    val holes = ownedHoles(layout)
    if (holes.isEmpty()) return null
    if (focus is BlockFocus.Slot) {
        val index = holes.indexOfFirst { it.first == focus }
        if (index >= 0) {
            val step = if (forward) (index + 1) % holes.size else (index - 1 + holes.size) % holes.size
            return holes[step].first
        }
    }
    val owner = focus?.let { current -> blocksOf(layout).indexOfFirst { it.id == current.id } } ?: 0
    val anchor = if (owner < 0) 0 else owner
    val candidate = if (forward) {
        holes.firstOrNull { it.second >= anchor } ?: holes.first()
    } else {
        holes.lastOrNull { it.second <= anchor } ?: holes.last()
    }
    return candidate.first
}

/** The block an arrow key reaches from [focus], or `null` when the surface holds none. */
fun nextBlock(layout: BlockLayout, focus: BlockFocus?, forward: Boolean = true): BlockFocus.Block? {
    val blocks = blocksOf(layout)
    if (blocks.isEmpty()) return null
    val current = focus?.let { focused -> blocks.indexOfFirst { it.id == focused.id } } ?: -1
    if (current < 0) return if (forward) blocks.first() else blocks.last()
    val step = if (forward) (current + 1) % blocks.size else (current - 1 + blocks.size) % blocks.size
    return blocks[step]
}

/**
 * [focus] resolved against [tree], or `null` when neither it nor an ancestor survives.
 *
 * A block id is the parse path of its node, so matching the path is matching the id. When the block is gone
 * the nearest surviving ancestor takes the focus as a whole block, and an index the block no longer offers, or
 * one that names chrome instead of an editable field, falls back to the block itself, so a projection never
 * leaves the focus pointing at nothing.
 */
fun reanchor(focus: BlockFocus, tree: BlockTree): BlockFocus? {
    val block = tree.block(focus.id) ?: return ancestorOf(focus.id, tree)?.let { id -> BlockFocus.Block(id) }
    return when (focus) {
        is BlockFocus.Block -> BlockFocus.Block(block.id)
        is BlockFocus.Field ->
            if (block.fields.getOrNull(focus.index)?.editable == true) {
                BlockFocus.Field(block.id, focus.index)
            } else {
                BlockFocus.Block(block.id)
            }
        is BlockFocus.Slot ->
            if (focus.index in block.slots.indices) BlockFocus.Slot(block.id, focus.index) else BlockFocus.Block(block.id)
    }
}

private fun ownedHoles(layout: BlockLayout): List<Pair<BlockFocus.Slot, Int>> {
    val holes = mutableListOf<Pair<BlockFocus.Slot, Int>>()
    var owner = 0
    fun visit(block: PlacedBlock) {
        block.ghost?.let { ghost -> holes += BlockFocus.Slot(ghost.blockId, ghost.slotIndex) to owner }
        owner += 1
        block.children.forEach(::visit)
    }
    layout.blocks.forEach(::visit)
    layout.ghost?.let { ghost -> holes += BlockFocus.Slot(ghost.blockId, ghost.slotIndex) to 0 }
    return holes
}

private fun deepestAt(blocks: List<PlacedBlock>, point: Offset): PlacedBlock? {
    for (block in blocks) {
        if (!block.rect.contains(point)) continue
        return deepestAt(block.children, point) ?: block
    }
    return null
}

private fun ancestorOf(id: BlockId, tree: BlockTree): BlockId? {
    var path = id.value
    while (true) {
        val cut = path.lastIndexOf('/')
        if (cut <= 0) return null
        path = path.substring(0, cut)
        tree.block(BlockId(path))?.let { block -> return block.id }
    }
}

/**
 * The token [focus] names, or `null` when no bar paints it.
 *
 * The token is searched for in every bar, not only in the bar of the block that owns the field, because
 * hoisting and inlining paint a borrowed field on the bar of the block that borrows it.
 */
internal fun focusedToken(layout: BlockLayout, focus: BlockFocus.Field): PlacedToken? {
    fun search(block: PlacedBlock): PlacedToken? {
        val owned = block.tokens.firstOrNull { it.names(focus) } ?: block.closingTokens.firstOrNull { it.names(focus) }
        if (owned != null) return owned
        block.children.forEach { child -> search(child)?.let { return it } }
        return null
    }
    layout.blocks.forEach { block -> search(block)?.let { return it } }
    return null
}

/** Whether this token paints [focus]. */
internal fun PlacedToken.names(focus: BlockFocus.Field): Boolean =
    blockId == focus.id && fieldIndex == focus.index
