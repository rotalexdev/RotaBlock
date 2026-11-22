package dev.rotalex.rotablock.language.dom

/**
 * A half-open span over a document, written `[start, end)`.
 *
 * A range is never negative and its end never precedes its start, so an instance always describes a real
 * span. An empty range has the same start and end and contains no offset.
 */
data class TextRange(val start: Int, val end: Int) {
    init {
        require(start >= 0) { "A range start must be non-negative, but was $start." }
        require(end >= start) { "A range end must not precede its start, but $end is before $start." }
    }

    /** The number of characters the span covers. */
    val length: Int get() = end - start

    /** Whether the span covers no character at all. */
    val isEmpty: Boolean get() = start == end

    /** Whether [offset] lies inside this span, so [start] is included and [end] is excluded. */
    fun contains(offset: Int): Boolean = offset >= start && offset < end
}
