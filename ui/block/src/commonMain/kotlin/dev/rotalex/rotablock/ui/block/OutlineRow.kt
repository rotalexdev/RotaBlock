package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockPart

/**
 * One row of the outline: the block it names, the size of its subtree and the rows nested below it.
 *
 * @property block the block the row names.
 * @property descendantCount the number of blocks in the subtree, this block included.
 * @property children the rows of the blocks directly below it, in document order.
 */
class OutlineRow(val block: BlockNode, val descendantCount: Int, val children: List<OutlineRow>)

/** The rows an outline shows for [root]: one per block directly below it, each carrying its subtree. */
fun outlineRows(root: BlockNode): List<OutlineRow> =
    root.slots.flatMap { slot -> slot.children.map(::outlineRow) }

/** The row of [block], built from the subtree it owns. */
fun outlineRow(block: BlockNode): OutlineRow {
    val children = block.slots.flatMap { slot -> slot.children.map(::outlineRow) }
    return OutlineRow(block, 1 + children.sumOf { it.descendantCount }, children)
}

/**
 * The label a row shows for [block]: the block's own label, else its first editable text, else its kind.
 *
 * A label longer than [MAX_LABEL_LENGTH] characters is cut and marked with an ellipsis, so one block never
 * widens the outline by itself.
 */
fun displayLabel(block: BlockNode): String {
    val own = block.label
    val text = block.parts.filterIsInstance<BlockPart.Field>()
        .firstOrNull { it.field.editable && it.field.text.isNotBlank() }
        ?.field?.text
    val label = own ?: text ?: block.kind.id.substringAfterLast('.')
    return if (label.length <= MAX_LABEL_LENGTH) label else label.take(MAX_LABEL_LENGTH) + "…"
}

private const val MAX_LABEL_LENGTH = 24
