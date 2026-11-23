package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockMappingTest {
    private val source = "f(42)"
    private val kind = NodeKind("Call")
    private val node = DomNode(kind, TextRange(0, 5), source)

    private class CallMapping : BlockMapping {
        override val languages: Set<LanguageId> = setOf(LanguageId("kotlin"))
        override val handles: Set<NodeKind> = setOf(NodeKind("Call"), NodeKind("Literal"))
        override val categories: Map<NodeKind, SlotCategory> = mapOf(
            NodeKind("Call") to SlotCategory.EXPRESSION,
            NodeKind("Literal") to SlotCategory.EXPRESSION,
        )
        override val layout: LayoutPolicy = LayoutPolicy.NONE
    }

    @Test
    fun declaresTheLanguagesItServes() {
        assertEquals(setOf(LanguageId("kotlin")), CallMapping().languages)
    }

    @Test
    fun declaresTheKindsItDecomposes() {
        assertTrue(NodeKind("Call") in CallMapping().handles)
        assertFalse(NodeKind("Unlike") in CallMapping().handles)
    }

    @Test
    fun projectDefaultsToCarvingTheNode() {
        val context = FakeProjectionContext(source)
        val block = CallMapping().project(node, context)
        assertEquals(listOf(node), context.carved)
        assertEquals(kind, block.kind)
        assertEquals(TextRange(0, 5), block.range)
        assertEquals(2, block.fields.size)
    }

    @Test
    fun aMappingMayOverrideProjection() {
        val custom = object : BlockMapping {
            override val languages: Set<LanguageId> = setOf(LanguageId("kotlin"))
            override val handles: Set<NodeKind> = emptySet()
            override val categories: Map<NodeKind, SlotCategory> = emptyMap()
            override val layout: LayoutPolicy = LayoutPolicy.NONE
            override fun project(node: DomNode, context: ProjectionContext): BlockNode =
                context.block(NodeKind("Opaque"), node.range, listOf(BlockPart.Field(context.field("text", context.textOf(node), true, node.range))), node)
        }
        val block = custom.project(node, FakeProjectionContext(source))
        assertEquals(NodeKind("Opaque"), block.kind)
        assertEquals("f(42)", block.fields.single().text)
    }

    @Test
    fun serializeConcatenatesPartsInOrder() {
        val mapping = CallMapping()
        val open = BlockField.chrome("open", "f(")
        val value = BlockField("value", "42", editable = true, range = TextRange(2, 4))
        val close = BlockField.chrome("close", ")")
        val block = BlockNode(
            BlockId("call"),
            kind,
            SlotCategory.EXPRESSION,
            null,
            TextRange(0, 5),
            listOf(BlockPart.Field(open), BlockPart.Field(value), BlockPart.Field(close)),
        )
        assertEquals("f(42)", mapping.serialize(block))
    }

    @Test
    fun serializeRecursesIntoSlotChildren() {
        val mapping = CallMapping()
        val inner = BlockNode(BlockId("inner"), kind, SlotCategory.EXPRESSION, null, TextRange(2, 4), listOf(BlockPart.Field(BlockField.chrome("text", "42"))))
        val slot = BlockSlot(SlotCategory.EXPRESSION, listOf(inner), false, null, ValueKind.NUMBER)
        val block = BlockNode(
            BlockId("outer"),
            kind,
            SlotCategory.EXPRESSION,
            null,
            TextRange(0, 5),
            listOf(BlockPart.Field(BlockField.chrome("open", "f(")), BlockPart.Slot(slot), BlockPart.Field(BlockField.chrome("close", ")"))),
        )
        assertEquals("f(42)", mapping.serialize(block))
    }

    @Test
    fun serializeOfALeafIsItsFieldText() {
        val leaf = BlockNode(BlockId("leaf"), kind, SlotCategory.EXPRESSION, null, TextRange(0, 2), listOf(BlockPart.Field(BlockField("text", "42", false, null))))
        assertEquals("42", CallMapping().serialize(leaf))
    }
}
