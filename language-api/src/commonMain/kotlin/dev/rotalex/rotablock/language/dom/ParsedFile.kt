package dev.rotalex.rotablock.language.dom

/**
 * The result of parsing one document version: the text that was parsed, the version it belongs to and
 * the neutral tree that projects it.
 */
class ParsedFile(
    private val source: CharSequence,
    val documentVersion: Long,
    val root: DomNode,
) {
    /** The whole document text this file was produced from. */
    fun text(): String = source.toString()
}
