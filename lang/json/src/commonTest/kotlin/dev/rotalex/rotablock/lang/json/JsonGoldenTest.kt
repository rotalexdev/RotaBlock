package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.BlockTemplate
import dev.rotalex.rotablock.core.block.SlotRef
import dev.rotalex.rotablock.core.language.NodeKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonGoldenTest {

    @Test
    fun deletingTheFirstMemberTakesItsTrailingSeparator() {
        val fixture = JsonFixture("{\"a\": 1, \"b\": 2}")

        val result = fixture.apply(BlockEdit.Delete(BlockRef(fixture.elements(fixture.topLevel.single())[0].id)))

        assertEquals("{\"b\": 2}", result)
    }

    @Test
    fun deletingTheLastMemberTakesThePrecedingSeparator() {
        val fixture = JsonFixture("{\"a\": 1, \"b\": 2}")

        val result = fixture.apply(BlockEdit.Delete(BlockRef(fixture.elements(fixture.topLevel.single())[1].id)))

        assertEquals("{\"a\": 1}", result)
    }

    @Test
    fun insertingIntoAnEmptyObjectAddsNoSeparator() {
        val fixture = JsonFixture("{}")

        val result = fixture.apply(insertMember(fixture, index = 0))

        assertEquals("{\"key\": null}", result)
    }

    @Test
    fun insertingAfterAMemberAddsTheSeparator() {
        val fixture = JsonFixture("{\"a\": 1}")

        val result = fixture.apply(insertMember(fixture, index = 1))

        assertEquals("{\"a\": 1, \"key\": null}", result)
    }

    @Test
    fun movingAnElementKeepsBothDisjointEdits() {
        val fixture = JsonFixture("[1, 2]")
        val array = fixture.topLevel.single()
        val values = fixture.elements(array)

        val edits = fixture.compile(BlockEdit.Move(BlockRef(values[0].id), SlotRef(BlockRef(array.id), 0, values.size)))

        assertEquals("[2, 1]", fixture.apply(edits))
        assertEquals(2, edits.size)
    }

    @Test
    fun deletingEveryMemberInTurnNeverLeavesAnInvalidDocument() {
        listOf("{\"a\":1,\"b\":2,\"c\":3}", "{\"a\":1}", "[1,2,3]", "[1]").forEach { text ->
            val probe = JsonFixture(text)
            val container = probe.topLevel.single()
            probe.elements(container).forEachIndexed { index, _ ->
                val fixture = JsonFixture(text)
                val victim = fixture.elements(fixture.topLevel.single())[index]
                assertParsesClean(fixture.apply(BlockEdit.Delete(BlockRef(victim.id))))
            }
        }
    }

    @Test
    fun aDocumentThatIsAnErrorBecomesOneEditableBlockThatRepairsInPlace() {
        val fixture = JsonFixture("???")
        val error = fixture.topLevel.single()

        assertEquals(NodeKind.ERROR, error.kind)

        val repaired = fixture.apply(BlockEdit.SetField(BlockRef(error.id), 0, "{\"a\": 1}"))

        assertEquals("{\"a\": 1}", repaired)
        assertParsesClean(repaired)
    }

    @Test
    fun aStringLiteralCutsItsQuotesIntoChrome() {
        val fixture = JsonFixture("{\"a\": \"x\"}")
        val literal = fixture.elements(fixture.elements(fixture.topLevel.single()).single()).single()

        assertEquals(listOf("\"", "x", "\""), literal.fields.map { it.text })
        assertEquals(listOf(false, true, false), literal.fields.map { it.editable })
    }

    @Test
    fun editingAStringLiteralKeepsItQuoted() {
        val fixture = JsonFixture("{\"a\": \"x\"}")
        val literal = fixture.elements(fixture.elements(fixture.topLevel.single()).single()).single()

        val result = fixture.apply(BlockEdit.SetField(BlockRef(literal.id), 1, "a b"))

        assertEquals("{\"a\": \"a b\"}", result)
        assertParsesClean(result)
    }

    @Test
    fun documentsRoundTripByteForByte() {
        listOf(
            "{\n  \"a\": [1, 2],\n  \"b\": {\"c\": null}\n}\n",
            "{\"a\":[1,2],\"b\":true}",
            "{\"a\":, [1,}",
            "",
        ).forEach { text ->
            val parsed = TreeSitterJson.adapter().parse(text, 1L)

            assertEquals(text, parsed.text())
            assertEquals(text.length, parsed.root.range.length)
        }
    }

    private fun insertMember(fixture: JsonFixture, index: Int): BlockEdit.InsertTemplate =
        BlockEdit.InsertTemplate(
            SlotRef(BlockRef(fixture.topLevel.single().id), 0, index),
            BlockTemplate("member", JsonKinds.PAIR, "\"key\": null", ""),
        )

    private fun assertParsesClean(text: String): BlockNode {
        val reparsed = JsonFixture(text)
        assertTrue(
            !containsError(reparsed.topLevel.single()),
            "The document \"$text\" still contains an error after the edit.",
        )
        return reparsed.topLevel.single()
    }

    private fun containsError(block: BlockNode): Boolean =
        block.kind == NodeKind.ERROR || block.slots.any { slot -> slot.children.any(::containsError) }
}
