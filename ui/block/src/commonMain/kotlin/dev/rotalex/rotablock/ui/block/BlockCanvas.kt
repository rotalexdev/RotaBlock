package dev.rotalex.rotablock.ui.block

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.rotalex.rotablock.core.block.BlockTree

/**
 * The projected document painted as a stack of blocks.
 *
 * Every construct is one silhouette: a bar of tokens, a mouth holding its children inside a C, and a bar
 * that closes the C. Consecutive blocks meet at a notch instead of at whitespace, so a document is one
 * connected shape a reader follows instead of a list of cards.
 *
 * Everything is painted on one [Canvas] from a [BlockLayout], so the shapes are paths rather than a tree of
 * layout boxes, and one document costs one draw pass. The surface is a pure function of [tree] and [focus]:
 * it writes nothing and emits nothing. A click reports the focus it resolved through [onFocus], and the keys
 * that need no text input walk it: `Tab` and `Shift+Tab` through the holes, the arrows through the blocks,
 * `Escape` out of both.
 *
 * @property tree the projection to paint.
 * @property modifier the layout modifier of the scrolling surface.
 * @property palette the three tones and the focus colours.
 * @property focus where the user is, or `null` when nothing is focused.
 * @property onFocus receives every focus the surface resolves, including `null` when it is cleared.
 */
@Composable
fun BlockCanvas(
    tree: BlockTree,
    modifier: Modifier = Modifier,
    palette: BlockPalette = BlockPalette(),
    focus: BlockFocus? = null,
    onFocus: (BlockFocus?) -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val typography = MaterialTheme.typography.bodyMedium
    val metrics = remember(density) { BlockMetrics(grid = with(density) { 4.dp.toPx() }) }
    val textStyle = remember(metrics, typography, density) {
        typography.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = with(density) { (metrics.grid * 3).toSp() },
        )
    }
    val sizer = remember(measurer, textStyle) {
        TextSizer { text, _ -> measurer.measure(text, textStyle).size.width.toFloat() }
    }
    val layout = remember(tree, sizer, metrics) { layoutBlocks(tree, sizer, metrics) }
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Tab -> {
                        onFocus(nextHole(layout, focus, forward = !event.isShiftPressed))
                        true
                    }
                    Key.DirectionDown -> {
                        onFocus(nextBlock(layout, focus))
                        true
                    }
                    Key.DirectionUp -> {
                        onFocus(nextBlock(layout, focus, forward = false))
                        true
                    }
                    Key.Escape -> {
                        onFocus(null)
                        true
                    }
                    else -> false
                }
            }
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        val width = with(density) { layout.width.toDp().coerceAtLeast(1.dp) }
        val height = with(density) { layout.height.toDp().coerceAtLeast(1.dp) }
        Canvas(
            modifier = Modifier
                .size(width = width, height = height)
                .pointerInput(layout) {
                    detectTapGestures { point ->
                        focusRequester.requestFocus()
                        onFocus(focusAt(layout, point))
                    }
                },
        ) {
            layout.blocks.forEach { block -> paintBlock(block, focus, palette, measurer, textStyle, metrics) }
            layout.ghost?.let { ghost ->
                paintGhost(ghost, focus?.let { it == BlockFocus.Slot(ghost.blockId, ghost.slotIndex) } == true, palette, measurer, textStyle, metrics)
            }
        }
    }
}

private fun DrawScope.paintBlock(
    block: PlacedBlock,
    focus: BlockFocus?,
    palette: BlockPalette,
    measurer: TextMeasurer,
    textStyle: TextStyle,
    metrics: BlockMetrics,
) {
    val fill = palette.fill(block.block.category, block.block.valueKind)
    val silhouette = silhouetteOf(block, metrics)
    if (focus == BlockFocus.Block(block.block.id)) {
        drawPath(silhouette, palette.focusRing, style = Stroke(width = metrics.grid * 1.5f))
    }
    drawPath(silhouette, fill)
    drawPath(silhouette, palette.stroke(fill), style = Stroke(width = metrics.grid / 4f))
    val pointedChip = !block.boolean
    block.tokens.forEach { token -> paintToken(token, focusedField(focus, token), fill, pointedChip, palette, measurer, textStyle, metrics) }
    block.closingTokens.forEach { token ->
        paintToken(token, focusedField(focus, token), fill, pointedChip, palette, measurer, textStyle, metrics)
    }
    block.ghost?.let { ghost ->
        val focused = focus == BlockFocus.Slot(ghost.blockId, ghost.slotIndex)
        paintGhost(ghost, focused, palette, measurer, textStyle, metrics)
    }
    block.children.forEach { child -> paintBlock(child, focus, palette, measurer, textStyle, metrics) }
}

private fun focusedField(focus: BlockFocus?, token: PlacedToken): Boolean =
    focus is BlockFocus.Field && focus.id == token.blockId && focus.index == token.fieldIndex

