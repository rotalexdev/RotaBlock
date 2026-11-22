package dev.rotalex.rotablock.block.impl

import dev.rotalex.rotablock.block.BLOCK_MAPPING_EP
import dev.rotalex.rotablock.block.BlockMapping
import dev.rotalex.rotablock.block.BlockTree
import dev.rotalex.rotablock.block.ValueKindOracle
import dev.rotalex.rotablock.language.dom.ExtensionPoint
import dev.rotalex.rotablock.language.dom.LanguageId
import dev.rotalex.rotablock.language.dom.ParsedFile

/**
 * Projects a parsed file into a tree of blocks.
 *
 * The first registered mapping that serves the requested language is used; with no such mapping every node
 * still becomes an opaque block, so a language without a mapping is readable rather than a dead end. Two
 * projections of the same source with the same registrations produce the same tree, ids included.
 */
class BlockProjector(
    private val mappings: ExtensionPoint<BlockMapping> = BLOCK_MAPPING_EP,
    private val oracle: ValueKindOracle? = null,
) {
    /** Projects [parsed] for [language], mirroring its document version. */
    fun project(parsed: ParsedFile, language: LanguageId): BlockTree {
        val mapping = mappings.extensions().firstOrNull { language in it.languages }
        val context = EngineProjectionContext(parsed.text(), mapping, oracle)
        return BlockTree(context.projectNode(parsed.root), parsed.documentVersion)
    }
}
