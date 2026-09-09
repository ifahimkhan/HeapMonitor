package com.fahim.heapmonitor.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.heapmonitor.HeapMonitor
import com.fahim.heapmonitor.HeapMonitorConfig
import com.fahim.heapmonitor.core.HeapSnapshot
import kotlin.math.roundToInt

/**
 * Draggable, tap-to-expand floating panel showing live heap metrics from [HeapMonitor.snapshots].
 *
 * Fills its parent but only the chip/card consumes touches; the rest of the area lets events through.
 * Place it inside your own `Box` if you want to control where it lives; the auto-installer already
 * puts one on every `ComponentActivity` window.
 */
@Composable
fun HeapMonitorOverlay(
    modifier: Modifier = Modifier,
    config: HeapMonitorConfig = HeapMonitorConfig(),
) {
    val snapshot by HeapMonitor.snapshots.collectAsStateWithLifecycle()
    val layoutDirection = LocalLayoutDirection.current
    val alignment = remember(config.initialPosition) { config.initialPosition.toAlignment() }
    val offset = rememberSaveable(stateSaver = OffsetSaver) { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val base = basePosition(alignment, contentSize, bounds, layoutDirection)
    // Re-clamp a restored/dragged offset whenever the window or content size changes (rotation).
    LaunchedEffect(bounds, contentSize, base) {
        offset.value = clampOffset(offset.value, bounds, contentSize, base)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .onSizeChanged { bounds = it },
    ) {
        HeapMonitorOverlayContent(
            snapshot = snapshot,
            config = config,
            onForceGc = HeapMonitor::forceGc,
            modifier = Modifier
                .align(alignment)
                .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                .onSizeChanged { contentSize = it }
                .draggableOverlay(offset, bounds, contentSize, base),
        )
    }
}

/**
 * Stateless (apart from expanded/collapsed) rendering of one [snapshot]. Used by the public overlay,
 * previews and UI tests. Never touches [HeapMonitor] or any memory API.
 */
@Composable
internal fun HeapMonitorOverlayContent(
    snapshot: HeapSnapshot,
    config: HeapMonitorConfig,
    onForceGc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(config.startExpanded) }
    Box(
        modifier = modifier
            .semantics { contentDescription = OverlayTags.CONTENT_DESCRIPTION }
            .testTag(OverlayTags.OVERLAY)
            .animateContentSize(),
    ) {
        if (expanded) {
            ExpandedCard(
                snapshot = snapshot,
                config = config,
                onForceGc = onForceGc,
                onCollapse = { expanded = false },
            )
        } else {
            CompactChip(
                snapshot = snapshot,
                config = config,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = { expanded = true })
                },
            )
        }
    }
}

internal fun previewSnapshot(
    usedMb: Int = 84,
    maxMb: Int = 200,
    gcCount: Long = 17,
    gcDelta: Long = 0,
): HeapSnapshot {
    val mb = 1024L * 1024L
    return HeapSnapshot.EMPTY.copy(
        timestampMs = 1L,
        maxBytes = maxMb * mb,
        totalBytes = (usedMb + 36) * mb,
        freeBytes = 36 * mb,
        nativeHeapSizeBytes = 30 * mb,
        nativeHeapAllocatedBytes = 20 * mb,
        nativeHeapFreeBytes = 10 * mb,
        gcCount = gcCount,
        gcTimeMs = 340L,
        blockingGcCount = 2L,
        blockingGcTimeMs = 12L,
        gcCountDelta = gcDelta,
        memoryClassMb = 192,
        largeMemoryClassMb = 512,
    )
}

@Preview(name = "Compact chip")
@Composable
private fun CompactPreview() {
    HeapMonitorOverlayContent(
        snapshot = previewSnapshot(),
        config = HeapMonitorConfig(startExpanded = false),
        onForceGc = {},
    )
}

@Preview(name = "Expanded card")
@Composable
private fun ExpandedPreview() {
    HeapMonitorOverlayContent(
        snapshot = previewSnapshot(gcDelta = 2),
        config = HeapMonitorConfig(startExpanded = true),
        onForceGc = {},
    )
}
