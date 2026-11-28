package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.block.BlockNode

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTemplateTest {
    @Test
    fun anObjectOffersTheMemberTemplate() {
        val objectBlock = JsonFixture("""{"a": 1}""").topLevel.single()
        val templates = objectBlock.slot().templates
        assertEquals(listOf("\"key\": null"), templates.map { it.insertionText })
        assertEquals(listOf(JsonKinds.PAIR), templates.map { it.kind })
    }

    @Test
    fun anEmptyObjectOffersTheMemberTemplate() {
        val objectBlock = JsonFixture("{}").topLevel.single()
        assertEquals(listOf("\"key\": null"), objectBlock.slot().templates.map { it.insertionText })
    }

    @Test
    fun anArrayOffersTheElementTemplate() {
        val arrayBlock = JsonFixture("""[1]""").topLevel.single()
        val templates = arrayBlock.slot().templates
        assertEquals(listOf("null"), templates.map { it.insertionText })
        assertEquals(listOf(JsonKinds.NULL), templates.map { it.kind })
    }

    @Test
    fun theRootOffersTheValueTemplate() {
        val root = JsonFixture("""{}""").tree.root
        assertEquals(listOf("null"), root.slot().templates.map { it.insertionText })
    }

    @Test
    fun aSingularSlotOffersNothing() {
        val member = JsonFixture("""{"a": 1}""").topLevel.single().slot().children.single()
        assertEquals(emptyList(), member.slot().templates)
    }
}

private fun BlockNode.slot() = slots.single()
