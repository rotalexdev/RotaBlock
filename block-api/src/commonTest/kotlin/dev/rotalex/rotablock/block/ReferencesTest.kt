package dev.rotalex.rotablock.block

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReferencesTest {
    @Test
    fun aBlockReferenceNamesItsBlock() {
        assertEquals(BlockId("b1"), BlockRef(BlockId("b1")).id)
    }

    @Test
    fun aSlotReferenceNamesOwnerSlotAndIndex() {
        val reference = SlotRef(BlockRef(BlockId("owner")), slotIndex = 2, index = 3)
        assertEquals(BlockRef(BlockId("owner")), reference.owner)
        assertEquals(2, reference.slotIndex)
        assertEquals(3, reference.index)
    }

    @Test
    fun aSlotReferenceAcceptsTheAppendPosition() {
        val children = listOf(BlockId("a"), BlockId("b"))
        assertEquals(children.size, SlotRef(BlockRef(BlockId("owner")), 0, children.size).index)
    }

    @Test
    fun aSlotReferenceRejectsANegativeSlotIndex() {
        assertFailsWith<IllegalArgumentException> { SlotRef(BlockRef(BlockId("owner")), -1, 0) }
    }

    @Test
    fun aSlotReferenceRejectsANegativeIndex() {
        assertFailsWith<IllegalArgumentException> { SlotRef(BlockRef(BlockId("owner")), 0, -1) }
    }
}
