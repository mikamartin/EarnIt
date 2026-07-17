package com.earnit.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DragReorderTest {
    // Four equal-height slots stacked top to bottom: [0,100), [100,200), [200,300), [300,400).
    private val slots =
        listOf(
            DragReorder.ItemBounds(index = 0, offset = 0, size = 100),
            DragReorder.ItemBounds(index = 1, offset = 100, size = 100),
            DragReorder.ItemBounds(index = 2, offset = 200, size = 100),
            DragReorder.ItemBounds(index = 3, offset = 300, size = 100),
        )

    @Test
    fun `center still inside the dragged item's own slot returns no target`() {
        assertNull(DragReorder.targetIndex(draggingIndex = 0, dragCenter = 50f, visibleItems = slots))
    }

    @Test
    fun `center over another slot returns that slot's index`() {
        assertEquals(1, DragReorder.targetIndex(draggingIndex = 0, dragCenter = 150f, visibleItems = slots))
    }

    @Test
    fun `dragged item is excluded even if the center falls back inside its own bounds`() {
        // draggingIndex is 1, but the center passed in falls inside slot 1's own range.
        assertNull(DragReorder.targetIndex(draggingIndex = 1, dragCenter = 150f, visibleItems = slots))
    }

    @Test
    fun `center exactly at a slot's leading edge does not match`() {
        assertNull(DragReorder.targetIndex(draggingIndex = 0, dragCenter = 100f, visibleItems = slots))
    }

    @Test
    fun `center exactly at a slot's trailing edge does not match`() {
        assertNull(DragReorder.targetIndex(draggingIndex = 0, dragCenter = 200f, visibleItems = slots))
    }

    @Test
    fun `center one unit past the leading edge matches`() {
        assertEquals(1, DragReorder.targetIndex(draggingIndex = 0, dragCenter = 100.01f, visibleItems = slots))
    }

    @Test
    fun `reordered moves an item down and shifts items in between up`() {
        assertEquals(listOf("B", "C", "A", "D"), DragReorder.reordered(listOf("A", "B", "C", "D"), fromIndex = 0, toIndex = 2))
    }

    @Test
    fun `reordered moves an item up and shifts items in between down`() {
        assertEquals(listOf("A", "D", "B", "C"), DragReorder.reordered(listOf("A", "B", "C", "D"), fromIndex = 3, toIndex = 1))
    }

    @Test
    fun `dragging down twice then up once ends with the item shifted down one net position`() {
        var list = listOf("A", "B", "C", "D")
        var draggingIndex = 0

        // Drag A down into slot 1 (over B).
        var target = DragReorder.targetIndex(draggingIndex, dragCenter = 150f, visibleItems = slots)
        assertEquals(1, target)
        list = DragReorder.reordered(list, draggingIndex, target!!)
        draggingIndex = target
        assertEquals(listOf("B", "A", "C", "D"), list)

        // Drag further down into slot 2 (over C).
        target = DragReorder.targetIndex(draggingIndex, dragCenter = 250f, visibleItems = slots)
        assertEquals(2, target)
        list = DragReorder.reordered(list, draggingIndex, target!!)
        draggingIndex = target
        assertEquals(listOf("B", "C", "A", "D"), list)

        // Drag back up into slot 1 (over C, now sitting in slot 1).
        target = DragReorder.targetIndex(draggingIndex, dragCenter = 150f, visibleItems = slots)
        assertEquals(1, target)
        list = DragReorder.reordered(list, draggingIndex, target!!)
        assertEquals(listOf("B", "A", "C", "D"), list)
    }
}
