package dev.rotalex.rotablock.language.dom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LanguageIdTest {
    @Test
    fun keepsItsStableId() {
        assertEquals("kotlin", LanguageId("kotlin").id)
    }

    @Test
    fun idsWithTheSameValueAreEqual() {
        assertEquals(LanguageId("kotlin"), LanguageId("kotlin"))
    }

    @Test
    fun rejectsBlankId() {
        assertFailsWith<IllegalArgumentException> { LanguageId("") }
    }
}
