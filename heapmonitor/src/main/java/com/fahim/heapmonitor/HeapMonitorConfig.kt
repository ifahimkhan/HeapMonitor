package com.fahim.heapmonitor

/** Corner of the activity window where the overlay first appears. */
enum class OverlayPosition { TopStart, TopEnd, BottomStart, BottomEnd }

/**
 * Immutable configuration for [HeapMonitor].
 *
 * @property enabled `null` = auto (visible only when the app is debuggable); `true`/`false` forces.
 * @property sampleIntervalMs how often the heap is read; coerced to >= 100 ms.
 * @property initialPosition corner the overlay starts in before the user drags it.
 * @property startExpanded show the full card instead of the compact chip on first attach.
 * @property showForceGcButton show the "Force GC" button in the expanded card.
 * @property warnUsedPercent used-heap percentage at which the usage colour turns amber.
 * @property criticalUsedPercent used-heap percentage at which the usage colour turns red.
 */
data class HeapMonitorConfig(
    val enabled: Boolean? = null,
    val sampleIntervalMs: Long = 1_000L,
    val initialPosition: OverlayPosition = OverlayPosition.TopEnd,
    val startExpanded: Boolean = false,
    val showForceGcButton: Boolean = true,
    val warnUsedPercent: Int = 75,
    val criticalUsedPercent: Int = 90,
)

/** Library-wide constants; keeps magic strings out of call sites. */
internal object Defaults {
    const val OVERLAY_TAG = "com.fahim.heapmonitor.OVERLAY"
    const val LOG_TAG = "HeapMonitor"
}
