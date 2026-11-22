package dev.rotalex.rotablock.core.engine

import dev.rotalex.rotablock.core.block.BlockMapping
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind
import dev.rotalex.rotablock.core.block.ValueKindOracle
import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.ExtensionPoint
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.ParsedFile
import dev.rotalex.rotablock.core.language.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockProjectorTest {
    private val source = "f(42)"
    private val kotlin = LanguageId("test")
    private val call = NodeKind("Call")
    private val literal = NodeKind("Literal")

    private class TestMapping(
        override val languages: Set<LanguageId> = setOf(LanguageId("test")),
        override val handles: Set<NodeKind> = setOf(NodeKind("Call")),
    ) : BlockMapping

    private fun registrations(vararg mappings: BlockMapping): ExtensionPoint<BlockMapping> {
        val point = ExtensionPoint<BlockMapping>("test.mappings")
        mappings.forEach { point.register(it) }
        return point
    }

    private fun callNode() = DomNode(
        call,
        TextRange(0, 5),
        source,
        listOf(DomNode(literal, TextRange(2, 4), source)),
    )

    private fun parsed(root: DomNode = callNode(), version: Long = 1L) = ParsedFile(source, version, root)

    private fun project(
        parsed: ParsedFile = parsed(),
        mapper: TestMapping = TestMapping(),
        oracle: ValueKindOracle? = null,
    ): BlockNode = BlockProjector(registrations(mapper), oracle).project(parsed, kotlin).root

    private fun idsOf(node: BlockNode): List<String> =
        listOf(node.id.value) + node.slots.flatMap { slot -> slot.children.flatMap { idsOf(it) } }

    @Test
    fun carvesSourceChromeAroundEachChild() {
        val root = project()
        assertEquals(3, root.parts.size)
        val open = root.parts[0] as BlockPart.Field
        assertEquals("f(", open.field.text)
        assertEquals(TextRange(0, 2), open.field.range)
        assertTrue(!open.field.editable)
        val close = root.parts[2] as BlockPart.Field
        assertEquals(")", close.field.text)
        assertEquals(TextRange(4, 5), close.field.range)
    }

    @Test
    fun eachChildGetsItsOwnSingleChildSlot() {
        val root = project()
        val slot = (root.parts[1] as BlockPart.Slot).slot
        assertEquals(1, slot.children.size)
        assertEquals(TextRange(2, 4), slot.range)
        assertTrue(!slot.multiple)
        assertEquals(SlotCategory.OPAQUE, slot.category)
    }

    @Test
    fun aHandledChildIsProjectedByItsMapping() {
        val root = project(mapper = TestMapping(handles = setOf(call, literal)))
        val child = (root.parts[1] as BlockPart.Slot).slot.children.single()
        assertEquals(literal, child.kind)
        assertEquals("42", child.fields.single().text)
    }

    @Test
    fun anUnhandledChildCollapsesIntoOneEditableField() {
        val child = (project().parts[1] as BlockPart.Slot).slot.children.single()
        assertEquals(literal, child.kind)
        assertEquals(1, child.parts.size)
        val field = child.fields.single()
        assertEquals("42", field.text)
        assertEquals(TextRange(2, 4), field.range)
        assertTrue(field.editable)
    }

    @Test
    fun aChildlessNodeBecomesOneEditableField() {
        val root = project(parsed = parsed(DomNode(literal, TextRange(0, 5), source)))
        assertEquals(1, root.parts.size)
        assertEquals("f(42)", root.fields.single().text)
        assertTrue(root.fields.single().editable)
    }

    @Test
    fun aLanguageWithoutAMappingStillProjectsEveryNodeAsOpaque() {
        val root = BlockProjector(registrations(), null).project(parsed(), kotlin).root
        assertEquals(1, root.parts.size)
        assertEquals("f(42)", root.fields.single().text)
    }

    @Test
    fun theTreeVersionMirrorsTheDocumentVersion() {
        assertEquals(7L, BlockProjector(registrations(TestMapping()), null).project(parsed(version = 7L), kotlin).version)
    }

    @Test
    fun projectionIsDeterministicIncludingIds() {
        val first = BlockProjector(registrations(TestMapping()), null).project(parsed(), kotlin).root
        val second = BlockProjector(registrations(TestMapping()), null).project(parsed(), kotlin).root
        assertEquals(idsOf(first), idsOf(second))
        assertEquals(listOf("b0", "b1"), idsOf(first))
    }

    @Test
    fun everyIdIsUnique() {
        val ids = idsOf(project())
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun anOracleRefinesTheProducedKind() {
        val oracle = ValueKindOracle { ValueKind.NUMBER }
        assertEquals(ValueKind.NUMBER, project(oracle = oracle).valueKind)
    }

    @Test
    fun withoutARefiningOracleTheKindClaimsNothing() {
        assertEquals(ValueKind.UNKNOWN, project().valueKind)
        assertNull(ValueKindOracle { null }.valueKindOf(project()))
    }

    @Test
    fun aSlotTakesItsKindFromTheContentItHolds() {
        val root = project(oracle = ValueKindOracle { ValueKind.STRING })
        assertEquals(ValueKind.STRING, (root.parts[1] as BlockPart.Slot).slot.valueKind)
    }

    @Test
    fun theRootIsTheBlockTheMappingProjects() {
        val root = project()
        assertEquals(call, root.kind)
        assertEquals(TextRange(0, 5), root.range)
        assertEquals(BlockRef(root.id).id, root.id)
    }
}
