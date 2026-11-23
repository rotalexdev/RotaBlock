package dev.rotalex.rotablock.core.engine

import dev.rotalex.rotablock.core.block.BLOCK_MAPPING_EP
import dev.rotalex.rotablock.core.block.BlockEdit
import dev.rotalex.rotablock.core.block.BlockId
import dev.rotalex.rotablock.core.block.BlockMapping
import dev.rotalex.rotablock.core.block.BlockNode
import dev.rotalex.rotablock.core.block.BlockPart
import dev.rotalex.rotablock.core.block.BlockSlot
import dev.rotalex.rotablock.core.block.BlockTree
import dev.rotalex.rotablock.core.block.LayoutContext
import dev.rotalex.rotablock.core.block.LayoutPolicy
import dev.rotalex.rotablock.core.block.SlotRef
import dev.rotalex.rotablock.core.language.DocumentEdit
import dev.rotalex.rotablock.core.language.ExtensionPoint
import dev.rotalex.rotablock.core.language.TextRange

/**
 * Turns one intent into the document edits that realize it against the tree it was derived from.
 *
 * Every variant resolves its references first, so a reference that no longer resolves yields an empty list
 * instead of throwing. The layout rules of the tree's language decide which owned syntax disappears with an
 * element and which separators appear around a new one; the engine itself never computes whitespace, it
 * adds the element's own text and validates the combined list.
 *
 * The result is deterministic and minimal: edits that address the same offset merge, adjacent deletions
 * collapse, and the list descends by offset so applying it invalidates nothing.
 */
