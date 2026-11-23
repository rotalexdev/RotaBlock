package dev.rotalex.rotablock.core.testkit

import dev.rotalex.rotablock.core.block.BlockMapping
import dev.rotalex.rotablock.core.block.LayoutPolicy
import dev.rotalex.rotablock.core.block.SlotCategory
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FakeMappingConformanceTest : MappingConformance() {
    override fun mapping(): BlockMapping = FakeMapping

    @Test
    fun failsWhenAHandledKindHasNoCategory() {
        val broken = object : MappingConformance() {
            override fun mapping(): BlockMapping = object : BlockMapping {
                override val languages: Set<LanguageId> = setOf(LanguageId("fake"))
                override val handles: Set<NodeKind> = setOf(FakeMapping.HANDLED)
                override val categories: Map<NodeKind, SlotCategory> = emptyMap()
                override val layout: LayoutPolicy = LayoutPolicy.NONE
            }
        }

        assertFailsWith<AssertionError> { broken.declaresACategoryForEveryHandledKind() }
    }
}

private object FakeMapping : BlockMapping {
    val HANDLED: NodeKind = NodeKind("fake.value")

    override val languages: Set<LanguageId> = setOf(LanguageId("fake"))
    override val handles: Set<NodeKind> = setOf(HANDLED)
    override val categories: Map<NodeKind, SlotCategory> = mapOf(HANDLED to SlotCategory.EXPRESSION)
    override val layout: LayoutPolicy = LayoutPolicy.NONE
}
