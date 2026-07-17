package com.earnit.app.ui

// Pure reorder math shared by Home's reward list and Tasks' task list long-press-drag
// gestures, extracted from their pointerInput blocks so it's testable without a Compose runtime.
object DragReorder {
    data class ItemBounds(
        val index: Int,
        val offset: Int,
        val size: Int,
    )

    fun targetIndex(
        draggingIndex: Int,
        dragCenter: Float,
        visibleItems: List<ItemBounds>,
    ): Int? =
        visibleItems
            .firstOrNull { item ->
                item.index != draggingIndex &&
                    dragCenter > item.offset &&
                    dragCenter < item.offset + item.size
            }?.index

    fun <T> reordered(
        list: List<T>,
        fromIndex: Int,
        toIndex: Int,
    ): List<T> = list.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
