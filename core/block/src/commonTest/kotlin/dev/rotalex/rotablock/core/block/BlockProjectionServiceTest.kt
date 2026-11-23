package dev.rotalex.rotablock.core.block

import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.DomNode
import dev.rotalex.rotablock.core.language.LanguageId
import dev.rotalex.rotablock.core.language.NodeKind
import dev.rotalex.rotablock.core.language.ParsedFile
import dev.rotalex.rotablock.core.language.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BlockProjectionServiceTest {
    private val source = "f(42)"
    private val language = LanguageId("kotlin")
    private val root = BlockNode(BlockId("root"), NodeKind.ROOT, null, TextRange(0, 5), emptyList())

    private inner class FakeService : BlockProjectionService {
        var seenEdit: BlockEdit? = null
        override fun project(parsed: ParsedFile, language: LanguageId): BlockTree = BlockTree(root, parsed.documentVersion)
        override fun compile(edit: BlockEdit, tree: BlockTree): List<DocumentEdit> {
            seenEdit = edit
            return if (tree.block((edit as? BlockEdit.Delete)?.block?.id ?: BlockId("absent")) == null) {
                emptyList()
            } else {
                listOf(DocumentEdit(0, 5, ""))
            }
        }
    }

    private fun parsed() = ParsedFile(source, 7L, DomNode(NodeKind.ROOT, TextRange(0, 5), source))

    @Test
    fun projectsAParsedFileIntoATreeCarryingItsVersion() {
        val tree = FakeService().project(parsed(), language)
        assertEquals(7L, tree.version)
        assertSame(root, tree.root)
    }

    @Test
    fun compilesAnEditAgainstTheTreeItWasDerivedFrom() {
        val service = FakeService()
        val edit = BlockEdit.Delete(BlockRef(BlockId("root")))
        assertEquals(listOf(DocumentEdit(0, 5, "")), service.compile(edit, BlockTree(root, 7L)))
        assertSame(edit, service.seenEdit)
    }

    @Test
    fun aStaleReferenceCompilesToNoEdits() {
        val service = FakeService()
        assertTrue(service.compile(BlockEdit.Delete(BlockRef(BlockId("gone"))), BlockTree(root, 7L)).isEmpty())
    }
}
