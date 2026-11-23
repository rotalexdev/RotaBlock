package dev.rotalex.rotablock.core.language

internal class LineIndex(text: String) {
    private val starts: IntArray = buildList {
        add(0)
        text.forEachIndexed { index, character ->
            if (character == '\n') add(index + 1)
        }
    }.toIntArray()

    private val length: Int = text.length

    val count: Int get() = starts.size

    fun startOf(line: Int): Int = starts[line]

    fun endOf(line: Int): Int =
        if (line + 1 < starts.size) starts[line + 1] - 1 else length

    fun lineOf(offset: Int): Int {
        var low = 0
        var high = starts.lastIndex
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (starts[middle] <= offset) low = middle else high = middle - 1
        }
        return low
    }
}
