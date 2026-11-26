package dev.rotalex.rotablock.shared

import dev.rotalex.rotablock.core.block.BLOCK_MAPPING_EP
import dev.rotalex.rotablock.core.block.BlockProjectionService
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.engine.EngineProjectionService
import dev.rotalex.rotablock.core.language.LanguageAdapter
import dev.rotalex.rotablock.lang.json.JsonBlockMapping
import dev.rotalex.rotablock.lang.json.TreeSitterJson

/**
 * The JSON workspace the application ships with: the grammar, the mapping registered for it and the service
 * that projects one through the other.
 *
 * Registration happens once, when this object is first touched, because an extension point appends and a
 * second registration would make the engine see the same mapping twice.
 */
object JsonWorkspace {
    /** The adapter over the bundled JSON grammar. */
    val adapter: LanguageAdapter = TreeSitterJson.adapter()

    /** The projection service both halves of the engine share. */
    val service: BlockProjectionService = EngineProjectionService()

    /** Projects [text] at [version] into the blocks of the JSON language. */
    fun project(text: String, version: Long = 1L): BlockTree =
        service.project(adapter.parse(text, version), TreeSitterJson.id)

    init {
        BLOCK_MAPPING_EP.register(JsonBlockMapping())
    }
}
