package dev.rotalex.rotablock.ui.block

import dev.rotalex.rotablock.core.block.BlockSlot
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.block.ValueKind

/**
 * The placeholder an empty slot renders: what the slot accepts, named for a reader.
 *
 * A ghost is never text and never a field, so it can never be mistaken for content or edited.
 *
 * @property category the structural grammar the slot accepts.
 * @property label the short name shown inside the ghost.
 * @property boolean whether the slot accepts a boolean, which draws the ghost as a hexagon.
 */
class SlotGhost(val category: SlotCategory, val label: String, val boolean: Boolean = false)

/** The ghost [slot] renders, or `null` when the slot already holds a block. */
fun ghostOf(slot: BlockSlot): SlotGhost? {
    if (slot.children.isNotEmpty()) return null
    return SlotGhost(
        category = slot.category,
        label = labelOf(slot.category, slot.valueKind),
        boolean = slot.valueKind == ValueKind.BOOLEAN,
    )
}

/**
 * The name a ghost shows for a slot that accepts [category] and is shaped by [valueKind].
 *
 * A slot that promises one shape names that shape; one that accepts anything names its grammar, so the
 * reader always learns what typing there would produce.
 */
fun labelOf(category: SlotCategory, valueKind: ValueKind): String = when (valueKind) {
    ValueKind.OBJECT -> "object"
    ValueKind.ARRAY -> "array"
    ValueKind.STRING -> "text"
    ValueKind.NUMBER -> "number"
    ValueKind.BOOLEAN -> "boolean"
    ValueKind.NULL -> "null"
    ValueKind.TYPE -> "type"
    ValueKind.UNKNOWN -> when (category) {
        SlotCategory.STATEMENT -> "statement"
        SlotCategory.EXPRESSION -> "expression"
        SlotCategory.TYPE -> "type"
        SlotCategory.DECLARATION -> "declaration"
        SlotCategory.NAME -> "name"
        SlotCategory.MODIFIER -> "modifier"
        SlotCategory.PARAMETER -> "parameter"
        SlotCategory.ARGUMENT -> "argument"
        SlotCategory.OPAQUE -> "value"
    }
}
