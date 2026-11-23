package dev.rotalex.rotablock.core.language

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DocumentTest {
    @Test
    fun copiesItsTextAndStartsAtVersionZero() {
        val source = StringBuilder("abc")
        val document = Document(source)
        source.append("def")
        assertEquals("abc", document.text.toString())
        assertEquals(3, document.length)
        assertEquals(0L, document.version)
    }

    @Test
    fun rejectsNegativeVersion() {
        assertFailsWith<IllegalArgumentException> { Document("abc", -1L) }
    }

    @Test
    fun lengthCountsUtf16CodeUnits() {
        assertEquals(3, Document("a\uD83D\uDE00").length)
        assertEquals(1, Document("é").length)
    }

    @Test
    fun editReturnsTheNextVersionAndLeavesThePreviousOneIntact() {
        val document = Document("abc")
        val edited = document.edit(listOf(DocumentEdit(1, 1, "XY")))
        assertEquals("aXYc", edited.text.toString())
        assertEquals(1L, edited.version)
        assertEquals("abc", document.text.toString())
        assertEquals(0L, document.version)
    }

    @Test
    fun editAppliesSeveralDisjointEditsInAscendingOrder() {
        val edited = Document("abcdef").edit(
            listOf(
                DocumentEdit(4, 2, "Z"),
                DocumentEdit(0, 1, "A"),
            ),
        )
        assertEquals("AbcdZ", edited.text.toString())
    }

    @Test
    fun editAppliesAnInsertionAndADeletionThatTouch() {
        val edited = Document("abc").edit(
            listOf(
                DocumentEdit(1, 2, ""),
                DocumentEdit(3, 0, "!"),
            ),
        )
        assertEquals("a!", edited.text.toString())
    }

    @Test
    fun editRejectsAnOutOfBoundsBatchWithoutApplyingIt() {
        val document = Document("abc")
        assertFailsWith<IllegalArgumentException> {
            document.edit(
                listOf(
                    DocumentEdit(0, 1, "X"),
                    DocumentEdit(2, 5, "Y"),
                ),
            )
        }
        assertEquals("abc", document.text.toString())
    }

    @Test
    fun editRejectsOverlappingEdits() {
        assertFailsWith<IllegalArgumentException> {
            Document("abcdef").edit(
                listOf(
                    DocumentEdit(0, 3, "X"),
                    DocumentEdit(2, 2, "Y"),
                ),
            )
        }
    }

    @Test
    fun editRejectsTwoInsertionsAtTheSameOffset() {
        assertFailsWith<IllegalArgumentException> {
            Document("abc").edit(
                listOf(
                    DocumentEdit(1, 0, "X"),
                    DocumentEdit(1, 0, "Y"),
                ),
            )
        }
    }

    @Test
    fun emptyBatchStillAdvancesTheVersion() {
        val edited = Document("abc").edit(emptyList())
        assertEquals("abc", edited.text.toString())
        assertEquals(1L, edited.version)
    }

    @Test
    fun sliceReturnsTheCoveredText() {
        assertEquals("bcd", Document("abcde").slice(TextRange(1, 4)))
        assertEquals("", Document("abcde").slice(TextRange(5, 5)))
    }

    @Test
    fun sliceRejectsARangePastTheEnd() {
        assertFailsWith<IllegalArgumentException> { Document("abc").slice(TextRange(1, 4)) }
    }

    @Test
    fun lineOfCountsLineFeedsBeforeTheOffset() {
        val document = Document("ab\ncd\n")
        assertEquals(0, document.lineOf(0))
        assertEquals(0, document.lineOf(2))
        assertEquals(1, document.lineOf(3))
        assertEquals(1, document.lineOf(5))
        assertEquals(2, document.lineOf(6))
    }

    @Test
    fun lineOfRejectsAnOffsetOutsideTheDocument() {
        assertFailsWith<IllegalArgumentException> { Document("abc").lineOf(4) }
        assertFailsWith<IllegalArgumentException> { Document("abc").lineOf(-1) }
    }

    @Test
    fun offsetOfCombinesLineStartAndColumn() {
        val document = Document("ab\ncd\n")
        assertEquals(0, document.offsetOf(0, 0))
        assertEquals(3, document.offsetOf(1, 0))
        assertEquals(5, document.offsetOf(1, 2))
        assertEquals(6, document.offsetOf(2, 0))
    }

    @Test
    fun offsetOfRejectsAnUnknownLineOrAColumnPastTheEnd() {
        val document = Document("ab\ncd")
        assertFailsWith<IllegalArgumentException> { document.offsetOf(2, 0) }
        assertFailsWith<IllegalArgumentException> { document.offsetOf(0, 3) }
        assertFailsWith<IllegalArgumentException> { document.offsetOf(0, -1) }
    }

    @Test
    fun byteOffsetCountsUtf8BytesOfEveryCharacterBeforeTheOffset() {
        val document = Document("aé€\uD83D\uDE00z")
        assertEquals(0, document.byteOffset(0))
        assertEquals(1, document.byteOffset(1))
        assertEquals(3, document.byteOffset(2))
        assertEquals(6, document.byteOffset(3))
        assertEquals(10, document.byteOffset(5))
        assertEquals(11, document.byteOffset(6))
    }

    @Test
    fun byteOffsetRoundsDownInsideASurrogatePair() {
        val document = Document("\uD83D\uDE00z")
        assertEquals(0, document.byteOffset(1))
        assertEquals(4, document.byteOffset(2))
    }

    @Test
    fun charOffsetRoundsDownInsideAMultiByteCharacter() {
        val document = Document("aé€\uD83D\uDE00z")
        assertEquals(1, document.charOffset(2))
        assertEquals(2, document.charOffset(4))
        assertEquals(3, document.charOffset(7))
        assertEquals(5, document.charOffset(10))
        assertEquals(6, document.charOffset(11))
    }

    @Test
    fun byteAndCharOffsetsRoundTripAtEveryCodePointBoundary() {
        val text = "aé€\uD83D\uDE00z\nnext"
        val document = Document(text)
        var offset = 0
        while (offset <= document.length) {
            val insidePair = offset in 1 until document.length &&
                text[offset - 1].isHighSurrogate() &&
                text[offset].isLowSurrogate()
            if (!insidePair) {
                assertEquals(offset, document.charOffset(document.byteOffset(offset)))
            }
            offset += 1
        }
    }

    @Test
    fun byteOffsetsRejectOffsetsOutsideTheDocument() {
        val document = Document("abc")
        assertFailsWith<IllegalArgumentException> { document.byteOffset(4) }
        assertFailsWith<IllegalArgumentException> { document.charOffset(4) }
    }

    @Test
    fun utf8MatchesTheStandardEncodingOfWellFormedText() {
        val text = "aé€\uD83D\uDE00z\nnext"
        assertContentEquals(text.encodeToByteArray(), Document(text).utf8)
    }

    @Test
    fun utf8IsComputedOncePerDocument() {
        val document = Document("abc")
        assertSame(document.utf8, document.utf8)
    }

    @Test
    fun utf8ReplacesALoneSurrogateTheWayThePlatformEncoderDoes() {
        val text = "a\uD800b"
        val document = Document(text)
        assertContentEquals(text.encodeToByteArray(), document.utf8)
        assertEquals(1, document.charOffset(1))
        assertEquals(2, document.charOffset(2))
        assertEquals(3, document.charOffset(3))
        assertEquals(1, document.byteOffset(1))
        assertEquals(2, document.byteOffset(2))
    }

    @Test
    fun editedDocumentRecomputesItsIndexes() {
        val edited = Document("ab\ncd").edit(listOf(DocumentEdit(2, 1, "\n\n")))
        assertEquals("ab\n\ncd", edited.text.toString())
        assertEquals(1, edited.lineOf(3))
        assertEquals(5, edited.byteOffset(5))
    }

    @Test
    fun indexLookupsStayCorrectPastTheFirstCheckpoint() {
        val text = "x".repeat(200) + "é\n" + "y".repeat(100)
        val document = Document(text)
        assertEquals(200, document.byteOffset(200))
        assertEquals(202, document.byteOffset(201))
        assertEquals(201, document.charOffset(202))
        assertEquals(0, document.lineOf(201))
        assertEquals(1, document.lineOf(202))
        assertEquals(303, document.utf8.size)
    }
}
