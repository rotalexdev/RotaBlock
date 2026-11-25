package dev.rotalex.rotablock.ui.block

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.block.ValueKind

/**
 * One token of a block, placed: its text, the rectangle it occupies and whether it is painted as a chip.
 *
 * @property text the token's text.
 * @property rect the rectangle the token occupies inside its block.
 * @property style the style it is measured and painted with.
 * @property chip whether the token carries a chip background.
 * @property valueKind the shape of the value the token carries, which picks its chip colour.
 */
class PlacedToken(
    val text: String,
    val rect: Rect,
    val style: BlockTextStyle,
    val chip: Boolean,
    val valueKind: ValueKind,
)

/**
 * The ghost of an empty slot, placed.
 *
 * @property label the name shown inside it.
 * @property rect the rectangle it occupies.
 */
class PlacedGhost(val label: String, val rect: Rect)

/**
 * One block of the surface, placed: the card it was given, its tokens, its nested blocks, its ghost and
 * the tab that continues its spine.
 *
 * @property block the block this shape paints.
 * @property rect the card body, which excludes [tab].
 * @property tokens the placed tokens, in part order.
 * @property children the placed nested blocks.
 * @property ghost the placed ghost, when the block has exactly one empty slot and nothing else to show.
 * @property tab the continuation below the body, `null` for a block with no children.
 */
class PlacedBlock(
    val block: BlockNode,
    val rect: Rect,
    val tokens: List<PlacedToken>,
    val children: List<PlacedBlock>,
    val ghost: PlacedGhost?,
    val tab: Rect?,
)

/**
 * The whole surface, placed: every top-level block and the bounds the painter has to cover.
 *
 * @property blocks the top-level blocks, in document order.
 * @property width the widest x the painter reaches.
 * @property height the lowest y the painter reaches.
 * @property ghost the ghost of an empty document, `null` once the document holds a block.
 */
class BlockLayout(
    val blocks: List<PlacedBlock>,
    val width: Float,
    val height: Float,
    val ghost: PlacedGhost?,
)

/**
 * Lays [tree] out as blocks: each block is a card sized by its measured tokens, its children are indented
 * below it, and a block that owns children carries a tab that continues its spine.
 *
 * The result is a pure function of the tree, the [sizer] and the [metrics], so the geometry is verified
 * without Compose running and the painter only draws rectangles it did not decide.
 */
fun layoutBlocks(
    tree: BlockTree,
    sizer: TextSizer,
    metrics: BlockMetrics = BlockMetrics(),
): BlockLayout {
    val top = tree.root.slots.flatMap { slot -> slot.children }
    if (top.isEmpty()) {
        val emptySlot = tree.root.slots.firstOrNull { it.children.isEmpty() }
        val label = emptySlot?.let { slot -> ghostOf(slot)?.label }
        val ghost = label?.let { text ->
            val width = sizer.width(text, BlockTextStyle.GHOST) + metrics.chipPaddingX * 2
            PlacedGhost(text, Rect(Offset.Zero, Size(width, metrics.textHeight)))
        }
        return BlockLayout(emptyList(), ghost?.rect?.right ?: 0f, ghost?.rect?.bottom ?: 0f, ghost)
    }
    val measured = top.map { block -> measure(block, sizer, metrics) }
    var y = 0f
    var right = 0f
    val placed = measured.map { node ->
        val block = place(node, 0f, y, metrics)
        y = block.rect.bottom + (block.tab?.height ?: 0f) + metrics.rowGap
        right = maxOf(right, block.rect.right)
        block
    }
    return BlockLayout(placed, right, (y - metrics.rowGap).coerceAtLeast(0f), null)
}

private class MeasuredToken(
    val text: String,
    val style: BlockTextStyle,
    val chip: Boolean,
    val valueKind: ValueKind,
    val width: Float,
)

private sealed interface MeasuredLine {
    class Tokens(val tokens: List<MeasuredToken>) : MeasuredLine
    class Hole(val label: String, val width: Float) : MeasuredLine
    class Kids(val children: List<Measured>) : MeasuredLine
}

private class Measured(
    val block: BlockNode,
    val lines: List<MeasuredLine>,
    val body: Float,
    val width: Float,
    val height: Float,
)

private fun MutableList<MeasuredLine>.appendTokens(tokens: List<MeasuredToken>) {
    if (tokens.isEmpty()) return
    val last = lastOrNull()
    if (last is MeasuredLine.Tokens) {
        set(size - 1, MeasuredLine.Tokens(last.tokens + tokens))
    } else {
        add(MeasuredLine.Tokens(tokens))
    }
}

