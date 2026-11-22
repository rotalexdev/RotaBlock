package dev.rotalex.rotablock.language.dom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ParsedFileTest {
    private val source = "f(1)"
    private val root = DomNode(NodeKind.ROOT, TextRange(0, source.length), source)

    @Test
    fun textReturnsTheWholeSource() {
        assertEquals("f(1)", ParsedFile(source, 1L, root).text())
    }

    @Test
    fun exposesTheDocumentVersion() {
        assertEquals(7L, ParsedFile(source, 7L, root).documentVersion)
    }

    @Test
    fun exposesItsRoot() {
        assertSame(root, ParsedFile(source, 1L, root).root)
    }

    @Test
    fun parsesAnEmptyDocument() {
        val empty = ""
        val emptyRoot = DomNode(NodeKind.ROOT, TextRange(0, 0), empty)
        assertEquals("", ParsedFile(empty, 2L, emptyRoot).text())
    }
}
