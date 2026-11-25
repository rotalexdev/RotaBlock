package dev.rotalex.rotablock.ui.block

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.rotalex.rotablock.core.block.BlockTree

/**
 * The projected document painted as blocks: each construct is one card, nested constructs are drawn
 * inside their owner, and a block that owns children carries the tab that continues its spine.
 *
 * Everything is painted on one [Canvas] from a [BlockLayout], so the shapes are paths rather than a tree of
 * layout boxes, and one document costs one draw pass. The surface is a pure function of [tree]: it reads
 * nothing else, writes nothing and emits nothing.
 */
@Composable
fun BlockCanvas(
    tree: BlockTree,
    modifier: Modifier = Modifier,
    palette: BlockPalette = BlockPalette(),
) {
    val measurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    val density = LocalDensity.current
    val metrics = remember(density) {
        with(density) {
            BlockMetrics(
                textHeight = 20.dp.toPx(),
                paddingX = 10.dp.toPx(),
                paddingY = 5.dp.toPx(),
                rowGap = 5.dp.toPx(),
                tokenGap = 6.dp.toPx(),
                indent = 16.dp.toPx(),
                chipPaddingX = 6.dp.toPx(),
                corner = 7.dp.toPx(),
                tabWidth = 16.dp.toPx(),
                tabHeight = 7.dp.toPx(),
                minBlockWidth = 60.dp.toPx(),
            )
        }
    }
    val sizer = remember(measurer, textStyle) {
        TextSizer { text, _ -> measurer.measure(text, textStyle).size.width.toFloat() }
    }
    val layout = remember(tree, sizer, metrics) { layoutBlocks(tree, sizer, metrics) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        val width = with(density) { layout.width.toDp().coerceAtLeast(1.dp) }
        val height = with(density) { layout.height.toDp().coerceAtLeast(1.dp) }
        Canvas(modifier = Modifier.size(width = width, height = height)) {
            layout.blocks.forEach { block -> paintBlock(block, palette, measurer, textStyle, metrics) }
            layout.ghost?.let { ghost -> paintGhost(ghost, palette, measurer, textStyle, metrics) }
        }
    }
}

private fun DrawScope.paintBlock(
    block: PlacedBlock,
    palette: BlockPalette,
    measurer: TextMeasurer,
    textStyle: TextStyle,
    metrics: BlockMetrics,
    depth: Int = 0,
) {
    val card = palette.blockColor(block.block.category)
    val radius = CornerRadius(metrics.corner, metrics.corner)
    drawRoundRect(card, topLeft = block.rect.topLeft, size = block.rect.size, cornerRadius = radius)
    if (depth > 0) {
        drawRoundRect(
            color = palette.cardOutline,
            topLeft = block.rect.topLeft,
            size = block.rect.size,
            cornerRadius = radius,
            style = Stroke(width = 1f),
        )
    }
    block.tab?.let { tab ->
        drawRoundRect(
            color = card,
            topLeft = tab.topLeft,
            size = tab.size,
            cornerRadius = CornerRadius(0f, metrics.corner),
        )
    }
    block.tokens.forEach { token -> paintToken(token, palette, measurer, textStyle, metrics) }
    block.ghost?.let { ghost -> paintGhost(ghost, palette, measurer, textStyle, metrics) }
    block.children.forEach { child -> paintBlock(child, palette, measurer, textStyle, metrics, depth + 1) }
}

private fun DrawScope.paintToken(
    token: PlacedToken,
    palette: BlockPalette,
    measurer: TextMeasurer,
    textStyle: TextStyle,
    metrics: BlockMetrics,
) {
    val chip = if (token.chip) palette.chipColor(token.valueKind) else null
    if (chip != null) {
        drawRoundRect(
            color = chip,
            topLeft = token.rect.topLeft,
            size = token.rect.size,
            cornerRadius = CornerRadius(metrics.corner / 2f, metrics.corner / 2f),
        )
    }
    val color = if (chip != null) palette.onChipColor(token.valueKind) else palette.onBlock
    val inset = if (token.chip) metrics.chipPaddingX else 0f
    val text = measurer.measure(token.text, textStyle)
    drawText(
        textLayoutResult = text,
        color = color,
        topLeft = Offset(
            x = token.rect.left + inset,
            y = token.rect.top + (token.rect.height - text.size.height) / 2f,
        ),
    )
}

private fun DrawScope.paintGhost(
    ghost: PlacedGhost,
    palette: BlockPalette,
    measurer: TextMeasurer,
    textStyle: TextStyle,
    metrics: BlockMetrics,
) {
    drawRoundRect(
        color = palette.ghostBorder,
        topLeft = ghost.rect.topLeft,
        size = ghost.rect.size,
        cornerRadius = CornerRadius(metrics.corner, metrics.corner),
        style = Stroke(width = 1f),
    )
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
