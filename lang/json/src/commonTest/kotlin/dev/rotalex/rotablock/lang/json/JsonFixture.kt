package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockMapping
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockProjectionService
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.engine.EngineProjectionService
import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.ExtensionPoint
import dev.rotalex.rotablock.core.language.ParsedFile

/** One JSON document projected through the real adapter, mapping and engine. */
internal class JsonFixture(val text: String) {
    val service: BlockProjectionService = EngineProjectionService(registrations())
    val mapping: BlockMapping = JsonBlockMapping()
    val parsed: ParsedFile = TreeSitterJson.adapter().parse(text, VERSION)
    val tree: BlockTree = service.project(parsed, TreeSitterJson.id)

    val topLevel: List<BlockNode> get() = tree.root.slots.flatMap { it.children }

    fun elements(container: BlockNode): List<BlockNode> = container.slots.flatMap { it.children }

    fun compile(edit: BlockEdit): List<DocumentEdit> = service.compile(edit, tree)

    fun apply(edit: BlockEdit): String = apply(compile(edit))

    fun apply(edits: List<DocumentEdit>): String {
        val builder = StringBuilder(text)
        edits.forEach { edit -> builder.replace(edit.offset, edit.offset + edit.oldLength, edit.newText) }
        return builder.toString()
    }

    private companion object {
        const val VERSION = 1L

        fun registrations(): ExtensionPoint<BlockMapping> =
            ExtensionPoint<BlockMapping>("test.json.mappings").apply { register(JsonBlockMapping()) }
    }
}
