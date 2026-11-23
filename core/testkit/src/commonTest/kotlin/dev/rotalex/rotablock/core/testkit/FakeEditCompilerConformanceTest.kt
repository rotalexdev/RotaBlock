package dev.rotalex.rotablock.core.testkit

import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockField
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.BlockProjectionService
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.BlockSlot
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind
import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.ParsedFile
import dev.rotalex.rotablock.core.language.TextRange

class FakeEditCompilerConformanceTest : EditCompilerConformance() {
    private val language = LanguageId("fake")
    private val text = "12"
    private val first = BlockNode(
        BlockId("first"),
        NodeKind("Digit"),
        SlotCategory.EXPRESSION,
        null,
        TextRange(0, 1),
        listOf(BlockPart.Field(BlockField("text", "1", true, TextRange(0, 1)))),
    )
    private val second = BlockNode(
        BlockId("second"),
        NodeKind("Digit"),
        SlotCategory.EXPRESSION,
        null,
        TextRange(1, 2),
        listOf(BlockPart.Field(BlockField("text", "2", true, TextRange(1, 2)))),
    )
    private val tree = BlockTree(
        BlockNode(
            BlockId("root"),
            NodeKind.ROOT,
            SlotCategory.STATEMENT,
            null,
            TextRange(0, 2),
            listOf(
                BlockPart.Slot(
                    BlockSlot(SlotCategory.STATEMENT, listOf(first, second), true, TextRange(0, 2), ValueKind.UNKNOWN),
                ),
            ),
        ),
        1L,
        language,
    )

    private val service = object : BlockProjectionService {
        override fun project(parsed: ParsedFile, language: LanguageId): BlockTree = tree

        override fun compile(edit: BlockEdit, tree: BlockTree): List<DocumentEdit> = when (edit) {
            is BlockEdit.Delete -> {
                val block = tree.block(edit.block.id)
                if (block == null) emptyList() else listOf(DocumentEdit(block.range.start, block.range.length, ""))
            }
            is BlockEdit.SetField -> {
                val block = tree.block(edit.target.id)
                val field = block?.fields?.getOrNull(edit.fieldIndex)?.range
                if (field == null) emptyList() else listOf(DocumentEdit(field.start, field.length, edit.text))
            }
            else -> emptyList()
        }
    }

    override fun service(): BlockProjectionService = service

    override fun scenario(): EditScenario = EditScenario(
        text = text,
        tree = tree,
        edits = listOf(
            BlockEdit.Delete(BlockRef(BlockId("first"))),
            BlockEdit.SetField(BlockRef(BlockId("second")), 0, "3"),
        ),
    )
}
