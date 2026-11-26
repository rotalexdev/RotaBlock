package dev.rotalex.rotablock.ui.block

/**
 * How a run of text is drawn: as a plain token on its block, or as a chip, which carries a background.
 */
enum class BlockTextStyle {
    /** Plain block chrome, such as a brace or a separator. */
    TOKEN,

    /** A value or an editable field, drawn on a chip. */
    CHIP,

    /** A ghost inside an empty slot. */
    GHOST,
}

/** Measures the width a text needs when drawn in a style. */
fun interface TextSizer {
    /** The width of [text] drawn in [style], in pixels. */
    fun width(text: String, style: BlockTextStyle): Float
}

/**
 * The geometry of the block surface, derived from one unit.
 *
 * Every measurement is an integer multiple of [grid], which is the property that makes a stack line up
 * to the pixel and a density change a scalar instead of a redesign. The multiples and their provenance
 * are recorded in `docs/block-visual-language.md`.
 *
 * @property grid the unit every other measurement is a multiple of.
 */
class BlockMetrics(val grid: Float = 4f) {
    /** The space between two tokens of the same bar. */
    val tokenGap: Float = 2 * grid

    /** The radius of every corner, including the inside corners of a mouth. */
    val corner: Float = grid

    /** The width of the notch cut into the top of a stacked block and of the tab below it. */
    val notchWidth: Float = 9 * grid

    /** How deep the notch and its tab reach. */
    val notchHeight: Float = 2 * grid

    /** The distance from a block's left edge to the left of its notch. */
    val notchOffset: Float = 3 * grid

    /** The distance a mouth's interior is set back from its owner's left edge. */
    val mouthIndent: Float = notchOffset + corner

    /** The narrowest a block may be drawn. */
    val minWidth: Float = 2 * grid

    /** The height of a bar that carries no content. */
    val minBarHeight: Float = 6 * grid

    /** The space between a bar's edge and its content. */
    val barPadding: Float = 4 * grid

    /** The height of a chip, which is also the height of every token box. */
    val chipHeight: Float = 8 * grid

    /** The horizontal padding a chip adds around its text. */
    val chipPaddingX: Float = 2 * grid

    /** The height of a bar that carries content: a chip plus padding above and below. */
    val barHeight: Float = chipHeight + 2 * (2 * grid)

    /** How far a boolean's pointed end reaches past its bar, on each side. */
    val hexCut: Float = notchHeight

    /** The width of a root block's hat. */
    val hatWidth: Float = 24 * grid

    /** The vertical control height of the hat's curve; its visible height is three quarters of it. */
    val hatControl: Float = 5.5f * grid
}
