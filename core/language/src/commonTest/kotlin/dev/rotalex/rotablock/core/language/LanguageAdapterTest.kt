package dev.rotalex.rotablock.core.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanguageAdapterTest {
    private val adapter = ParenthesisAdapter()

    @Test
    fun exposesItsLanguage() {
        assertEquals(LanguageId("parentheses"), adapter.language)
    }

    @Test
    fun returnsAParsedFileForEmptyInput() {
        val parsed = adapter.parse("", 1L)
        assertEquals("", parsed.text())
        assertEquals(1L, parsed.documentVersion)
    }

    @Test
    fun returnsAParsedFileForMalformedInputInsteadOfThrowing() {
        val parsed = adapter.parse(")((", 3L)
        assertEquals(")((", parsed.text())
        assertEquals(3L, parsed.documentVersion)
    }

    @Test
    fun marksUnparsableRegionsWithTheErrorKind() {
        val parsed = adapter.parse(")", 1L)
        assertEquals(listOf(NodeKind.ERROR), parsed.root.children.map { it.kind })
    }

    @Test
    fun leavesBalancedInputWithoutErrorRegions() {
        assertTrue(adapter.parse("(())", 1L).root.children.isEmpty())
    }

    @Test
    fun coversTheWholeSourceWithItsRoot() {
        val parsed = adapter.parse("(a)", 1L)
        assertEquals(TextRange(0, 3), parsed.root.range)
        assertEquals("(a)", parsed.root.text())
    }
}

private class ParenthesisAdapter : LanguageAdapter {
    override val language: LanguageId = LanguageId("parentheses")

    override fun parse(source: CharSequence, version: Long): ParsedFile {
        val errors = mutableListOf<DomNode>()
        var depth = 0
        source.forEachIndexed { index, char ->
            when (char) {
                '(' -> depth += 1
                ')' -> if (depth == 0) {
                    errors += DomNode(NodeKind.ERROR, TextRange(index, index + 1), source)
                } else {
                    depth -= 1
                }
            }
        }
        val root = DomNode(NodeKind.ROOT, TextRange(0, source.length), source, errors)
        return ParsedFile(source, version, root)
    }
}
