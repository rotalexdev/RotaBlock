package dev.rotalex.rotablock.core.language

/**
 * The stable identifier of a language an adapter serves.
 *
 * The id is never blank, so a registered adapter always names a language a host can request.
 */
@JvmInline
value class LanguageId(val id: String) {
    init {
        require(id.isNotBlank()) { "A language id must not be blank." }
    }
}
