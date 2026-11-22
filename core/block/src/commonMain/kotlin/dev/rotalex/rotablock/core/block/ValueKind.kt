package dev.rotalex.rotablock.core.block

/**
 * The shape of a value, which shapes the socket a block is rendered with.
 *
 * This is a rendering affordance and stays orthogonal to [SlotCategory], which is structural: the two are
 * never merged. The set is closed and [UNKNOWN] says nothing is claimed.
 */
enum class ValueKind {
    BOOLEAN,
    NUMBER,
    STRING,
    OBJECT,
    TYPE,
    UNKNOWN,
}
