package dev.rotalex.rotablock.core.engine

import dev.rotalex.rotablock.core.block.BlockField
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockMapping
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.BlockSlot
import dev.rotalex.rotablock.core.block.ProjectionContext
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind
import dev.rotalex.rotablock.core.block.ValueKindOracle
import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

/**
 * The engine side of the projection seam: it reads the source, interleaves chrome, projects children,
 * resolves categories and assigns block ids, so a mapping only has to say what a node decomposes into.
 *
 * Ids come from the parse position of each node, never from a traversal counter, so an edit that leaves the
 * structure of a subtree untouched leaves its ids untouched too.
 */
class EngineProjectionContext(
    override val source: CharSequence,
    private val mapping: BlockMapping?,
    private val oracle: ValueKindOracle?,
) : ProjectionContext {
    private var synthetic = 0

    /** Projects [node]: through its mapping when the mapping handles its kind, as one opaque block otherwise. */
    fun projectNode(node: DomNode): BlockNode =
        if (mapping != null && node.kind in mapping.handles) mapping.project(node, this) else opaque(node)

    override fun textOf(node: DomNode): String = node.text()

    override fun carve(node: DomNode): List<BlockPart> {
        if (node.children.isEmpty()) {
            return listOf(BlockPart.Field(field("text", node.text(), editable = true, range = node.range)))
        }
        val parts = mutableListOf<BlockPart>()
        var cursor = node.range.start
        node.children.forEach { childNode ->
            if (childNode.range.start > cursor) {
                parts += BlockPart.Field(chrome(cursor, childNode.range.start))
            }
            val projected = child(childNode)
            parts += BlockPart.Slot(
                slot(
                    category = SlotCategory.OPAQUE,
                    children = listOf(projected),
                    multiple = false,
                    range = childNode.range,
                    valueKind = projected.valueKind,
                ),
            )
            cursor = childNode.range.end
        }
        if (node.range.end > cursor) {
            parts += BlockPart.Field(chrome(cursor, node.range.end))
        }
        return parts
    }

    override fun child(node: DomNode): BlockNode = projectNode(node)

    override fun category(kind: NodeKind): SlotCategory {
        val declared = mapping?.categories?.get(kind)
        if (declared != null) return declared
        return if (mapping != null && kind in mapping.handles) {
            throw IllegalStateException("The mapping handles ${kind.id} without declaring its category.")
        } else {
            SlotCategory.OPAQUE
        }
    }

    override fun block(
        kind: NodeKind,
        range: TextRange,
        parts: List<BlockPart>,
        anchor: DomNode?,
        label: String?,
        valueKind: ValueKind,
    ): BlockNode {
        val id = idOf(anchor)
        val declared = category(kind)
        val produced = BlockNode(id, kind, declared, anchor, range, parts, label, valueKind)
        val refined = oracle?.valueKindOf(produced) ?: return produced
        return if (refined == valueKind) produced else BlockNode(id, kind, declared, anchor, range, parts, label, refined)
    }

    override fun slot(
        category: SlotCategory,
        children: List<BlockNode>,
        multiple: Boolean,
        range: TextRange?,
        valueKind: ValueKind,
    ): BlockSlot = BlockSlot(category, children, multiple, range, valueKind)

    override fun field(role: String, text: String, editable: Boolean, range: TextRange?): BlockField =
        BlockField(role, text, editable, range)

    private fun opaque(node: DomNode): BlockNode = block(
        kind = node.kind,
        range = node.range,
        parts = listOf(BlockPart.Field(field("text", node.text(), editable = true, range = node.range))),
        anchor = node,
    )

    private fun chrome(start: Int, end: Int): BlockField =
        BlockField.chrome("syntax", source.subSequence(start, end).toString(), TextRange(start, end))

    private fun idOf(anchor: DomNode?): BlockId {
        if (anchor == null) {
            val id = BlockId("synthetic#" + synthetic)
            synthetic += 1
            return id
        }
        val segments = mutableListOf<String>()
        var current: DomNode? = anchor
        while (current != null) {
            val parent = current.parent
            segments += if (parent == null) current.kind.id else "${current.kind.id}#${parent.children.indexOf(current)}"
            current = parent
        }
        return BlockId(segments.asReversed().joinToString("/"))
    }
}
