package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.BlockId

import kotlin.test.Test
import kotlin.test.assertEquals

class EnterIntentTest {
    private val block = BlockId("root")

    @Test
    fun withNothingFocusedTheKeyBelongsToTheHost() {
        assertEquals(EnterIntent.None, enterIntent(null))
    }

    @Test
    fun onAFieldItCommitsAndStays() {
        assertEquals(EnterIntent.Commit, enterIntent(BlockFocus.Field(block, 1)))
    }

    @Test
    fun onABlockItAsksForASiblingAfter() {
        assertEquals(EnterIntent.InsertAfter(block), enterIntent(BlockFocus.Block(block)))
    }

    @Test
    fun onAnEmptySlotItAsksForAChild() {
        assertEquals(EnterIntent.InsertInto(block, 2), enterIntent(BlockFocus.Slot(block, 2)))
    }
}
