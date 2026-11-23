package dev.rotalex.rotablock.core.block

/**
 * The structural grammar of a slot: what it is allowed to hold.
 *
 * The set is closed. [accepts] is the whole answer to "may this go here?"; [OPAQUE] accepts every
 * category and every other category accepts only itself.
 */
enum class SlotCategory {
    STATEMENT,
    EXPRESSION,
    TYPE,
    DECLARATION,
    NAME,
    MODIFIER,
    PARAMETER,
    ARGUMENT,
    OPAQUE,
    ;

    /** Whether this slot may hold a block of [other]. */
    fun accepts(other: SlotCategory): Boolean = this == OPAQUE || this == other
}
