package dev.rotalex.rotablock.core.testkit

import dev.rotalex.rotablock.core.block.BlockMapping
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The suite every [BlockMapping] must pass before its language is considered supported.
 *
 * A consumer extends this class in its own test source set and supplies the mapping under test. The suite
 * covers what every mapping owes the engine regardless of language: each kind it decomposes declares the
 * category its blocks will carry, so a slot can decide whether it may hold them.
 */
abstract class MappingConformance {
    /** The mapping under test. */
    protected abstract fun mapping(): BlockMapping

    @Test
    fun declaresACategoryForEveryHandledKind() {
        val subject = mapping()
        val missing = subject.handles - subject.categories.keys
        assertTrue(
            missing.isEmpty(),
            "The mapping handles ${missing.map { it.id }} without declaring a category.",
        )
    }
}
