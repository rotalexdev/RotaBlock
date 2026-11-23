package dev.rotalex.rotablock.core.engine

import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockField
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockMapping
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.BlockProjectionService
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.BlockSlot
import dev.rotalex.rotablock.core.block.BlockTemplate
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.block.IndentStyle
import dev.rotalex.rotablock.core.block.LayoutContext
import dev.rotalex.rotablock.core.block.LayoutPolicy
import dev.rotalex.rotablock.core.block.SeparatorStyle
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.SlotRef
import dev.rotalex.rotablock.core.block.ValueKind
import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.ExtensionPoint
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange
import dev.rotalex.rotablock.core.testkit.EditCompilerConformance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditCompilerTest : EditCompilerConformance() {
    private val language = LanguageId("commas")
    private val element = NodeKind("element")
    private val one = field("one", "1", TextRange(1, 2))
    private val two = field("two", "2", TextRange(4, 5))
    private val tree = BlockTree(
        BlockNode(
            BlockId("array"),
            NodeKind("array"),
            SlotCategory.EXPRESSION,
            null,
            TextRange(0, 6),
            listOf(
                BlockPart.Field(BlockField.chrome("open", "[")),
                BlockPart.Slot(
                    BlockSlot(
                        SlotCategory.EXPRESSION,
                        listOf(one, two),
                        multiple = true,
                        range = TextRange(1, 5),
                        valueKind = ValueKind.NUMBER,
                    ),
                ),
                BlockPart.Field(BlockField.chrome("close", "]")),
            ),
        ),
        1L,
        language,
    )
    private val mapping = object : BlockMapping {
        override val languages: Set<LanguageId> = setOf(language)
        override val handles: Set<NodeKind> = setOf(element)
        override val categories: Map<NodeKind, SlotCategory> = mapOf(element to SlotCategory.EXPRESSION)
        override val layout: LayoutPolicy = CommaLayout
    }
    private val point = ExtensionPoint<BlockMapping>("test.mappings").apply { register(mapping) }
    private val compiler = EditCompiler(point)

    override fun service(): BlockProjectionService = EngineProjectionService(point)

    override fun scenario(): EditScenario = EditScenario(
        text = "[1, 2]",
        tree = tree,
        edits = listOf(
            BlockEdit.SetField(BlockRef(BlockId("one")), 0, "3"),
            BlockEdit.Delete(BlockRef(BlockId("one"))),
            BlockEdit.Delete(BlockRef(BlockId("two"))),
            BlockEdit.Wrap(BlockRef(BlockId("two")), BlockTemplate("group", NodeKind("group"), "(", ")")),
            BlockEdit.Move(BlockRef(BlockId("one")), SlotRef(BlockRef(BlockId("array")), 0, 2)),
            BlockEdit.InsertTemplate(SlotRef(BlockRef(BlockId("array")), 0, 2), BlockTemplate("nine", element, "9", "")),
            BlockEdit.ReplaceWithText(SlotRef(BlockRef(BlockId("array")), 0, 2), "9"),
        ),
    )

    @Test
    fun aFieldEditReplacesTheAddressedFieldRange() {
        assertEquals(
            listOf(DocumentEdit(1, 1, "3")),
            compiler.compile(BlockEdit.SetField(BlockRef(BlockId("one")), 0, "3"), tree),
        )
    }

    @Test
    fun aFieldIndexThatIsNotEditableYieldsNoEdit() {
        assertTrue(compiler.compile(BlockEdit.SetField(BlockRef(BlockId("array")), 0, "x"), tree).isEmpty())
        assertTrue(compiler.compile(BlockEdit.SetField(BlockRef(BlockId("one")), 7, "x"), tree).isEmpty())
    }

    @Test
    fun deletingTheFirstElementTakesItsTrailingSeparator() {
        assertEquals(
            listOf(DocumentEdit(1, 3, "")),
            compiler.compile(BlockEdit.Delete(BlockRef(BlockId("one"))), tree),
        )
    }

    @Test
    fun deletingTheLastElementTakesThePrecedingSeparator() {
        assertEquals(
            listOf(DocumentEdit(2, 3, "")),
            compiler.compile(BlockEdit.Delete(BlockRef(BlockId("two"))), tree),
        )
    }

    @Test
    fun theRootCannotBeDeleted() {
        assertTrue(compiler.compile(BlockEdit.Delete(BlockRef(BlockId("array"))), tree).isEmpty())
    }

    @Test
    fun anInsertIntoAnEmptySlotCarriesNoSeparator() {
        assertEquals(
            listOf(DocumentEdit(1, 0, "9")),
            EditCompiler(point).compile(
                BlockEdit.InsertTemplate(SlotRef(BlockRef(BlockId("array")), 0, 0), BlockTemplate("nine", element, "9", "")),
                emptyTree(),
            ),
        )
    }

    @Test
    fun anAppendPlacesTheSeparatorBeforeTheElement() {
        assertEquals(
            listOf(DocumentEdit(5, 0, ", 9")),
            compiler.compile(
                BlockEdit.InsertTemplate(SlotRef(BlockRef(BlockId("array")), 0, 2), BlockTemplate("nine", element, "9", "")),
                tree,
            ),
        )
    }

    @Test
    fun anInsertBeforeAnElementPlacesTheSeparatorAfterIt() {
        assertEquals(
            listOf(DocumentEdit(1, 0, "9, ")),
            compiler.compile(
                BlockEdit.InsertTemplate(SlotRef(BlockRef(BlockId("array")), 0, 0), BlockTemplate("nine", element, "9", "")),
                tree,
            ),
        )
    }

    @Test
    fun aTextReplacementInsideASlotAddressesTheChildItReplaces() {
        assertEquals(
            listOf(DocumentEdit(4, 1, "9")),
            compiler.compile(BlockEdit.ReplaceWithText(SlotRef(BlockRef(BlockId("array")), 0, 1), "9"), tree),
        )
    }

    @Test
    fun wrappingReplacesTheBlockWithItsWrappedText() {
        assertEquals(
            listOf(DocumentEdit(4, 1, "(2)")),
            compiler.compile(
                BlockEdit.Wrap(BlockRef(BlockId("two")), BlockTemplate("group", NodeKind("group"), "(", ")")),
                tree,
            ),
        )
    }

    @Test
    fun movingTheFirstElementAfterTheLastYieldsTwoDisjointEdits() {
        assertEquals(
            listOf(DocumentEdit(5, 0, ", 1"), DocumentEdit(1, 3, "")),
            compiler.compile(BlockEdit.Move(BlockRef(BlockId("one")), SlotRef(BlockRef(BlockId("array")), 0, 2)), tree),
        )
    }

    @Test
    fun movingAnElementWhereItAlreadyIsYieldsNoEdit() {
        assertTrue(
            compiler.compile(BlockEdit.Move(BlockRef(BlockId("one")), SlotRef(BlockRef(BlockId("array")), 0, 0)), tree).isEmpty(),
        )
        assertTrue(
            compiler.compile(BlockEdit.Move(BlockRef(BlockId("one")), SlotRef(BlockRef(BlockId("array")), 0, 1)), tree).isEmpty(),
        )
    }

    @Test
    fun aSlotThatDoesNotExistYieldsNoEdit() {
        assertTrue(
            compiler.compile(
                BlockEdit.InsertTemplate(SlotRef(BlockRef(BlockId("array")), 4, 0), BlockTemplate("nine", element, "9", "")),
                tree,
            ).isEmpty(),
        )
        assertTrue(
            compiler.compile(
                BlockEdit.InsertTemplate(SlotRef(BlockRef(BlockId("one")), 0, 0), BlockTemplate("nine", element, "9", "")),
                tree,
            ).isEmpty(),
        )
    }

    @Test
    fun aTreeWithoutAMappingUsesTheNaiveLayout() {
        val naive = EditCompiler(ExtensionPoint<BlockMapping>("test.empty"))
        assertEquals(
            listOf(DocumentEdit(1, 1, "")),
            naive.compile(BlockEdit.Delete(BlockRef(BlockId("one"))), tree),
        )
    }

    private fun emptyTree(): BlockTree = BlockTree(
        BlockNode(
            BlockId("array"),
            NodeKind("array"),
            SlotCategory.EXPRESSION,
            null,
            TextRange(0, 2),
            listOf(
                BlockPart.Field(BlockField.chrome("open", "[")),
                BlockPart.Slot(
                    BlockSlot(
                        SlotCategory.EXPRESSION,
                        emptyList(),
                        multiple = true,
                        range = TextRange(1, 1),
                        valueKind = ValueKind.UNKNOWN,
                    ),
                ),
                BlockPart.Field(BlockField.chrome("close", "]")),
            ),
        ),
        1L,
        language,
    )

    private fun field(id: String, text: String, range: TextRange): BlockNode = BlockNode(
        BlockId(id),
        element,
        SlotCategory.EXPRESSION,
        null,
        range,
        listOf(BlockPart.Field(BlockField("text", text, true, range))),
    )

    private object CommaLayout : LayoutPolicy {
        override val separator: SeparatorStyle = SeparatorStyle.Comma
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

        override fun onInsert(offset: Int, elementText: String, context: LayoutContext): List<DocumentEdit> = when {
            context.previous != null -> listOf(DocumentEdit(offset, 0, ", "))
            context.next != null -> listOf(DocumentEdit(offset + elementText.length, 0, ", "))
            else -> emptyList()
        }
    }
}
