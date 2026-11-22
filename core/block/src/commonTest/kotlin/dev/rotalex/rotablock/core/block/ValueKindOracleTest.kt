package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValueKindOracleTest {
    private val block = BlockNode(BlockId("b"), NodeKind("Literal"), null, TextRange(0, 2), emptyList())

    @Test
    fun anOracleMayClaimAKind() {
        val oracle = ValueKindOracle { ValueKind.STRING }
        assertEquals(ValueKind.STRING, oracle.valueKindOf(block))
    }

    @Test
    fun aNullAnswerFallsBackToTheHeuristic() {
        val oracle = ValueKindOracle { null }
        assertNull(oracle.valueKindOf(block))
    }

    @Test
    fun anOracleSeesTheBlockItRefines() {
        val seen = mutableListOf<NodeKind>()
        val oracle = ValueKindOracle { inspected -> seen += inspected.kind; ValueKind.BOOLEAN }
        oracle.valueKindOf(block)
        assertEquals(listOf(NodeKind("Literal")), seen)
    }
}
