package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.BlockId

import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteIntentTest {
    private val block = BlockId("root")

    @Test
    fun withNothingFocusedTheKeyBelongsToTheHost() {
        assertEquals(DeleteIntent.None, deleteIntent(null))
    }

    @Test
    fun aFieldKeepsTheKeyForItsOwnText() {
        assertEquals(DeleteIntent.None, deleteIntent(BlockFocus.Field(block, 1)))
    }

    @Test
    fun anEmptySlotHasNothingToDelete() {
        assertEquals(DeleteIntent.None, deleteIntent(BlockFocus.Slot(block, 0)))
    }

    @Test
    fun aBlockIsDeleted() {
        assertEquals(DeleteIntent.DeleteBlock(block), deleteIntent(BlockFocus.Block(block)))
    }
}
