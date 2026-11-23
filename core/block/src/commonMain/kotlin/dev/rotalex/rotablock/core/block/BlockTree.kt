package dev.rotalex.rotablock.core.block

/**
 * One projection of one document version: the root block, the version it mirrors and the lookups over it.
 *
 * Construction rejects duplicate [BlockId] values, so "unique within a projection" holds for every tree
 * that exists. Lookups never throw for an unknown id or an offset outside the root.
 */
class BlockTree(val root: BlockNode, val version: Long) {
    private val blocksById: Map<BlockId, BlockNode> = indexOf(root)

    /** The block with [id], or `null` when this tree does not contain it. */
    fun block(id: BlockId): BlockNode? = blocksById[id]

    /** The deepest block whose range contains [offset], or `null` when the offset lies outside the root. */
    fun blockAt(offset: Int): BlockNode? {
        if (!root.range.contains(offset)) return null
        var current = root
        while (true) {
            val next = current.slots
                .asSequence()
                .flatMap { it.children.asSequence() }
                .firstOrNull { it.range.contains(offset) }
                ?: return current
            current = next
        }
    }

    private companion object {
        fun indexOf(root: BlockNode): Map<BlockId, BlockNode> {
            val index = mutableMapOf<BlockId, BlockNode>()
            fun visit(node: BlockNode) {
                require(index.put(node.id, node) == null) { "Duplicate block id ${node.id.value}." }
                node.slots.forEach { slot -> slot.children.forEach(::visit) }
            }
            visit(root)
            return index
        }
    }
}
