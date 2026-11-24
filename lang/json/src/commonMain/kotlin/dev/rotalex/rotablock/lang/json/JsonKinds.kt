package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.language.NodeKind

/** The neutral kinds this module produces, each carrying the grammar's own type name. */
internal object JsonKinds {
    val OBJECT: NodeKind = NodeKind("json.object")
    val ARRAY: NodeKind = NodeKind("json.array")
    val PAIR: NodeKind = NodeKind("json.pair")
    val STRING: NodeKind = NodeKind("json.string")
    val NUMBER: NodeKind = NodeKind("json.number")
    val TRUE: NodeKind = NodeKind("json.true")
    val FALSE: NodeKind = NodeKind("json.false")
    val NULL: NodeKind = NodeKind("json.null")

    val literals: Set<NodeKind> = setOf(STRING, NUMBER, TRUE, FALSE, NULL)

    val handled: Set<NodeKind> = literals + setOf(NodeKind.ROOT, OBJECT, ARRAY, PAIR)
}
