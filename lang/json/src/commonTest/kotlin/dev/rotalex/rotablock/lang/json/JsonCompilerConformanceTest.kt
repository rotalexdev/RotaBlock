package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockProjectionService
import dev.rotalex.rotablock.core.block.BlockRef
import dev.rotalex.rotablock.core.block.BlockTemplate
import dev.rotalex.rotablock.core.block.SlotRef
import dev.rotalex.rotablock.core.testkit.EditCompilerConformance

class JsonCompilerConformanceTest : EditCompilerConformance() {
    private val fixture = JsonFixture("{\"a\": 1, \"b\": [2, 3]}")

    override fun service(): BlockProjectionService = fixture.service

    override fun scenario(): EditScenario {
        val obj = fixture.topLevel.single()
        val pairs = fixture.elements(obj)
        val array = fixture.elements(pairs[1]).single()
        val values = fixture.elements(array)
        return EditScenario(
            text = fixture.text,
            tree = fixture.tree,
            edits = listOf(
                BlockEdit.Delete(BlockRef(pairs[0].id)),
                BlockEdit.InsertTemplate(
                    SlotRef(BlockRef(obj.id), 0, pairs.size),
                    BlockTemplate("member", JsonKinds.PAIR, "\"key\": null", ""),
                ),
                BlockEdit.SetField(BlockRef(pairs[0].id), 1, "renamed"),
                BlockEdit.ReplaceWithText(SlotRef(BlockRef(obj.id), 0, 0), "{\"z\": 0}"),
                BlockEdit.Move(BlockRef(values[0].id), SlotRef(BlockRef(array.id), 0, values.size)),
                BlockEdit.Wrap(BlockRef(values[1].id), BlockTemplate("wrap", JsonKinds.ARRAY, "[", "]")),
            ),
        )
    }
}
