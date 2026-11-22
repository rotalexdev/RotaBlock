package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

/**
 * The seam a [BlockMapping] projects through.
 *
 * A mapping decides what a node decomposes into; the engine owns everything a mapping must not: reading
 * the source, interleaving chrome, projecting children and assigning block ids. That split is what keeps a
 * mapping pure and projection deterministic.
 */
interface ProjectionContext {
    /** The text the projection is computed against. */
    val source: CharSequence

    /** The source text of [node]. */
    fun textOf(node: DomNode): String

    /**
     * The generic interleave of [node]: the literal source between consecutive children as read-only chrome
     * fields, with one single-child slot per child, and trailing chrome.
     */
    fun carve(node: DomNode): List<BlockPart>

    /** Projects one child node in isolation. */
    fun child(node: DomNode): BlockNode

    /** Builds a block, letting the engine assign its id. */
    fun block(
        kind: NodeKind,
        range: TextRange,
        parts: List<BlockPart>,
        anchor: DomNode? = null,
        label: String? = null,
        valueKind: ValueKind = ValueKind.UNKNOWN,
    ): BlockNode

    /** Builds a slot. */
    fun slot(
        category: SlotCategory,
        children: List<BlockNode>,
        multiple: Boolean,
        range: TextRange? = null,
        valueKind: ValueKind = ValueKind.UNKNOWN,
    ): BlockSlot

    /** Builds a field. */
    fun field(role: String, text: String, editable: Boolean, range: TextRange? = null): BlockField
}
