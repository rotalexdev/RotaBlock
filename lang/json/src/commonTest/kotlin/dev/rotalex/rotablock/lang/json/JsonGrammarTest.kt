package dev.rotalex.rotablock.lang.json

import io.github.treesitter.ktreesitter.InputEncoding
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Parser
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonGrammarTest {

    @Test
    fun parsesAnObject() {
        val parser = Parser(Language(jsonLanguage()))

        val tree = parser.parse("""{"a": 1}""", InputEncoding.UTF_8)

        assertEquals("document", tree.rootNode.type)
    }

    @Test
    fun parsesAnEmptyArray() {
        val parser = Parser(Language(jsonLanguage()))

        val tree = parser.parse("[]", InputEncoding.UTF_8)

        assertEquals("document", tree.rootNode.type)
        assertEquals(1, tree.rootNode.namedChildren.count())
    }
}
