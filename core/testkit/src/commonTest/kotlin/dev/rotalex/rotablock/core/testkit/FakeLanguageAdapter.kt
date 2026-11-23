package dev.rotalex.rotablock.core.testkit

import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.LanguageAdapter
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.ParsedFile
import dev.rotalex.rotablock.core.language.TextRange

internal class FakeLanguageAdapter : LanguageAdapter {
    override val language: LanguageId = LanguageId("fake")

    override fun parse(source: CharSequence, version: Long): ParsedFile {
        val text = source.toString()
        val whole = TextRange(0, text.length)
        val root = if (text.contains(UNPARSABLE)) {
            DomNode(
                NodeKind.ROOT,
                whole,
                text,
                listOf(DomNode(NodeKind.ERROR, whole, text)),
            )
        } else {
            DomNode(NodeKind.ROOT, whole, text, lines(text))
        }
        return ParsedFile(text, version, root)
    }

    private fun lines(text: String): List<DomNode> {
        val nodes = mutableListOf<DomNode>()
        var start = 0
        text.forEachIndexed { index, character ->
            if (character == '\n') {
                nodes.add(DomNode(LINE, TextRange(start, index), text))
                start = index + 1
            }
        }
        if (start < text.length) {
            nodes.add(DomNode(LINE, TextRange(start, text.length), text))
        }
        return nodes
    }

    private companion object {
        val LINE = NodeKind("fake.line")
        const val UNPARSABLE = "?"
    }
}