private fun silhouetteOf(block: PlacedBlock, metrics: BlockMetrics): Path {
    val corners = outlineOf(block, metrics)
    val path = Path()
    val count = corners.size
    fun before(index: Int): Offset {
        val corner = corners[index]
        if (corner.radius == 0f) return corner.point
        return corner.point.stepTowards(corners[(index + count - 1) % count].point, corner.radius)
    }
    fun after(index: Int): Offset {
        val corner = corners[index]
        if (corner.radius == 0f) return corner.point
        return corner.point.stepTowards(corners[(index + 1) % count].point, corner.radius)
    }
    val start = before(0)
    path.moveTo(start.x, start.y)
    corners.forEachIndexed { index, corner ->
        val isHatTop = block.hat && index == 1
        if (isHatTop) {
            val from = after(0)
            val to = before(1)
            val raised = corners[0].point.y - metrics.hatControl
            val span = (to.x - from.x) * 0.25f
            path.cubicTo(from.x + span, raised, to.x - span, raised, to.x, to.y)
        } else {
            val entry = before(index)
            path.lineTo(entry.x, entry.y)
            if (corner.radius > 0f) {
                val exit = after(index)
                path.quadraticTo(corner.point.x, corner.point.y, exit.x, exit.y)
            }
        }
    }
    path.close()
    return path
}

private fun hexagonOf(rect: Rect, metrics: BlockMetrics): Path {
    val cut = metrics.hexCut
    val middle = rect.center.y
    val path = Path()
    path.moveTo(rect.left + cut, rect.top)
    path.lineTo(rect.right - cut, rect.top)
    path.lineTo(rect.right, middle)
    path.lineTo(rect.right - cut, rect.bottom)
    path.lineTo(rect.left + cut, rect.bottom)
    path.lineTo(rect.left, middle)
    path.close()
    return path
}

private fun Offset.stepTowards(target: Offset, distance: Float): Offset {
    val dx = target.x - x
    val dy = target.y - y
    val length = kotlin.math.sqrt(dx * dx + dy * dy)
    if (length == 0f) return this
    return Offset(x + dx / length * distance, y + dy / length * distance)
}

private fun DrawScope.paintToken(
    token: PlacedToken,
    focused: Boolean,
    fill: Color,
    pointedChip: Boolean,
    palette: BlockPalette,
    measurer: TextMeasurer,
    textStyle: TextStyle,
    metrics: BlockMetrics,
) {
    val radius = CornerRadius(metrics.corner, metrics.corner)
    if (token.chip) {
        if (pointedChip && token.lead > 0f) {
            drawPath(hexagonOf(token.rect, metrics), palette.chip(fill))
        } else {
            drawRoundRect(palette.chip(fill), topLeft = token.rect.topLeft, size = token.rect.size, cornerRadius = radius)
        }
    }
    if (focused) {
        drawRoundRect(
            color = palette.focusRing,
            topLeft = Offset(token.rect.left, token.rect.top + metrics.grid / 2f),
            size = Size(metrics.grid / 2f, token.rect.height - metrics.grid),
            cornerRadius = CornerRadius(metrics.grid / 4f, metrics.grid / 4f),
        )
    }
    val inset = if (token.chip) metrics.chipPaddingX else 0f
    val text = measurer.measure(token.text, textStyle)
    drawText(
        textLayoutResult = text,
        color = palette.onBlock,
        topLeft = Offset(
            x = token.rect.left + inset + token.lead,
            y = token.rect.top + (token.rect.height - text.size.height) / 2f,
        ),
    )
}

private fun DrawScope.paintGhost(
    ghost: PlacedGhost,
    focused: Boolean,
    palette: BlockPalette,
    measurer: TextMeasurer,
    textStyle: TextStyle,
    metrics: BlockMetrics,
) {
    val stroke = Stroke(width = metrics.grid / 2f)
    if (focused) {
        if (ghost.boolean) {
            drawPath(hexagonOf(ghost.rect, metrics), palette.focusGhost)
        } else {
            drawRoundRect(
                color = palette.focusGhost,
                topLeft = ghost.rect.topLeft,
                size = ghost.rect.size,
                cornerRadius = CornerRadius(metrics.corner, metrics.corner),
            )
        }
    }
    if (ghost.boolean) {
        drawPath(hexagonOf(ghost.rect, metrics), palette.ghostBorder, style = stroke)
    } else {
        drawRoundRect(
            color = palette.ghostBorder,
            topLeft = ghost.rect.topLeft,
            size = ghost.rect.size,
            cornerRadius = CornerRadius(metrics.corner, metrics.corner),
            style = stroke,
        )
    }
    val text = measurer.measure(ghost.label, textStyle)
    drawText(
        textLayoutResult = text,
        color = palette.ghostText,
        topLeft = Offset(
            x = ghost.rect.left + metrics.chipPaddingX,
            y = ghost.rect.top + (ghost.rect.height - text.size.height) / 2f,
        ),
    )
}
