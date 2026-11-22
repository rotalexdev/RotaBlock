package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.DomNode
import dev.rotalex.rotablock.language.dom.LanguageId
import dev.rotalex.rotablock.language.dom.NodeKind

/**
 * How one language, or one host, decomposes its syntax into blocks.
 *
 * [handles] is the decomposition boundary: a handled child becomes its own block, an unhandled child
 * collapses into a single editable text field, so no construct is ever a dead end. Both defaults are the
 * generic behaviour, and a mapping that has nothing specific to say leaves them alone.
 */
interface BlockMapping {
    /** The languages this mapping serves. */
    val languages: Set<LanguageId>

    /** The node kinds this mapping decomposes. */
    val handles: Set<NodeKind>

    /** Projects one node, carving it by default. */
    fun project(node: DomNode, context: ProjectionContext): BlockNode =
        context.block(kind = node.kind, range = node.range, parts = context.carve(node), anchor = node)

    /**
     * The text of one created or edited [block], by concatenating its parts in order.
     *
     * It is never called on the whole file: syntax outside the block being written is not this mapping's
     * business.
     */
    fun serialize(block: BlockNode): String = buildString {
        block.parts.forEach { part ->
            when (part) {
                is BlockPart.Field -> append(part.field.text)
                is BlockPart.Slot -> part.slot.children.forEach { append(serialize(it)) }
            }
        }
    }
}
