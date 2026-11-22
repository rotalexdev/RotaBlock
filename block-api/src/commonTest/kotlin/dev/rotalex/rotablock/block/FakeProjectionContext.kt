package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.DomNode
import dev.rotalex.rotablock.language.dom.NodeKind
import dev.rotalex.rotablock.language.dom.TextRange

class FakeProjectionContext(override val source: CharSequence) : ProjectionContext {
    val carved: MutableList<DomNode> = mutableListOf()
    val projected: MutableList<DomNode> = mutableListOf()
    private var counter = 0

    override fun textOf(node: DomNode): String = node.text()

    override fun carve(node: DomNode): List<BlockPart> {
        carved += node
        return listOf(
            BlockPart.Field(BlockField.chrome("open", "<", TextRange(node.range.start, node.range.start))),
            BlockPart.Field(BlockField.chrome("close", ">", TextRange(node.range.end, node.range.end))),
        )
    }

    override fun child(node: DomNode): BlockNode {
        projected += node
        return block(node.kind, node.range, emptyList(), node)
    }

    override fun block(
        kind: NodeKind,
        range: TextRange,
        parts: List<BlockPart>,
        anchor: DomNode?,
        label: String?,
        valueKind: ValueKind,
    ): BlockNode {
        counter += 1
        return BlockNode(BlockId("b" + counter), kind, anchor, range, parts, label, valueKind)
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
}
