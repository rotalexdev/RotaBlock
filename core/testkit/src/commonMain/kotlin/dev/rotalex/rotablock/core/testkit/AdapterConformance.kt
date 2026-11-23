package dev.rotalex.rotablock.core.testkit

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.LanguageAdapter
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.ParsedFile
import dev.rotalex.rotablock.core.language.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The suite every [LanguageAdapter] must pass before its language is considered supported.
 *
 * A consumer extends this class in its own test source set and supplies the adapter under test. The suite
 * covers what every adapter owes the rest of the framework regardless of grammar: it is total over any
 * text, it reports ranges inside the document in UTF-16 code units, it keeps children ordered and
 * disjoint, it is deterministic, and it marks what it cannot parse with [NodeKind.ERROR].
 */
abstract class AdapterConformance {
    /** The adapter under test, created fresh for each test. */
    protected abstract fun adapter(): LanguageAdapter

    /** Texts the adapter must parse without failing, in addition to the defaults. */
    protected open fun samples(): List<String> = listOf("", "a", "a\nb", "aé€\uD83D\uDE00z", "\n\n")

    /** Texts the adapter cannot fully parse and must therefore mark with [NodeKind.ERROR]. */
    protected open fun malformedSamples(): List<String> = listOf("???")

    @Test
    fun parsesEverySampleAtTheRequestedVersion() {
        samples().forEach { sample ->
            val file = adapter().parse(sample, VERSION)
            assertEquals(VERSION, file.documentVersion)
            assertEquals(sample, file.text())
            assertEquals(TextRange(0, sample.length), file.root.range)
            assertEquals(NodeKind.ROOT, file.root.kind)
        }
    }

    @Test
    fun keepsEveryNodeRangeInsideTheDocument() {
        everyText().forEach { sample ->
            walk(adapter().parse(sample, VERSION).root).forEach { node ->
                assertTrue(
                    node.range.start >= 0 && node.range.end <= sample.length,
                    "Node ${node.kind.id} has ${node.range} outside a document of ${sample.length} characters.",
                )
                assertEquals(sample.substring(node.range.start, node.range.end), node.text())
            }
        }
    }

    @Test
    fun ordersChildrenWithoutOverlapAndInsideTheirParent() {
        everyText().forEach { sample ->
            walk(adapter().parse(sample, VERSION).root).forEach { node ->
                var previousEnd = node.range.start
                node.children.forEach { child ->
                    assertTrue(
                        child.range.start >= previousEnd,
                        "Children of ${node.kind.id} overlap at ${child.range}.",
                    )
                    assertTrue(
                        child.range.end <= node.range.end,
                        "Child ${child.kind.id} ends outside its parent ${node.kind.id}.",
                    )
                    previousEnd = child.range.end
                }
            }
        }
    }

    @Test
    fun keepsUtf16RangesAcrossAstralCharacters() {
        val sample = "a\uD83D\uDE00b"
        val file = adapter().parse(sample, VERSION)
        assertEquals(4, file.root.range.length)
        walk(file.root).forEach { node ->
            assertEquals(sample.substring(node.range.start, node.range.end), node.text())
        }
    }

    @Test
    fun parsesTheSameTextTheSameWayEveryTime() {
        everyText().forEach { sample ->
            val first = dump(adapter().parse(sample, VERSION).root)
            val second = dump(adapter().parse(sample, VERSION).root)
            assertEquals(first, second)
        }
    }

    @Test
    fun marksWhatItCannotParseAsAnError() {
        malformedSamples().forEach { sample ->
            val file = adapter().parse(sample, VERSION)
            assertEquals(sample, file.text())
            assertTrue(
                walk(file.root).any { it.kind == NodeKind.ERROR },
                "Malformed text \"$sample\" produced no ${NodeKind.ERROR.id} node.",
            )
        }
    }

    private fun everyText(): List<String> = samples() + malformedSamples()

    private fun walk(node: DomNode): List<DomNode> = buildList {
        add(node)
        node.children.forEach { addAll(walk(it)) }
    }

    private fun dump(node: DomNode): String =
        "${node.kind.id}${node.range.start}-${node.range.end}(${node.children.joinToString(",") { dump(it) }})"

    private companion object {
        const val VERSION = 7L
    }
}
