package com.fahim.heapmonitor.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.fahim.heapmonitor.OverlayPosition

/** Saver so the drag offset survives configuration changes via `rememberSaveable`. */
internal val OffsetSaver: Saver<Offset, Any> = listSaver(
    save = { listOf(it.x, it.y) },
    restore = { Offset(it[0], it[1]) },
)

internal fun OverlayPosition.toAlignment(): Alignment = when (this) {
    OverlayPosition.TopStart -> Alignment.TopStart
    OverlayPosition.TopEnd -> Alignment.TopEnd
    OverlayPosition.BottomStart -> Alignment.BottomStart
    OverlayPosition.BottomEnd -> Alignment.BottomEnd
}

/**
 * Clamps a drag [offset] (relative to [basePosition], the aligned resting spot) so the content of
 * [contentSize] stays fully inside [bounds]. Pure function; unit-testable.
 */
internal fun clampOffset(
    offset: Offset,
    bounds: IntSize,
    contentSize: IntSize,
    basePosition: IntOffset,
): Offset {
    if (bounds.width <= 0 || bounds.height <= 0) return offset
    val maxX = (bounds.width - contentSize.width).coerceAtLeast(0)
    val maxY = (bounds.height - contentSize.height).coerceAtLeast(0)
    val absX = (basePosition.x + offset.x).coerceIn(0f, maxX.toFloat())
    val absY = (basePosition.y + offset.y).coerceIn(0f, maxY.toFloat())
    return Offset(absX - basePosition.x, absY - basePosition.y)
}

/** Resting position of content aligned by [alignment] inside [bounds]. */
internal fun basePosition(
    alignment: Alignment,
    contentSize: IntSize,
    bounds: IntSize,
    layoutDirection: LayoutDirection,
): IntOffset = alignment.align(contentSize, bounds, layoutDirection)

/**
 * Drag handler that updates [offset], keeping the content inside the parent bounds.
 * Does not consume taps: a tap without movement past touch slop is left to [detectTapGestures].
 */
internal fun Modifier.draggableOverlay(
    offset: MutableState<Offset>,
    bounds: IntSize,
    contentSize: IntSize,
    basePosition: IntOffset,
): Modifier = pointerInput(bounds, contentSize, basePosition) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        offset.value = clampOffset(offset.value + dragAmount, bounds, contentSize, basePosition)
    }
}
