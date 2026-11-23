package dev.rotalex.rotablock.core.language

internal class Utf8Index(private val text: String) {
    private val checkpointChars: IntArray
    private val checkpointBytes: IntArray

    val data: ByteArray

    val total: Int get() = data.size

    init {
        val chars = mutableListOf<Int>()
        val offsets = mutableListOf<Int>()
        var byteCount = 0
        var index = 0
        while (index < text.length) {
            if (index % CHECKPOINT == 0) {
                chars.add(index)
                offsets.add(byteCount)
            }
            byteCount += utf8Size(codePointAt(text, index))
            index += charCountAt(text, index)
        }
        checkpointChars = chars.toIntArray()
        checkpointBytes = offsets.toIntArray()
        data = ByteArray(byteCount)
        var target = 0
        index = 0
        while (index < text.length) {
            target = writeCodePoint(codePointAt(text, index), data, target)
            index += charCountAt(text, index)
        }
    }

    fun byteOf(charOffset: Int): Int {
        if (charOffset <= 0) return 0
        if (charOffset >= text.length) return data.size
        val position = checkpointFor(checkpointChars, charOffset)
        var charIndex = checkpointChars[position]
        var byteIndex = checkpointBytes[position]
        while (charIndex < charOffset) {
            val width = charCountAt(text, charIndex)
            if (charIndex + width > charOffset) return byteIndex
            byteIndex += utf8Size(codePointAt(text, charIndex))
            charIndex += width
        }
        return byteIndex
    }

    fun charOf(byteOffset: Int): Int {
        if (byteOffset <= 0) return 0
        if (byteOffset >= data.size) return text.length
        val position = checkpointFor(checkpointBytes, byteOffset)
        var charIndex = checkpointChars[position]
        var byteIndex = checkpointBytes[position]
        while (charIndex < text.length) {
            val size = utf8Size(codePointAt(text, charIndex))
            if (byteIndex + size > byteOffset) return charIndex
            byteIndex += size
            charIndex += charCountAt(text, charIndex)
        }
        return text.length
    }
}

private const val CHECKPOINT = 64
private const val REPLACEMENT = 0x3F
private const val FIRST_SUPPLEMENTARY = 0x10000

private fun checkpointFor(offsets: IntArray, value: Int): Int {
    var low = 0
    var high = offsets.lastIndex
    while (low < high) {
        val middle = (low + high + 1) / 2
        if (offsets[middle] <= value) low = middle else high = middle - 1
    }
    return low
}

private fun codePointAt(text: String, index: Int): Int {
    val first = text[index]
    if (first.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
        return FIRST_SUPPLEMENTARY +
            ((first.code - 0xD800) shl 10) +
            (text[index + 1].code - 0xDC00)
    }
    return if (first.isSurrogate()) REPLACEMENT else first.code
}

private fun charCountAt(text: String, index: Int): Int =
    if (codePointAt(text, index) >= FIRST_SUPPLEMENTARY) 2 else 1

private fun utf8Size(codePoint: Int): Int = when {
    codePoint < 0x80 -> 1
    codePoint < 0x800 -> 2
    codePoint < FIRST_SUPPLEMENTARY -> 3
    else -> 4
}

private fun writeCodePoint(codePoint: Int, target: ByteArray, offset: Int): Int = when {
    codePoint < 0x80 -> {
        target[offset] = codePoint.toByte()
        offset + 1
    }
    codePoint < 0x800 -> {
        target[offset] = (0xC0 or (codePoint shr 6)).toByte()
        target[offset + 1] = (0x80 or (codePoint and 0x3F)).toByte()
        offset + 2
    }
    codePoint < FIRST_SUPPLEMENTARY -> {
        target[offset] = (0xE0 or (codePoint shr 12)).toByte()
        target[offset + 1] = (0x80 or ((codePoint shr 6) and 0x3F)).toByte()
        target[offset + 2] = (0x80 or (codePoint and 0x3F)).toByte()
        offset + 3
    }
    else -> {
        target[offset] = (0xF0 or (codePoint shr 18)).toByte()
        target[offset + 1] = (0x80 or ((codePoint shr 12) and 0x3F)).toByte()
        target[offset + 2] = (0x80 or ((codePoint shr 6) and 0x3F)).toByte()
        target[offset + 3] = (0x80 or (codePoint and 0x3F)).toByte()
        offset + 4
    }
}
