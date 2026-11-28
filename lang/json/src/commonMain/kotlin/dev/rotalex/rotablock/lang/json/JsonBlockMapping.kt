package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.block.BlockMapping
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.BlockTemplate
import dev.rotalex.rotablock.core.block.LayoutPolicy
import dev.rotalex.rotablock.core.block.ProjectionContext
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind
import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

/**
 * The JSON decomposition: values become blocks, a member becomes a declaration with an editable key, and a
 * container becomes chrome around one multiple slot of its elements.
 *
 * Everything the grammar cannot express structurally is left to the engine: an unhandled node becomes one
 * editable field, so a document that is entirely a parse error is still a block a reader can repair.
 */
class JsonBlockMapping : BlockMapping {
    override val languages: Set<LanguageId> = setOf(TreeSitterJson.id)

    override val handles: Set<NodeKind> = JsonKinds.handled

    override val categories: Map<NodeKind, SlotCategory> = JsonKinds.handled.associateWith { kind ->
        when (kind) {
            NodeKind.ROOT -> SlotCategory.OPAQUE
            JsonKinds.PAIR -> SlotCategory.DECLARATION
            else -> SlotCategory.EXPRESSION
        }
    }

    override val layout: LayoutPolicy = JsonLayoutPolicy

    override fun project(node: DomNode, context: ProjectionContext): BlockNode = when (node.kind) {
        NodeKind.ROOT -> container(node, context, SlotCategory.OPAQUE, ValueKind.UNKNOWN, listOf(VALUE_TEMPLATE))
        JsonKinds.OBJECT -> container(node, context, SlotCategory.DECLARATION, ValueKind.OBJECT, listOf(MEMBER_TEMPLATE))
        JsonKinds.ARRAY -> container(node, context, SlotCategory.EXPRESSION, ValueKind.ARRAY, listOf(ELEMENT_TEMPLATE))
        JsonKinds.PAIR -> member(node, context)
        else -> literal(node, context)
    }

    private fun container(
        node: DomNode,
        context: ProjectionContext,
        category: SlotCategory,
        valueKind: ValueKind,
        templates: List<BlockTemplate>,
    ): BlockNode {
        val children = node.children
        val start = node.range.start
        val end = node.range.end
        if (children.isEmpty()) {
            val parts = listOf(
                BlockPart.Field(context.field("open", context.textOf(node), editable = false, range = node.range)),
                BlockPart.Slot(
                    context.slot(
                        category = category,
                        children = emptyList(),
                        multiple = true,
                        range = if (end - start >= 2) TextRange(start + 1, end - 1) else TextRange(start, start),
                        templates = templates,
                    ),
                ),
            )
            return context.block(node.kind, node.range, parts, anchor = node, valueKind = valueKind)
        }
        val first = children.first()
        val last = children.last()
        val parts = listOf(
            BlockPart.Field(context.field("open", slice(context, start, first.range.start), editable = false)),
            BlockPart.Slot(
                context.slot(
                    category = category,
                    children = children.map(context::child),
                    multiple = true,
                    range = TextRange(first.range.start, last.range.end),
                    templates = templates,
                ),
            ),
            BlockPart.Field(context.field("close", slice(context, last.range.end, end), editable = false)),
        )
        return context.block(node.kind, node.range, parts, anchor = node, valueKind = valueKind)
    }

    private fun member(node: DomNode, context: ProjectionContext): BlockNode {
        val key = node.children.firstOrNull()
        val value = node.children.getOrNull(1)
        if (key == null || key.kind != JsonKinds.STRING || key.range.length < 2 || value == null) {
            return context.block(node.kind, node.range, context.carve(node), anchor = node)
        }
        val inner = TextRange(key.range.start + 1, key.range.end - 1)
        val parts = listOf(
            BlockPart.Field(context.field("openQuote", "\"", editable = false, range = TextRange(key.range.start, inner.start))),
            BlockPart.Field(context.field("key", slice(context, inner.start, inner.end), editable = true, range = inner)),
            BlockPart.Field(context.field("closeQuote", "\"", editable = false, range = TextRange(inner.end, key.range.end))),
            BlockPart.Field(
                context.field(
                    "separator",
                    slice(context, key.range.end, value.range.start),
                    editable = false,
                    range = TextRange(key.range.end, value.range.start),
                ),
            ),
            BlockPart.Slot(
                context.slot(
                    category = SlotCategory.EXPRESSION,
                    children = listOf(context.child(value)),
                    multiple = false,
                    range = value.range,
                    valueKind = blockKindOf(value),
                ),
            ),
        )
        return context.block(node.kind, node.range, parts, anchor = node)
    }

    private fun literal(node: DomNode, context: ProjectionContext): BlockNode {
        val parts = if (node.kind == JsonKinds.STRING && node.range.length >= 2) {
            quoted(node, context)
        } else {
            listOf(BlockPart.Field(context.field("literal", context.textOf(node), editable = true, range = node.range)))
        }
        return context.block(node.kind, node.range, parts, anchor = node, valueKind = valueKindOf(node.kind))
    }

    private fun quoted(node: DomNode, context: ProjectionContext): List<BlockPart> {
        val start = node.range.start
        val end = node.range.end
        return listOf(
            BlockPart.Field(context.field("openQuote", "\"", editable = false, range = TextRange(start, start + 1))),
            BlockPart.Field(
                context.field("value", slice(context, start + 1, end - 1), editable = true, range = TextRange(start + 1, end - 1)),
            ),
            BlockPart.Field(context.field("closeQuote", "\"", editable = false, range = TextRange(end - 1, end))),
        )
    }

    private fun slice(context: ProjectionContext, start: Int, end: Int): String =
        context.source.subSequence(start, end).toString()

    private fun blockKindOf(node: DomNode): ValueKind = when (node.kind) {
        JsonKinds.OBJECT -> ValueKind.OBJECT
        JsonKinds.ARRAY -> ValueKind.ARRAY
        else -> valueKindOf(node.kind)
    }

    private fun valueKindOf(kind: NodeKind): ValueKind = when (kind) {
        JsonKinds.STRING -> ValueKind.STRING
        JsonKinds.NUMBER -> ValueKind.NUMBER
        JsonKinds.TRUE, JsonKinds.FALSE -> ValueKind.BOOLEAN
        JsonKinds.NULL -> ValueKind.NULL
        else -> ValueKind.UNKNOWN
    }
}

private val MEMBER_TEMPLATE = BlockTemplate("member", JsonKinds.PAIR, "\"key\": null", "")

private val ELEMENT_TEMPLATE = BlockTemplate("element", JsonKinds.NULL, "null", "")

private val VALUE_TEMPLATE = BlockTemplate("value", JsonKinds.NULL, "null", "")
