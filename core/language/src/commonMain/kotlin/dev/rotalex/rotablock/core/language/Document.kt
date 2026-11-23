package dev.rotalex.rotablock.core.language

/**
 * The single owner of a document's text and its only mutator.
 *
 * A document is immutable: [edit] returns a new instance and the previous one stays valid and cheap to
 * retain, which is what makes undo a matter of keeping the earlier version. Ranges are expressed in UTF-16
 * code units, the unit Kotlin's [String] indexes and every consumer surface measures in, while the parser
 * boundary is crossed through [byteOffset], [charOffset] and [utf8].
 *
 * A document carries text and nothing else: no blocks, no parse tree and no selection, all of which are
 * derived or owned by the host.
 *
 * @param text the initial content, copied so later changes to the argument cannot reach this document.
 * @param version the version this content belongs to, never negative.
 */
class Document(text: CharSequence, val version: Long = 0L) {
    private val content: String = text.toString()

    init {
        require(version >= 0L) { "A document version must be non-negative, but was $version." }
    }

    /** The current content, which no caller can mutate. */
    val text: CharSequence get() = content

    /** The number of UTF-16 code units in [text]. */
    val length: Int get() = content.length

    private val lines: LineIndex by lazy { LineIndex(content) }

    private val bytes: Utf8Index by lazy { Utf8Index(content) }

    /**
     * The whole text encoded as UTF-8, computed once per version for the parser.
     *
     * An unpaired surrogate becomes the replacement byte `0x3F`, the same byte the platform encoder emits
     * for it, so these bytes are exactly the ones the parser reads and a byte offset it reports maps back
     * to the character that produced it.
     */
    val utf8: ByteArray by lazy { bytes.data }

    /**
     * Applies [edits] as one atomic batch and returns the next version.
     *
     * The batch is validated in full before any of it is applied, so an invalid batch leaves the document
     * untouched instead of applying a prefix. Edits must be disjoint and are applied against the original
     * offsets, so their position in [edits] does not change the result. Two edits may not share a start when
     * one of them inserts nothing, because their order would be ambiguous.
     *
     * @throws IllegalArgumentException when an edit is out of bounds, overlaps another edit, or shares an
     *   insertion offset with another edit.
     */
    fun edit(edits: List<DocumentEdit>): Document {
        val ordered = edits.sortedBy { it.offset }
        var previousOffset = -1
        var previousEnd = 0
        ordered.forEach { edit ->
            require(edit.offset + edit.oldLength <= length) {
                "An edit must stay inside the document, but ${edit.offset} + ${edit.oldLength} exceeds $length."
            }
            require(edit.offset >= previousEnd) {
                "Edits must be disjoint, but ${edit.offset} overlaps the edit ending at $previousEnd."
            }
            require(previousEnd != previousOffset || edit.offset != previousOffset) {
                "Two insertions at ${edit.offset} have no defined order."
            }
            previousOffset = edit.offset
            previousEnd = edit.offset + edit.oldLength
        }
        val builder = StringBuilder(length)
        var cursor = 0
        ordered.forEach { edit ->
            builder.append(content, cursor, edit.offset)
            builder.append(edit.newText)
            cursor = edit.offset + edit.oldLength
        }
        builder.append(content, cursor, length)
        return Document(builder, version + 1L)
    }

    /**
     * The text covered by [range].
     *
     * @throws IllegalArgumentException when the range ends after [length].
     */
    fun slice(range: TextRange): String {
        require(range.end <= length) {
            "A slice must stay inside the document, but $range ends after $length."
        }
        return content.substring(range.start, range.end)
    }

    /**
     * The zero-based line that contains [offset], where a line is terminated by a line feed.
     *
     * @throws IllegalArgumentException when [offset] is outside `0..length`.
     */
    fun lineOf(offset: Int): Int {
        require(offset in 0..length) {
            "An offset must lie in 0..$length, but was $offset."
        }
        return lines.lineOf(offset)
    }

    /**
     * The offset of [column] in the zero-based [line], where column zero is the line's first character.
     *
     * A column may point at the terminating line feed but not past it.
     *
     * @throws IllegalArgumentException when [line] does not exist or [column] is past the line's end.
     */
    fun offsetOf(line: Int, column: Int): Int {
        require(line in 0 until lines.count) {
            "A line must lie in 0..${lines.count - 1}, but was $line."
        }
        require(column >= 0) { "A column must be non-negative, but was $column." }
        val start = lines.startOf(line)
        val end = lines.endOf(line)
        require(start + column <= end) {
            "A column must stay inside line $line, but $column is past its end."
        }
        return start + column
    }

    /**
     * The UTF-8 byte offset of [charOffset].
     *
     * An offset that falls inside a surrogate pair resolves to the first byte of that character, because
     * the parser addresses whole code points and never a lone surrogate half.
     *
     * @throws IllegalArgumentException when [charOffset] is outside `0..length`.
     */
    fun byteOffset(charOffset: Int): Int {
        require(charOffset in 0..length) {
            "A character offset must lie in 0..$length, but was $charOffset."
        }
        return bytes.byteOf(charOffset)
    }

    /**
     * The UTF-16 offset of [byteOffset].
     *
     * A byte offset that falls inside a multi-byte character resolves to the first code unit of that
     * character, for the same reason as [byteOffset].
     *
     * @throws IllegalArgumentException when [byteOffset] is outside `0..utf8.size`.
     */
    fun charOffset(byteOffset: Int): Int {
        require(byteOffset in 0..bytes.total) {
            "A byte offset must lie in 0..${bytes.total}, but was $byteOffset."
        }
        return bytes.charOf(byteOffset)
    }
}