class EditCompiler(
    private val mappings: ExtensionPoint<BlockMapping> = BLOCK_MAPPING_EP,
) {
    /** Compiles [edit] against [tree], returning the edits in descending offset order. */
    fun compile(edit: BlockEdit, tree: BlockTree): List<DocumentEdit> {
        val layout = layoutOf(tree)
        val edits = when (edit) {
            is BlockEdit.SetField -> setField(edit, tree)
            is BlockEdit.ReplaceWithText -> replaceWithText(edit, tree, layout)
            is BlockEdit.Delete -> remove(edit.block.id, tree, layout)
            is BlockEdit.InsertTemplate -> insert(edit.slot, edit.template.insertionText, tree, layout)
            is BlockEdit.Wrap -> wrap(edit.block.id, edit.template.prefix, edit.template.suffix, tree)
            is BlockEdit.Move -> move(edit, tree, layout)
        }
        return TextEdits.normalize(edits, tree.root.range.end)
    }

    private fun layoutOf(tree: BlockTree): LayoutPolicy =
        mappings.extensions().firstOrNull { tree.language in it.languages }?.layout ?: LayoutPolicy.NONE

    private fun setField(edit: BlockEdit.SetField, tree: BlockTree): List<DocumentEdit> {
        val block = tree.block(edit.target.id) ?: return emptyList()
        val field = block.fields.getOrNull(edit.fieldIndex) ?: return emptyList()
        if (!field.editable) return emptyList()
        val range = field.range ?: return emptyList()
        return listOf(DocumentEdit(range.start, range.length, edit.text))
    }

    private fun replaceWithText(
        edit: BlockEdit.ReplaceWithText,
        tree: BlockTree,
        layout: LayoutPolicy,
    ): List<DocumentEdit> {
        val target = slotOf(edit.slot, tree) ?: return emptyList()
        if (edit.slot.index > target.children.size) return emptyList()
        if (edit.slot.index == target.children.size) return insert(edit.slot, edit.text, tree, layout)
        val child = target.children[edit.slot.index]
        return listOf(DocumentEdit(child.range.start, child.range.length, edit.text))
    }

    private fun remove(id: BlockId, tree: BlockTree, layout: LayoutPolicy): List<DocumentEdit> {
        val block = tree.block(id) ?: return emptyList()
        val position = positionsOf(tree)[id] ?: return emptyList()
        val siblings = position.slot.children
        val context = LayoutContext(
            element = block.range,
            previous = siblings.getOrNull(position.index - 1)?.range,
            next = siblings.getOrNull(position.index + 1)?.range,
        )
        return layout.onDelete(block.range, context) +
            DocumentEdit(block.range.start, block.range.length, "")
    }

    private fun insert(
        slot: SlotRef,
        text: String,
        tree: BlockTree,
        layout: LayoutPolicy,
    ): List<DocumentEdit> {
        val target = slotOf(slot, tree) ?: return emptyList()
        if (slot.index > target.children.size) return emptyList()
        val offset = insertionOffset(slot, target) ?: return emptyList()
        val context = LayoutContext(
            element = TextRange(offset, offset),
            previous = if (slot.index == 0) null else target.children[slot.index - 1].range,
            next = target.children.getOrNull(slot.index)?.range,
        )
        val separators = layout.onInsert(offset, text, context)
        separators.forEach { edit ->
            if (edit.offset != offset && edit.offset != offset + text.length) {
                throw IllegalStateException("A layout policy may only place a separator at ${offset} or ${offset + text.length}, but placed one at ${edit.offset}.")
            }
        }
        val leading = separators.filter { it.offset == offset }.joinToString("") { separator(it) }
        val trailing = separators.filter { it.offset == offset + text.length }.joinToString("") { separator(it) }
        return listOf(DocumentEdit(offset, 0, leading + text + trailing))
    }

    private fun wrap(id: BlockId, prefix: String, suffix: String, tree: BlockTree): List<DocumentEdit> {
        val block = tree.block(id) ?: return emptyList()
        val wrapped = prefix + textOf(block) + suffix
        return listOf(DocumentEdit(block.range.start, block.range.length, wrapped))
    }

    private fun move(edit: BlockEdit.Move, tree: BlockTree, layout: LayoutPolicy): List<DocumentEdit> {
        val block = tree.block(edit.block.id) ?: return emptyList()
        val position = positionsOf(tree)[block.id] ?: return emptyList()
        val target = slotOf(edit.target, tree) ?: return emptyList()
        if (edit.target.index > target.children.size) return emptyList()
        val stays =
            position.slot === target && (edit.target.index == position.index || edit.target.index == position.index + 1)
        if (stays) return emptyList()
        val insertion = insert(edit.target, textOf(block), tree, layout)
        return remove(block.id, tree, layout) + insertion
    }

    private fun separator(edit: DocumentEdit): String {
        if (edit.oldLength != 0) {
            throw IllegalStateException("A layout policy may only insert around a new element, but asked to remove at ${edit.offset}.")
        }
        return edit.newText
    }

    private fun slotOf(reference: SlotRef, tree: BlockTree): BlockSlot? =
        tree.block(reference.owner.id)?.slots?.getOrNull(reference.slotIndex)

    private fun insertionOffset(slot: SlotRef, target: BlockSlot): Int? = when {
        slot.index == target.children.size ->
            target.children.lastOrNull()?.range?.end ?: target.range?.start
        else -> target.children[slot.index].range.start
    }

    private fun positionsOf(tree: BlockTree): Map<BlockId, Position> {
        val positions = mutableMapOf<BlockId, Position>()
        fun visit(node: BlockNode) {
            node.slots.forEach { slot ->
                slot.children.forEachIndexed { index, child ->
                    positions[child.id] = Position(slot, index)
                    visit(child)
                }
            }
        }
        visit(tree.root)
        return positions
    }

    private fun textOf(block: BlockNode): String = buildString {
        block.parts.forEach { part ->
            when (part) {
                is BlockPart.Field -> append(part.field.text)
                is BlockPart.Slot -> part.slot.children.forEach { append(textOf(it)) }
            }
        }
    }

    private class Position(val slot: BlockSlot, val index: Int)
}
