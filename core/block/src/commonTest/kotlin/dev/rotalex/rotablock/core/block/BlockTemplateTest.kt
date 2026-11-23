package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.NodeKind

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BlockTemplateTest {
    private val kind = NodeKind("IfStatement")

    @Test
    fun keepsIdKindAndSyntax() {
        val template = BlockTemplate("if", kind, "if (", ") {}")
        assertEquals("if", template.id)
        assertEquals(kind, template.kind)
        assertEquals("if (", template.prefix)
        assertEquals(") {}", template.suffix)
    }

    @Test
    fun rejectsBlankId() {
        assertFailsWith<IllegalArgumentException> { BlockTemplate(" ", kind, "", "") }
    }

    @Test
    fun insertionTextIsBothSidesTogether() {
        assertEquals("for (;;) {}", BlockTemplate("for", kind, "for (;;) {", "}").insertionText)
    }

    @Test
    fun wrappingSplicesTheContentBetweenBothSides() {
        assertEquals("if (a > 0) {}", BlockTemplate("if", kind, "if (", ") {}").wrapped("a > 0"))
    }

    @Test
    fun anEmptyTemplateChangesNothing() {
        val template = BlockTemplate("none", kind, "", "")
        assertEquals("", template.insertionText)
        assertEquals("a > 0", template.wrapped("a > 0"))
    }
}
