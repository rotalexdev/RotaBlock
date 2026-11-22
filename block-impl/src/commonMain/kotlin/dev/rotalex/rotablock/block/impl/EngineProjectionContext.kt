package dev.rotalex.rotablock.block.impl

import dev.rotalex.rotablock.block.BlockField
import dev.rotalex.rotablock.block.BlockId
import dev.rotalex.rotablock.block.BlockMapping
import dev.rotalex.rotablock.block.BlockNode
import dev.rotalex.rotablock.block.BlockPart
import dev.rotalex.rotablock.block.BlockSlot
import dev.rotalex.rotablock.block.ProjectionContext
import dev.rotalex.rotablock.block.SlotCategory
import dev.rotalex.rotablock.block.ValueKind
import dev.rotalex.rotablock.block.ValueKindOracle
import dev.rotalex.rotablock.language.dom.DomNode
import dev.rotalex.rotablock.language.dom.NodeKind
import dev.rotalex.rotablock.language.dom.TextRange

/**
 * The engine side of the projection seam: it reads the source, interleaves chrome, projects children and
 * assigns block ids, so a mapping only has to say what a node decomposes into.
 *
 * One instance serves exactly one projection, which is what makes the id counter deterministic.
 */
class EngineProjectionContext(
    override val source: CharSequence,
    private val mapping: BlockMapping?,
    private val oracle: ValueKindOracle?,
) : ProjectionContext {
    private var counter = 0
    private val reserved: MutableList<BlockId> = mutableListOf()

    /** Projects [node]: through its mapping when the mapping handles its kind, as one opaque block otherwise. */
    fun projectNode(node: DomNode): BlockNode =
        buildNode { if (mapping != null && node.kind in mapping.handles) mapping.project(node, this) else opaque(node) }

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

    override fun block(
        kind: NodeKind,
        range: TextRange,
        parts: List<BlockPart>,
        anchor: DomNode?,
        label: String?,
        valueKind: ValueKind,
    ): BlockNode {
        val id = takeId()
        val produced = BlockNode(id, kind, anchor, range, parts, label, valueKind)
        val refined = oracle?.valueKindOf(produced) ?: return produced
        return if (refined == valueKind) produced else BlockNode(id, kind, anchor, range, parts, label, refined)
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

    private inline fun buildNode(produce: () -> BlockNode): BlockNode {
        val id = BlockId("b" + counter)
        counter += 1
        reserved.add(id)
        val produced = produce()
        if (reserved.isNotEmpty() && reserved.last() == id) reserved.removeAt(reserved.lastIndex)
        return produced
    }

    private fun takeId(): BlockId =
        if (reserved.isEmpty()) {
            val id = BlockId("b" + counter)
            counter += 1
            id
        } else {
            reserved.removeAt(reserved.lastIndex)
        }
}
