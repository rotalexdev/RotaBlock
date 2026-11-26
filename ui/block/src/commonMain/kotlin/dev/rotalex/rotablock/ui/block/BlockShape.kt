package dev.rotalex.rotablock.ui.block

import androidx.compose.ui.geometry.Offset

/**
 * One corner of a block's silhouette: where it sits and how far its edge is rounded there.
 *
 * A radius of zero keeps the corner sharp, which is what the inside of a notch needs.
 *
 * @property point the corner itself.
 * @property radius the distance from [point] at which the curve starts.
 */
class BlockCorner(val point: Offset, val radius: Float)

/**
 * The corners of [block]'s silhouette, walked clockwise from the top left.
 *
 * The shape is a rectangle whose left edge is a spine, whose top edge carries the notch cut by the block
 * above, whose right side folds into a mouth when the block owns children, and whose bottom edge carries
 * the tab the block below cuts. Consecutive blocks meet tab to notch, so a stack is one silhouette and
 * never a list of cards.
 */
fun outlineOf(block: PlacedBlock, metrics: BlockMetrics): List<BlockCorner> {
    val x = block.rect.left
    val y = block.rect.top
    val right = block.rect.right
    val bottom = block.rect.bottom
    val radius = metrics.corner
    val notchLeft = x + metrics.notchOffset
    val notchRight = notchLeft + metrics.notchWidth
    val depth = metrics.notchHeight
    val mouthLeft = x + metrics.mouthIndent
    val openBottom = y + block.openHeight
    val mouthFloor = bottom - block.closeHeight
    val corners = mutableListOf<BlockCorner>()

    if (block.boolean) {
        val cut = metrics.hexCut
        val middle = (y + bottom) / 2f
        corners += BlockCorner(Offset(x + cut, y), radius)
        corners += BlockCorner(Offset(right - cut, y), radius)
        corners += BlockCorner(Offset(right, middle), 0f)
        corners += BlockCorner(Offset(right - cut, bottom), radius)
        corners += BlockCorner(Offset(x + cut, bottom), radius)
        corners += BlockCorner(Offset(x, middle), 0f)
        return corners
    }

    fun corner(atX: Float, atY: Float, rounding: Float = radius) {
        corners += BlockCorner(Offset(atX, atY), rounding)
    }

    corner(x, y)
    if (block.notch) {
        corner(notchLeft, y, 0f)
        corner(notchLeft, y + depth)
        corner(notchRight, y + depth)
        corner(notchRight, y, 0f)
    }
    corner(right, y)
    if (block.hasMouth) {
        corner(right, openBottom)
        corner(mouthLeft, openBottom)
        corner(mouthLeft, mouthFloor)
        corner(right, mouthFloor)
    }
    corner(right, bottom)
    if (block.tab) {
        corner(notchRight, bottom, 0f)
        corner(notchRight, bottom + depth)
        corner(notchLeft, bottom + depth)
        corner(notchLeft, bottom, 0f)
    }
    corner(x, bottom)
    return corners
}
