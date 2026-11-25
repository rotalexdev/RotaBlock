package dev.rotalex.rotablock.ui.block

import androidx.compose.ui.graphics.Color
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind

/**
 * The colours the block surface paints with, taken from the design reference for the editing surface.
 *
 * Colours are values rather than theme roles because a block is painted as one shape on a Canvas, not
 * assembled from Material surfaces.
 */
class BlockPalette(
    val statement: Color = Color(0xFF2E9D8E),
    val expression: Color = Color(0xFF2F6CA8),
    val declaration: Color = Color(0xFFC2185B),
    val other: Color = Color(0xFF4A4A4A),
    val onBlock: Color = Color(0xFFFFFFFF),
    val chipNumber: Color = Color(0xFFC87F1E),
    val chipString: Color = Color(0xFFFFFFFF),
    val onChipString: Color = Color(0xFFC2185B),
    val onChipValue: Color = Color(0xFFFFFFFF),
    val ghostBorder: Color = Color(0xFF9E9E9E),
    val ghostText: Color = Color(0xFF6E6E6E),
    val cardOutline: Color = Color(0x66FFFFFF),
) {
    /** The card colour of a block with [category]. */
    fun blockColor(category: SlotCategory): Color = when (category) {
        SlotCategory.STATEMENT -> statement
        SlotCategory.EXPRESSION -> expression
        SlotCategory.DECLARATION -> declaration
        else -> other
    }

    /** The chip background of a value of [valueKind], or `null` when the token is painted without one. */
    fun chipColor(valueKind: ValueKind): Color? = when (valueKind) {
        ValueKind.STRING -> chipString
        ValueKind.NUMBER -> chipNumber
        ValueKind.BOOLEAN -> statement
        else -> null
    }

    /** The colour a chip's text is painted with. */
    fun onChipColor(valueKind: ValueKind): Color = when (valueKind) {
        ValueKind.STRING -> onChipString
        else -> onChipValue
    }
}
