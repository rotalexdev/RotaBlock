package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.TextRange

/** The way a language separates the elements of a list. */
sealed interface SeparatorStyle {
    /** Elements are adjacent with nothing between them. */
    data object None : SeparatorStyle

    /** Elements are separated by a line break. */
    data object LineBreak : SeparatorStyle

    /** Elements are separated by a comma. */
    data object Comma : SeparatorStyle

    /** Elements are separated by language-specific text. */
    data class Custom(val leading: String, val trailing: String) : SeparatorStyle
}

/** The way a language indents nested statements. */
sealed interface IndentStyle {
    /** The language never indents. */
    data object None : IndentStyle

    /** Each nesting level adds [unit] spaces. */
    data class Spaces(val unit: Int) : IndentStyle {
        init {
            require(unit > 0) { "An indent unit must be positive, but was $unit." }
        }
    }

    /** Each nesting level adds one tab. */
    data object Tabs : IndentStyle
}

/**
 * What a layout policy may inspect when it answers for one element of a list: the ranges of the element and
 * of its neighbours, taken from the tree the intent was derived from.
 *
 * The context carries no text, because no block tree owns any: the separator a language owns is the span
 * between two elements, and naming that span is all a policy needs to remove or place it. An insertion has
 * no element yet, so [element] is zero-width at the insertion offset.
 */
class LayoutContext(
    val element: TextRange,
    val previous: TextRange?,
    val next: TextRange?,
)

/**
 * The whitespace and separator rules of one language, as the mapping declares them.
 *
 * Layout is the difference between an edit that is correct and one that is acceptable, and no single rule
 * serves JSON, Kotlin and Python, so the engine asks this policy and validates the result instead of
 * computing whitespace itself. The engine always adds the element's own text; the policy answers only for
 * the syntax the language owns around it.
 */
interface LayoutPolicy {
    /** The separator between two elements of a list. */
    val separator: SeparatorStyle

    /** Whether the last element of a list also carries a separator. */
    val trailingSeparator: Boolean

    /** How nested statements are indented. */
    val indent: IndentStyle

    /**
     * The syntax that disappears with [elementRange], outside the element itself.
     *
     * @param elementRange the range of the element being removed.
     * @param context the source and the neighbouring elements, when they exist.
     */
    fun onDelete(elementRange: TextRange, context: LayoutContext): List<DocumentEdit>

    /**
     * The separators that appear when [elementText] is inserted at [offset].
     *
     * @param offset the offset the element is inserted at.
     * @param elementText the text of the element being inserted.
     * @param context the source and the neighbouring elements, when they exist.
     */
    fun onInsert(offset: Int, elementText: String, context: LayoutContext): List<DocumentEdit>

    companion object {
        /**
         * The naive result: no separator, no trailing separator and no indentation.
         *
         * This is the only value legal for a language where whitespace is insignificant, because every other
         * value claims the language has syntax the engine must respect.
         */
        val NONE: LayoutPolicy = object : LayoutPolicy {
            override val separator: SeparatorStyle = SeparatorStyle.None

            override val trailingSeparator: Boolean = false

            override val indent: IndentStyle = IndentStyle.None

            override fun onDelete(elementRange: TextRange, context: LayoutContext): List<DocumentEdit> =
                emptyList()

            override fun onInsert(offset: Int, elementText: String, context: LayoutContext): List<DocumentEdit> =
                emptyList()
        }
    }
}
