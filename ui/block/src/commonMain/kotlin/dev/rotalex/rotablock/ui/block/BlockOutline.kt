package dev.rotalex.rotablock.ui.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.rotalex.rotablock.core.block.SlotCategory

/**
 * The document as an outline: one row per block of [rows], each showing a category glyph, its label and
 * how many blocks it owns, with its nested rows below it.
 *
 * The outline is a view of the same projection the surface renders, so the two never disagree.
 */
@Composable
fun BlockOutline(rows: List<OutlineRow>, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        rows.forEach { row -> OutlineRowView(row, depth = 0) }
    }
}

@Composable
private fun OutlineRowView(row: OutlineRow, depth: Int) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (8 + depth * 16).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(glyphColor(row.block.category, colors), RoundedCornerShape(2.dp)),
        )
        Text(
            text = displayLabel(row.block),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = blockCount(row.descendantCount),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
    }
    row.children.forEach { child -> OutlineRowView(child, depth + 1) }
}

private fun blockCount(count: Int): String = if (count == 1) "1 block" else "$count blocks"

private fun glyphColor(category: SlotCategory, colors: ColorScheme): Color = when (category) {
    SlotCategory.STATEMENT -> colors.primary
    SlotCategory.EXPRESSION -> colors.tertiary
    SlotCategory.DECLARATION -> colors.secondary
    SlotCategory.TYPE -> colors.error
    SlotCategory.NAME, SlotCategory.PARAMETER, SlotCategory.ARGUMENT -> colors.onSurfaceVariant
    SlotCategory.MODIFIER -> colors.outline
    SlotCategory.OPAQUE -> colors.outlineVariant
}
