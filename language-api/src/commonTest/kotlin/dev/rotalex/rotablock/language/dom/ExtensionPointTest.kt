package dev.rotalex.rotablock.language.dom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExtensionPointTest {
    @Test
    fun startsWithoutExtensions() {
        assertTrue(ExtensionPoint<String>("mapping").extensions().isEmpty())
    }

    @Test
    fun keepsTheRegistrationOrder() {
        val point = ExtensionPoint<String>("mapping")
        point.register("first")
        point.register("second")
        assertEquals(listOf("first", "second"), point.extensions())
    }

    @Test
    fun exposesItsId() {
        assertEquals("block-mapping", ExtensionPoint<String>("block-mapping").id)
    }

    @Test
    fun rejectsBlankId() {
        assertFailsWith<IllegalArgumentException> { ExtensionPoint<String>("  ") }
    }

    @Test
    fun pointsAreIndependent() {
        val first = ExtensionPoint<String>("first")
        val second = ExtensionPoint<String>("second")
        first.register("value")
        assertTrue(second.extensions().isEmpty())
    }

    @Test
    fun anExtensionSnapshotDoesNotChangeAfterLaterRegistrations() {
        val point = ExtensionPoint<String>("mapping")
        point.register("first")
        val snapshot = point.extensions()
        point.register("second")
        assertEquals(listOf("first"), snapshot)
        assertEquals(listOf("first", "second"), point.extensions())
    }
}
