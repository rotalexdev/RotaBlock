package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.LanguageId
import dev.rotalex.rotablock.language.dom.NodeKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockMappingExtensionPointTest {
    private fun mapping(language: String) = object : BlockMapping {
        override val languages: Set<LanguageId> = setOf(LanguageId(language))
        override val handles: Set<NodeKind> = emptySet()
    }

    @Test
    fun theBlockMappingPointIsAnExtensionPoint() {
        assertTrue(BLOCK_MAPPING_EP is dev.rotalex.rotablock.language.dom.ExtensionPoint<BlockMapping>)
    }

    @Test
    fun registrationsAreExplicitAndOrdered() {
        val point = dev.rotalex.rotablock.language.dom.ExtensionPoint<BlockMapping>("test.mappings")
        val first = mapping("kotlin")
        val second = mapping("markdown")
        point.register(first)
        point.register(second)
        assertEquals(listOf(first, second), point.extensions())
    }

    @Test
    fun theBlockMappingPointStartsEmpty() {
        assertTrue(BLOCK_MAPPING_EP.extensions().isEmpty())
    }
}
