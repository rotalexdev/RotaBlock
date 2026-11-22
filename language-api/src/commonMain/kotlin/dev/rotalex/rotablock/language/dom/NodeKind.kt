package dev.rotalex.rotablock.language.dom

/**
 * The stable identifier of a node's structural kind.
 *
 * The neutral kinds are the constants on the companion; a language adapter builds its own kinds from its
 * naming. Ids are never blank and are comparable, so any collection of kinds has a deterministic order.
 */
@JvmInline
value class NodeKind(val id: String) : Comparable<NodeKind> {
    init {
        require(id.isNotBlank()) { "A node kind id must not be blank." }
    }

    override fun compareTo(other: NodeKind): Int = id.compareTo(other.id)

    companion object {
        /** The kind of the single root node of a parsed document. */
        val ROOT: NodeKind = NodeKind("root")

        /** The kind of a region the adapter could not parse. */
        val ERROR: NodeKind = NodeKind("error")
    }
}
