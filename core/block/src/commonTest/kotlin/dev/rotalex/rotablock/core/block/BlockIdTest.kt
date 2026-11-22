package dev.rotalex.rotablock.core.block

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class BlockIdTest {
    @Test
    fun keepsItsValue() {
        assertEquals("b1", BlockId("b1").value)
    }

    @Test
    fun idsWithTheSameValueAreEqual() {
        assertEquals(BlockId("b1"), BlockId("b1"))
        assertNotEquals(BlockId("b1"), BlockId("b2"))
    }

    @Test
    fun rejectsBlankValue() {
        assertFailsWith<IllegalArgumentException> { BlockId(" ") }
    }
}
