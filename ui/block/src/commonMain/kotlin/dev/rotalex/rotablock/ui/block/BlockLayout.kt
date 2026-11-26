package dev.rotalex.rotablock.ui.block

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import dev.rotalex.rotablock.core.block.BlockField
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind

/**
 * One token of a block, placed: its text, the box it occupies and the style it is painted with.
 *
 * @property text the token's text.
 * @property rect the box the token occupies, always one chip tall.
 * @property style the style it is measured and painted with.
 * @property chip whether the token carries a chip background.
 * @property valueKind the shape of the value the token carries, which picks the colour of its block.
 * @property lead how far the text is set back from the box's left edge, which a pointed value needs.
 */
class PlacedToken(
    val text: String,
    val rect: Rect,
    val style: BlockTextStyle,
    val chip: Boolean,
    val valueKind: ValueKind,
    val lead: Float = 0f,
)

/**
 * The ghost of an empty slot, placed.
 *
 * @property label the word shown inside it.
 * @property rect the box it occupies.
 * @property boolean whether the slot accepts a boolean, which draws the ghost as a hexagon.
 */
class PlacedGhost(val label: String, val rect: Rect, val boolean: Boolean = false)

/**
 * One block of the surface, placed: its silhouette, the tokens of its opening and closing bars, its
 * mouth and the notch, tab and hat that join it to what surrounds it.
 *
 * @property block the block this shape paints.
 * @property rect the body, which excludes any tab below it and any hat above it.
 * @property openHeight the height of the opening bar.
 * @property closeHeight the height of the closing bar, zero when the block has no mouth.
 * @property tokens the placed tokens of the opening bar.
 * @property closingTokens the placed tokens of the closing bar.
 * @property children the placed blocks in the mouth, in document order.
 * @property ghost the placed ghost, when the block shows an empty slot.
 * @property notch whether the top edge carries a notch, which the block above it fills.
 * @property tab whether the bottom edge carries a tab, which the block below it cuts.
 * @property hat whether a hat rises above the top edge, which marks where the surface starts.
 */
class PlacedBlock(
    val block: BlockNode,
    val rect: Rect,
    val openHeight: Float,
    val closeHeight: Float,
    val tokens: List<PlacedToken>,
    val closingTokens: List<PlacedToken>,
    val children: List<PlacedBlock>,
    val ghost: PlacedGhost?,
    val notch: Boolean,
    val tab: Boolean,
    val hat: Boolean,
) {
    /** Whether the block opens a mouth for its children. */
    val hasMouth: Boolean get() = children.isNotEmpty() || closeHeight > 0f

    /** Whether the block is a boolean value, which ends in a point instead of a corner. */
    val boolean: Boolean get() = block.valueKind == ValueKind.BOOLEAN && !hasMouth
}

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
 * Lays [tree] out as a stack of blocks.
 *
 * A block is a bar of tokens, an optional mouth holding its children inside a C, and an optional bar that
 * closes the C. A member whose value is a container lends its key to that container's opening bar, so a
 * nested object reads as `"key": {` above its own contents instead of a bar repeating the container's
 * brace. A member whose value is a single literal keeps that literal on its own bar. Consecutive blocks
 * meet at a notch and never at whitespace, so a stack is one shape and not a list.
 *
 * The result is a pure function of the tree, the [sizer] and the [metrics]: the geometry is verified
 * without Compose running, and the painter draws only the shape it is given.
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
            PlacedGhost(text, Rect(Offset.Zero, Size(width, metrics.chipHeight)), boolean = false)
        }
        return BlockLayout(emptyList(), ghost?.rect?.right ?: 0f, ghost?.rect?.bottom ?: 0f, ghost)
    }
    val forms = top.map { block -> formOf(block, sizer, metrics) }
    val measured = forms.map { form -> measure(form, metrics) }
    var y = metrics.hatControl * 0.75f
    var right = 0f
    val placed = measured.mapIndexed { index, node ->
        val last = index == measured.lastIndex
        val boolean = node.lead > 0f
        val block = place(node, 0f, y, metrics, notch = index > 0 && !boolean, tab = !last && !boolean, hat = index == 0 && !boolean)
        y = block.rect.bottom
        right = maxOf(right, block.rect.right)
        block
    }
    val height = placed.last().rect.bottom
    return BlockLayout(placed, right, height, null)
}

