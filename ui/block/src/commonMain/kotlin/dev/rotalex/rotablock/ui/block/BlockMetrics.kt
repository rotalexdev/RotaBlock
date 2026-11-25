package dev.rotalex.rotablock.ui.block

/** The style one run of text is measured and painted with. */
enum class BlockTextStyle {
    /** A token of the block's own text, painted on the block. */
    TOKEN,

    /** A value token painted as a chip. */
    CHIP,

    /** The label of a ghost. */
    GHOST,
}

/** Measures the width one run of text occupies, so the layout stays a pure function of the tree. */
fun interface TextSizer {
    /** The width of [text] in [style], in pixels. */
    fun width(text: String, style: BlockTextStyle): Float
}

/**
 * The fixed geometry of the block surface, in pixels.
 *
 * Geometry is separate from painting so the layout is computed and tested without Compose running, and the
 * painter only consumes the rectangles it is given.
 */
class BlockMetrics(
    val textHeight: Float = 20f,
    val paddingX: Float = 10f,
    val paddingY: Float = 5f,
    val rowGap: Float = 5f,
    val tokenGap: Float = 6f,
    val indent: Float = 16f,
    val chipPaddingX: Float = 6f,
    val corner: Float = 7f,
    val tabWidth: Float = 16f,
    val tabHeight: Float = 7f,
    val minBlockWidth: Float = 60f,
)
