package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.DomNode
import dev.rotalex.rotablock.language.dom.NodeKind
import dev.rotalex.rotablock.language.dom.TextRange

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockPartTest {
    @Test
    fun aFieldPartCarriesAField() {
        val field = BlockField.chrome("open", "(")
        val part = BlockPart.Field(field)
        assertEquals(field, part.field)
    }

    @Test
    fun aSlotPartCarriesASlot() {
        val slot = BlockSlot(SlotCategory.OPAQUE, emptyList(), false, null, ValueKind.UNKNOWN)
        val part = BlockPart.Slot(slot)
        assertEquals(slot, part.slot)
    }

    @Test
    fun partsAreADistinguishableClosedSet() {
        val parts: List<BlockPart> = listOf(
            BlockPart.Field(BlockField.chrome("open", "(")),
            BlockPart.Slot(BlockSlot(SlotCategory.OPAQUE, emptyList(), false, null, ValueKind.UNKNOWN)),
        )
        assertEquals(1, parts.filterIsInstance<BlockPart.Field>().size)
        assertEquals(1, parts.filterIsInstance<BlockPart.Slot>().size)
        assertTrue(parts[0] is BlockPart.Field)
    }
}