private class TokenSpec(
    val text: String,
    val style: BlockTextStyle,
    val chip: Boolean,
    val valueKind: ValueKind,
    val lead: Float,
    val width: Float,
)

private class GhostSpec(val label: String, val width: Float, val boolean: Boolean)

private class Form(
    val block: BlockNode,
    val open: List<TokenSpec>,
    val closing: List<TokenSpec>,
    val mouth: List<Form>,
    val ghost: GhostSpec?,
) {
    val hasMouth: Boolean get() = mouth.isNotEmpty() || closing.isNotEmpty()
}

private fun displayText(field: BlockField): String =
    if (field.editable) field.text else field.text.trim().replace(WHITESPACE, " ").trim()

private val WHITESPACE = Regex("\\s+")

private fun tokenOf(
    field: BlockField,
    owner: ValueKind,
    pointed: Boolean,
    sizer: TextSizer,
    metrics: BlockMetrics,
): TokenSpec? {
    val text = displayText(field)
    if (text.isEmpty()) return null
    val style = if (field.editable) BlockTextStyle.CHIP else BlockTextStyle.TOKEN
    val lead = if (pointed && field.editable && owner == ValueKind.BOOLEAN) metrics.hexCut else 0f
    val width = sizer.width(text, style) + if (field.editable) metrics.chipPaddingX * 2 else 0f
    return TokenSpec(text, style, field.editable, owner, lead, width + lead * 2)
}

private fun tokensOf(
    fields: List<BlockField>,
    owner: ValueKind,
    sizer: TextSizer,
    metrics: BlockMetrics,
    pointed: Boolean = false,
): List<TokenSpec> = fields.mapNotNull { field -> tokenOf(field, owner, pointed, sizer, metrics) }

private fun formOf(block: BlockNode, sizer: TextSizer, metrics: BlockMetrics): Form {
    val open = mutableListOf<TokenSpec>()
    val closing = mutableListOf<TokenSpec>()
    val mouth = mutableListOf<Form>()
    var ghost: GhostSpec? = null
    var seenSlot = false
    linesOf(block).forEach { line ->
        when (line) {
            is BlockLine.Tokens -> {
                val specs = tokensOf(line.fields, block.valueKind, sizer, metrics)
                if (seenSlot) closing += specs else open += specs
            }
            is BlockLine.Hole -> {
                seenSlot = true
                val width = sizer.width(line.ghost.label, BlockTextStyle.GHOST) + metrics.chipPaddingX * 2 +
                    if (line.ghost.boolean) metrics.hexCut * 2 else 0f
                ghost = GhostSpec(line.ghost.label, width, line.ghost.boolean)
            }
            is BlockLine.Children -> {
                seenSlot = true
                val singular = block.slots.getOrNull(line.slotIndex)?.multiple == false
                val inline = line.blocks.singleOrNull()?.takeIf { child -> singular && child.slots.isEmpty() }
                if (inline != null) {
                    open += tokensOf(
                        fields = inline.fields,
                        owner = inline.valueKind,
                        sizer = sizer,
                        metrics = metrics,
                        pointed = inline.valueKind == ValueKind.BOOLEAN,
                    )
                } else {
                    mouth += line.blocks.map { child -> formOf(child, sizer, metrics) }
                }
            }
        }
    }
    return reduce(Form(block, open, closing, mouth, ghost))
}

private fun reduce(start: Form): Form {
    var form = start
    while (true) {
        if (form.open.isEmpty() && form.closing.isEmpty() && form.ghost == null && form.mouth.size == 1) {
            form = form.mouth.single()
            continue
        }
        val child = form.mouth.singleOrNull()
        if (child != null && form.open.isNotEmpty() && form.closing.isEmpty() && form.ghost == null &&
            form.block.category == SlotCategory.DECLARATION && (child.hasMouth || child.ghost != null)
        ) {
            form = Form(child.block, form.open + child.open, child.closing, child.mouth, child.ghost)
            continue
        }
        return form
    }
}

