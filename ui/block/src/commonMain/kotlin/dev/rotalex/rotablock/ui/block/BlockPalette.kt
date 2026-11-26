package dev.rotalex.rotablock.ui.block

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind

/**
 * The three tones a block is painted with: one fill per block family, and a stroke and a chip tint derived
 * from it.
 *
 * A block carries one hue because a reader recognises a family before reading it; depth is shown by the
 * mouth, never by darkening the fill. The derivation rule is recorded in `docs/block-visual-language.md`.
 *
 * @property onBlock the colour of a block's own text.
 * @property ghostBorder the dashed border of an empty slot.
 * @property ghostText the colour of the words that name what an empty slot accepts.
 */
class BlockPalette(
    val declaration: Color = Color(0xFFC2185B),
    val objectValue: Color = Color(0xFF2F6CA8),
    val arrayValue: Color = Color(0xFF2E9D8E),
    val booleanValue: Color = Color(0xFFC87F1E),
    val other: Color = Color(0xFF4A4A4A),
    val onBlock: Color = Color(0xFFFFFFFF),
    val ghostBorder: Color = Color(0xFF9E9E9E),
    val ghostText: Color = Color(0xFF6E6E6E),
) {
    /** The fill of a block with [category] holding a value of [valueKind]. */
    fun fill(category: SlotCategory, valueKind: ValueKind): Color = when (valueKind) {
        ValueKind.OBJECT -> objectValue
        ValueKind.ARRAY -> arrayValue
        ValueKind.BOOLEAN -> booleanValue
        else -> when (category) {
            SlotCategory.DECLARATION -> declaration
            SlotCategory.EXPRESSION -> objectValue
            SlotCategory.STATEMENT -> arrayValue
            else -> other
        }
    }

    /** The stroke of a block filled with [fill]. */
    fun stroke(fill: Color): Color = lerp(fill, Color.Black, 0.25f)

    /** The tint a chip is filled with on a block filled with [fill]. */
    fun chip(fill: Color): Color = lerp(fill, Color.White, 0.18f)
}
