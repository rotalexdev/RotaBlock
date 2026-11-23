package dev.rotalex.rotablock.parser.treesitter

import dev.rotalex.rotablock.core.language.Document
import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.LanguageAdapter
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.ParsedFile
import dev.rotalex.rotablock.core.language.TextRange
import io.github.treesitter.ktreesitter.InputEncoding
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node as SyntaxNode
import io.github.treesitter.ktreesitter.Parser

/**
 * The adapter that turns any tree-sitter grammar into a neutral tree.
 *
 * It is written once for every grammar: the grammar arrives as data and the adapter applies the same rules
 * to all of them. Named children become nodes and anonymous tokens are dropped, because only the former
 * carry structure; a token the parser had to invent to finish a production is not dropped, because it names
 * the position where the text is incomplete. Kinds are namespaced by [language], so two grammars may use the
 * same type name without colliding, and both a node the parser could not understand and a missing token
 * become [NodeKind.ERROR]. Byte offsets never leave this class: every range is converted to UTF-16 code
 * units through [Document].
 *
 * @param language the language this adapter serves and the namespace of the kinds it produces.
 * @param grammar the tree-sitter grammar to parse with.
 */
class TreeSitterAdapter(
    override val language: LanguageId,
    private val grammar: Language,
) : LanguageAdapter {
    override fun parse(source: CharSequence, version: Long): ParsedFile {
        val document = Document(source, version)
        val parser = Parser(grammar)
        val tree = parser.parse(document.text.toString(), InputEncoding.UTF_8)
        return ParsedFile(document.text, version, convert(tree.rootNode, document, isRoot = true))
    }

    private fun convert(node: SyntaxNode, document: Document, isRoot: Boolean): DomNode {
        val range = TextRange(
            document.charOffset(node.startByte.toInt()),
            document.charOffset(node.endByte.toInt()),
        )
        val children = node.children.mapNotNull { child ->
            when {
                child.isMissing -> {
                    val missing = TextRange(
                        document.charOffset(child.startByte.toInt()),
                        document.charOffset(child.endByte.toInt()),
                    )
                    DomNode(NodeKind.ERROR, missing, document.text)
                }
                child.isNamed -> convert(child, document, isRoot = false)
                else -> null
            }
        }
        return DomNode(kindOf(node, isRoot), range, document.text, children)
    }

    private fun kindOf(node: SyntaxNode, isRoot: Boolean): NodeKind = when {
        node.isError -> NodeKind.ERROR
        isRoot -> NodeKind.ROOT
        else -> NodeKind("${language.id}.${node.type}")
    }
}