private class Measured(
    val form: Form,
    val openHeight: Float,
    val closeHeight: Float,
    val mouthHeight: Float,
    val lead: Float,
    val width: Float,
    val body: Float,
)

private fun rowWidth(tokens: List<TokenSpec>, metrics: BlockMetrics): Float {
    if (tokens.isEmpty()) return 0f
    val content = tokens.fold(0f) { acc, token -> acc + token.width } +
        metrics.tokenGap * (tokens.size - 1)
    return metrics.barPadding * 2 + content
}

private fun measure(form: Form, metrics: BlockMetrics): Measured {
    val openHeight = if (form.open.isEmpty() && form.ghost == null) metrics.minBarHeight else metrics.barHeight
    val closeHeight = if (!form.hasMouth) 0f else metrics.barHeight
    val kids = form.mouth.map { child -> measure(child, metrics) }
    val mouthHeight = if (kids.isEmpty()) {
        0f
    } else {
        kids.fold(0f) { acc, child -> acc + child.body }
    }
    val ghostWidth = form.ghost?.let { spec ->
        spec.width + if (form.open.isEmpty()) 0f else metrics.tokenGap
    } ?: 0f
    val mouthWidth = if (kids.isEmpty()) 0f else metrics.mouthIndent + kids.maxOf { it.width }
    val boolean = form.block.valueKind == ValueKind.BOOLEAN && !form.hasMouth
    val lead = if (boolean) metrics.hexCut else 0f
    val width = maxOf(
        metrics.minWidth,
        lead * 2 + rowWidth(form.open, metrics) + ghostWidth,
        rowWidth(form.closing, metrics),
        mouthWidth,
    )
    val body = openHeight + mouthHeight + closeHeight
    return Measured(form, openHeight, closeHeight, mouthHeight, lead, width, body)
}

private fun place(
    node: Measured,
    x: Float,
    y: Float,
    metrics: BlockMetrics,
    notch: Boolean,
    tab: Boolean,
    hat: Boolean,
): PlacedBlock {
    val barTop = y + (node.openHeight - metrics.chipHeight) / 2f
    val tokens = placeRow(node.form.open, x + node.lead, barTop, metrics)
    val ghost = node.form.ghost?.let { spec ->
        val origin = x + node.lead
        val start = if (tokens.isEmpty()) origin + metrics.barPadding else tokens.last().rect.right + metrics.tokenGap
        PlacedGhost(
            label = spec.label,
            rect = Rect(start, barTop, start + spec.width, barTop + metrics.chipHeight),
            boolean = spec.boolean,
        )
    }
    val children = mutableListOf<PlacedBlock>()
    var childY = y + node.openHeight
    node.form.mouth.forEachIndexed { index, child ->
        val last = index == node.form.mouth.lastIndex
        val placed = place(
            measure(child, metrics),
            x + metrics.mouthIndent,
            childY,
            metrics,
            notch = index > 0,
            tab = !last,
            hat = false,
        )
        children += placed
        childY = placed.rect.bottom
    }
    val closeTop = y + node.openHeight + node.mouthHeight
    val closingTokens = if (node.closeHeight == 0f) {
        emptyList()
    } else {
        placeRow(node.form.closing, x, closeTop + (node.closeHeight - metrics.chipHeight) / 2f, metrics)
    }
    return PlacedBlock(
        block = node.form.block,
        rect = Rect(x, y, x + node.width, y + node.body),
        openHeight = node.openHeight,
        closeHeight = node.closeHeight,
        tokens = tokens,
        closingTokens = closingTokens,
        children = children,
        ghost = ghost,
        notch = notch,
        tab = tab,
        hat = hat,
    )
}

private fun placeRow(
    tokens: List<TokenSpec>,
    x: Float,
    top: Float,
    metrics: BlockMetrics,
): List<PlacedToken> {
    var cursor = x + metrics.barPadding
    return tokens.map { token ->
        val rect = Rect(cursor, top, cursor + token.width, top + metrics.chipHeight)
        cursor = rect.right + metrics.tokenGap
        PlacedToken(token.text, rect, token.style, token.chip, token.valueKind, token.lead)
    }
}
