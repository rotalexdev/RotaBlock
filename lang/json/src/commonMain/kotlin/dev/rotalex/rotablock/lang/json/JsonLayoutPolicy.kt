package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.block.IndentStyle
import dev.rotalex.rotablock.core.block.LayoutContext
import dev.rotalex.rotablock.core.block.LayoutPolicy
import dev.rotalex.rotablock.core.block.SeparatorStyle
import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.TextRange

/**
 * The layout JSON owns: a comma and a space between the elements of a list, no trailing comma, and no
 * indentation.
 *
 * Removal takes the separator that follows the element, or the one before it when it is last, so a list
 * never keeps a comma that its grammar rejects. Insertion places the separator only when the element joins
 * an existing one, so the first element of a list arrives bare. Indentation stays the user's, because JSON
 * never requires it and the framework does not reformat.
 */
object JsonLayoutPolicy : LayoutPolicy {
    override val separator: SeparatorStyle = SeparatorStyle.Custom(leading = "", trailing = ", ")

    override val trailingSeparator: Boolean = false

    override val indent: IndentStyle = IndentStyle.None

    override fun onDelete(elementRange: TextRange, context: LayoutContext): List<DocumentEdit> {
        val next = context.next
        val previous = context.previous
        return when {
            next != null -> listOf(DocumentEdit(elementRange.end, next.start - elementRange.end, ""))
            previous != null -> listOf(DocumentEdit(previous.end, elementRange.start - previous.end, ""))
            else -> emptyList()
        }
    }

    override fun onInsert(offset: Int, elementText: String, context: LayoutContext): List<DocumentEdit> =
        if (context.previous == null) emptyList() else listOf(DocumentEdit(offset, 0, ", "))
}
