package dev.rotalex.rotablock.core.testkit

import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockProjectionService
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.language.DocumentEdit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What every implementation of [BlockProjectionService] must satisfy when it compiles an intent.
 *
 * The suite is abstract because the language is the consumer's: it asserts the properties of §7.3 that hold
 * for every language, never the text a particular policy produces. A consumer provides the service and one
 * scenario whose edits cover the variants it supports.
 */
abstract class EditCompilerConformance {
    /** The service under test. */
    protected abstract fun service(): BlockProjectionService

    /** One projected document and representative intents against it. */
    protected abstract fun scenario(): EditScenario

    /**
     * A projection to compile against, with the text it was projected from.
     *
     * @property text the exact document text [tree] was projected from.
     * @property tree the tree the edits are compiled against.
     * @property edits one intent per variant the language supports.
     */
    class EditScenario(val text: String, val tree: BlockTree, val edits: List<BlockEdit>)

    @Test
    fun everyCompiledListDescendsByOffset() {
        val fixture = scenario()
        fixture.edits.forEach { edit ->
            val offsets = service().compile(edit, fixture.tree).map { it.offset }
            assertEquals(offsets.sortedDescending(), offsets, "An edit list must descend: $edit")
        }
    }

    @Test
    fun everyCompiledListIsDisjoint() {
        val fixture = scenario()
        fixture.edits.forEach { edit ->
            val compiled = service().compile(edit, fixture.tree)
            compiled.zipWithNext().forEach { (higher, lower) ->
                assertTrue(
                    lower.offset + lower.oldLength <= higher.offset,
                    "Spans must not overlap: $lower and $higher",
                )
            }
        }
    }

    @Test
    fun everyCompiledListStaysInsideTheDocument() {
        val fixture = scenario()
        fixture.edits.forEach { edit ->
            service().compile(edit, fixture.tree).forEach { documentEdit ->
                assertTrue(
                    documentEdit.offset + documentEdit.oldLength <= fixture.text.length,
                    "An edit must stay inside the document: $documentEdit",
                )
            }
        }
    }

    @Test
    fun textOutsideTheEditedRangesIsUntouched() {
        val fixture = scenario()
        fixture.edits.forEach { edit ->
            val compiled = service().compile(edit, fixture.tree)
            if (compiled.isEmpty()) return@forEach
            val start = compiled.minOf { it.offset }
            val end = compiled.maxOf { it.offset + it.oldLength }
            val applied = apply(fixture.text, compiled)
            val suffix = fixture.text.length - end
            assertEquals(fixture.text.substring(0, start), applied.substring(0, start))
            assertEquals(fixture.text.substring(end), applied.substring(applied.length - suffix))
        }
    }

    @Test
    fun compilingTheSameIntentTwiceYieldsTheSameList() {
        val fixture = scenario()
        fixture.edits.forEach { edit ->
            assertEquals(service().compile(edit, fixture.tree), service().compile(edit, fixture.tree))
        }
    }

    @Test
    fun aReferenceThatNoLongerResolvesYieldsNoEdit() {
        val fixture = scenario()
        val stale = BlockEdit.Delete(BlockRef(BlockId("absent")))
        assertTrue(service().compile(stale, fixture.tree).isEmpty())
    }

    private fun apply(text: String, edits: List<DocumentEdit>): String {
        val builder = StringBuilder(text)
        edits.forEach { edit -> builder.replace(edit.offset, edit.offset + edit.oldLength, edit.newText) }
        return builder.toString()
    }
}
