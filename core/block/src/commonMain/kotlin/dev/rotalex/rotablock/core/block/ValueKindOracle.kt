package dev.rotalex.rotablock.core.block

/**
 * A host-supplied refinement of the shape a block is rendered with.
 *
 * The engine consults the oracle first: a non-null answer replaces the kind the projection produced, and a
 * null answer leaves the syntactic heuristic in charge. An oracle is a pure function of the block it sees,
 * so wiring a semantic one never changes projection determinism.
 */
fun interface ValueKindOracle {
    /** The kind this oracle claims for [block], or `null` to defer to the syntactic heuristic. */
    fun valueKindOf(block: BlockNode): ValueKind?
}
