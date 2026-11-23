package dev.rotalex.rotablock.core.language

/**
 * One node of the neutral tree.
 *
 * A node always carries the [range] it covers in the source it was parsed from, and its [children] appear
 * in source order. The parent link is assigned by the node that owns a child, so a node nobody owns has
 * no parent.
 */
class DomNode(
    val kind: NodeKind,
    val range: TextRange,
    private val source: CharSequence,
    children: List<DomNode> = emptyList(),
) {
    /** The children of this node, in source order. */
    val children: List<DomNode> = children.toList()

    /** The node that owns this one, or `null` when this node is a root or is detached. */
    var parent: DomNode? = null
        private set

    init {
        require(range.end <= source.length) {
            "A node range must stay inside its source, but $range ends after ${source.length} characters."
        }
        this.children.forEach { child ->
            require(child.range.start >= range.start && child.range.end <= range.end) {
                "A child range must stay inside its parent, but ${child.range} is not within $range."
            }
            child.parent = this
        }
    }

    /** The exact source slice this node covers. */
    fun text(): String = source.subSequence(range.start, range.end).toString()
}
