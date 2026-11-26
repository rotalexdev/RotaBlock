package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.BlockField
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.BlockSlot
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange

internal fun leaf(
    text: String,
    id: String = text,
    kind: String = "test.leaf",
    category: SlotCategory = SlotCategory.EXPRESSION,
    valueKind: ValueKind = ValueKind.STRING,
    label: String? = null,
): BlockNode {
    val range = TextRange(0, text.length)
    return BlockNode(
        id = BlockId(id),
        kind = NodeKind(kind),
        category = category,
        anchor = null,
        range = range,
        parts = listOf(
            BlockPart.Field(BlockField(role = "text", text = text, editable = true, range = range)),
        ),
        label = label,
        valueKind = valueKind,
    )
}

internal fun owner(
    id: String,
    children: List<BlockNode>,
    slots: Int = 1,
    slotCategory: SlotCategory = SlotCategory.STATEMENT,
    category: SlotCategory = SlotCategory.STATEMENT,
    kind: String = "test.statement",
    syntax: String = "do",
    label: String? = null,
): BlockNode {
    val parts = mutableListOf<BlockPart>(BlockPart.Field(BlockField.chrome("syntax", syntax)))
    repeat(slots) { index ->
        parts += BlockPart.Slot(
            BlockSlot(
                category = slotCategory,
                children = if (index == 0) children else emptyList(),
                multiple = true,
                range = null,
                valueKind = ValueKind.UNKNOWN,
            ),
        )
    }
    return BlockNode(
        id = BlockId(id),
        kind = NodeKind(kind),
        category = category,
        anchor = null,
        range = TextRange(0, 0),
        parts = parts,
        label = label,
    )
}

internal fun field(text: String, editable: Boolean = true): BlockField {
    val range = TextRange(0, text.length)
    return BlockField(role = "text", text = text, editable = editable, range = range)
}

internal fun slotOf(
    category: SlotCategory,
    children: List<BlockNode>,
    valueKind: ValueKind = ValueKind.UNKNOWN,
    multiple: Boolean = false,
): BlockSlot = BlockSlot(
    category = category,
    children = children,
    multiple = multiple,
    range = null,
    valueKind = valueKind,
)

internal fun withParts(
    id: String,
    parts: List<BlockPart>,
    kind: String = "test.block",
    category: SlotCategory = SlotCategory.STATEMENT,
    label: String? = null,
    valueKind: ValueKind = ValueKind.UNKNOWN,
): BlockNode = BlockNode(
    id = BlockId(id),
    kind = NodeKind(kind),
    category = category,
    anchor = null,
    range = TextRange(0, 0),
    parts = parts,
    label = label,
    valueKind = valueKind,
)

internal fun treeOf(children: List<BlockNode>): BlockTree = BlockTree(
    root = BlockNode(
        id = BlockId("root"),
        kind = NodeKind.ROOT,
        category = SlotCategory.OPAQUE,
        anchor = null,
        range = TextRange(0, 0),
        parts = listOf(
            BlockPart.Slot(
                BlockSlot(
                    category = SlotCategory.OPAQUE,
                    children = children,
                    multiple = true,
                    range = null,
                    valueKind = ValueKind.UNKNOWN,
                ),
            ),
        ),
    ),
    version = 1L,
    language = LanguageId("test"),
)

internal val testMetrics = BlockMetrics(grid = 4f)

internal val charSizer = TextSizer { text, _ -> text.length * 8f }
