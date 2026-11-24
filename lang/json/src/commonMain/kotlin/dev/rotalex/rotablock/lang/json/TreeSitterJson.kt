package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.language.LanguageAdapter
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.parser.treesitter.TreeSitterAdapter
import io.github.treesitter.ktreesitter.Language

/**
 * The JSON grammar: the native library that carries it and the adapter that projects it.
 *
 * The grammar's C sources live in the `tree-sitter-json` submodule and are compiled unmodified into
 * `libktreesitter-json` by the JNI build plugin; this object only names the language and hands the loaded
 * [Language] to the shared tree-sitter adapter.
 */
object TreeSitterJson {
    /** The language id every kind and mapping of this module namespaces itself with. */
    val id: LanguageId = LanguageId("json")

    private val grammar: Language by lazy { Language(jsonLanguage()) }

    /** The loaded grammar, shared by every parser this process creates. */
    fun language(): Language = grammar

    /** An adapter over [language], producing neutral kinds namespaced by [id]. */
    fun adapter(): LanguageAdapter = TreeSitterAdapter(id, language())
}
