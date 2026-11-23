package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.ParsedFile

/**
 * The engine as the rest of the framework sees it: a document becomes a tree, and an intent becomes text.
 *
 * The implementation lives in the engine module. Two rules belong to it and to nothing else: an edit
 * compiles to the minimum number of disjoint document edits, leaving everything outside their ranges
 * byte-identical, and an edit whose references no longer resolve compiles to an empty list instead of
 * throwing, so a caller re-projects and retries from a fresh tree.
 */
interface BlockProjectionService {
    /** Projects [parsed] into a tree of blocks for [language]. */
    fun project(parsed: ParsedFile, language: LanguageId): BlockTree

    /** Compiles [edit] against [tree], the tree it was derived from. */
    fun compile(edit: BlockEdit, tree: BlockTree): List<DocumentEdit>
}