private fun measure(block: BlockNode, sizer: TextSizer, metrics: BlockMetrics): Measured {
    val lines = mutableListOf<MeasuredLine>()
    linesOf(block).forEach { line ->
        when (line) {
            is BlockLine.Tokens -> {
                val tokens = line.fields
                    .filter { field -> field.text.isNotBlank() }
                    .map { field ->
                        val style = if (field.editable) BlockTextStyle.CHIP else BlockTextStyle.TOKEN
                        val textWidth = sizer.width(field.text, style)
                        val width = if (field.editable) textWidth + metrics.chipPaddingX * 2 else textWidth
                        MeasuredToken(field.text, style, field.editable, block.valueKind, width)
                    }
                lines.appendTokens(tokens)
            }
            is BlockLine.Hole -> {
                val width = sizer.width(line.ghost.label, BlockTextStyle.GHOST) + metrics.chipPaddingX * 2
                lines += MeasuredLine.Hole(line.ghost.label, width)
            }
            is BlockLine.Children -> {
                val inline = line.blocks.singleOrNull()?.takeIf { child -> child.slots.isEmpty() }
                if (inline != null) {
                    val tokens = inline.fields
                        .filter { field -> field.text.isNotBlank() }
                        .map { field ->
                            val width = sizer.width(field.text, BlockTextStyle.CHIP) + metrics.chipPaddingX * 2
                            MeasuredToken(field.text, BlockTextStyle.CHIP, true, inline.valueKind, width)
                        }
                    lines.appendTokens(tokens)
                } else {
                    lines += MeasuredLine.Kids(line.blocks.map { child -> measure(child, sizer, metrics) })
                }
            }
        }
    }
    var contentWidth = 0f
    var contentHeight = 0f
    lines.forEachIndexed { index, line ->
        if (index > 0) contentHeight += metrics.rowGap
        when (line) {
            is MeasuredLine.Tokens -> {
                val rowWidth = line.tokens.fold(0f) { acc, token -> acc + token.width } +
                    metrics.tokenGap * (line.tokens.size - 1).coerceAtLeast(0)
                contentWidth = maxOf(contentWidth, rowWidth)
                contentHeight += metrics.textHeight
            }
            is MeasuredLine.Hole -> {
                contentWidth = maxOf(contentWidth, line.width)
                contentHeight += metrics.textHeight
            }
            is MeasuredLine.Kids -> {
                val rowWidth = line.children.maxOf { child -> child.width } + metrics.indent
                contentWidth = maxOf(contentWidth, rowWidth)
                line.children.forEachIndexed { childIndex, child ->
                    if (childIndex > 0) contentHeight += metrics.rowGap
                    contentHeight += child.height
                }
            }
        }
    }
    val body = metrics.paddingY * 2 + contentHeight
    val width = maxOf(metrics.minBlockWidth, metrics.paddingX * 2 + contentWidth)
    val hasKids = lines.any { it is MeasuredLine.Kids }
    return Measured(block, lines, body, width, body + if (hasKids) metrics.tabHeight else 0f)
}

private fun place(node: Measured, x: Float, y: Float, metrics: BlockMetrics): PlacedBlock {
    val rect = Rect(x, y, x + node.width, y + node.body)
    val tokens = mutableListOf<PlacedToken>()
    val children = mutableListOf<PlacedBlock>()
    var ghost: PlacedGhost? = null
    var cursorY = y + metrics.paddingY
    node.lines.forEachIndexed { index, line ->
        if (index > 0) cursorY += metrics.rowGap
        when (line) {
            is MeasuredLine.Tokens -> {
                var cursorX = x + metrics.paddingX
                line.tokens.forEachIndexed { tokenIndex, token ->
                    if (tokenIndex > 0) cursorX += metrics.tokenGap
                    val tokenRect = Rect(cursorX, cursorY, cursorX + token.width, cursorY + metrics.textHeight)
                    tokens += PlacedToken(token.text, tokenRect, token.style, token.chip, token.valueKind)
                    cursorX = tokenRect.right
                }
                cursorY += metrics.textHeight
            }
            is MeasuredLine.Hole -> {
                val start = x + metrics.paddingX
                ghost = PlacedGhost(line.label, Rect(start, cursorY, start + line.width, cursorY + metrics.textHeight))
                cursorY += metrics.textHeight
            }
            is MeasuredLine.Kids -> {
                var childY = cursorY
                line.children.forEachIndexed { childIndex, child ->
                    if (childIndex > 0) childY += metrics.rowGap
                    val placed = place(child, x + metrics.indent, childY, metrics)
                    children += placed
                    childY = placed.rect.bottom + (placed.tab?.height ?: 0f)
                }
                cursorY = childY
            }
        }
    }
    val tab = if (children.isEmpty()) {
        null
    } else {
        Rect(x, rect.bottom, x + metrics.tabWidth, rect.bottom + metrics.tabHeight)
    }
    return PlacedBlock(node.block, rect, tokens, children, ghost, tab)
}
