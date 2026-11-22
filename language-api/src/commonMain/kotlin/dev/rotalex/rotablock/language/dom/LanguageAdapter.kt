package dev.rotalex.rotablock.language.dom

/**
 * The contract a host implements to make one language available to the framework.
 *
 * An adapter is total over malformed input: [parse] returns a [ParsedFile] for any text and marks the
 * regions it cannot parse with [NodeKind.ERROR] instead of throwing, so an editor can always place a
 * caret.
 */
interface LanguageAdapter {
    /** The language this adapter serves. */
    val language: LanguageId

    /** Parses [source] at [version] into the neutral tree, for any input at all. */
    fun parse(source: CharSequence, version: Long): ParsedFile
}
