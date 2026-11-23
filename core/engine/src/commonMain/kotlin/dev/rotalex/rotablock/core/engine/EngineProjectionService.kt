package dev.rotalex.rotablock.core.engine

import dev.rotalex.rotablock.core.block.BLOCK_MAPPING_EP
import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockMapping
import dev.rotalex.rotablock.core.block.BlockProjectionService
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.block.ValueKindOracle
import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.ExtensionPoint
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.ParsedFile

/**
 * The engine as the rest of the framework sees it: projection and compilation over one resolved registry.
 *
 * Both halves read the same registrations, so a tree is compiled with the layout rules of the language it
 * was projected for. A host resolves the registry once per document version and reuses one instance.
 */
class EngineProjectionService(
    mappings: ExtensionPoint<BlockMapping> = BLOCK_MAPPING_EP,
    oracle: ValueKindOracle? = null,
) : BlockProjectionService {
    private val projector = BlockProjector(mappings, oracle)

    private val compiler = EditCompiler(mappings)

    override fun project(parsed: ParsedFile, language: LanguageId): BlockTree =
        projector.project(parsed, language)

    override fun compile(edit: BlockEdit, tree: BlockTree): List<DocumentEdit> =
        compiler.compile(edit, tree)
}
