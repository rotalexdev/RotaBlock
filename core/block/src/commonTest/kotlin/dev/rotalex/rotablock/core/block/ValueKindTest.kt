package dev.rotalex.rotablock.core.block

import kotlin.test.Test
import kotlin.test.assertEquals

class ValueKindTest {
    @Test
    fun theValueKindSetIsClosed() {
        assertEquals(
            listOf("BOOLEAN", "NUMBER", "STRING", "OBJECT", "ARRAY", "TYPE", "NULL", "UNKNOWN"),
            ValueKind.entries.map { it.name },
        )
    }
}
