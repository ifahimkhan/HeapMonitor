package com.fahim.heapmonitor.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.heapmonitor.HeapMonitorConfig

/** Self-contained dark palette so the overlay stays readable on any host theme. */
internal object OverlayColors {
    val background = Color(0xCC121212)
    val text = Color(0xFFECECEC)
    val textDim = Color(0xB3ECECEC)
    val ok = Color(0xFF4CAF50)
    val warn = Color(0xFFFFC107)
    val critical = Color(0xFFF44336)
    val accent = Color(0xFF64B5F6)
    val track = Color(0x33FFFFFF)
}

/** Shared dimensions and type sizes for the overlay composables. */
internal object OverlayDimens {
    val cornerRadius = 8.dp
    val cardMaxWidth = 260.dp
    val cardPadding = 10.dp
    val chipHeight = 24.dp
    val chipHorizontalPadding = 8.dp
    val dotSize = 6.dp
    val barHeight = 4.dp
    val textSize = 11.sp
}

/** Test tags exposed for UI tests. */
internal object OverlayTags {
    const val OVERLAY = "heap_overlay"
    const val EXPANDED = "heap_expanded"
    const val FORCE_GC = "heap_force_gc"
    const val COLLAPSE = "heap_collapse"
    const val USAGE_OK = "heap_usage_ok"
    const val USAGE_WARN = "heap_usage_warn"
    const val USAGE_CRITICAL = "heap_usage_critical"
    const val CONTENT_DESCRIPTION = "Heap monitor overlay"
}

/** Severity of the current heap usage relative to the configured thresholds. */
internal enum class UsageLevel { Ok, Warn, Critical }

internal fun usageLevel(percent: Int, config: HeapMonitorConfig): UsageLevel = when {
    percent >= config.criticalUsedPercent -> UsageLevel.Critical
    percent >= config.warnUsedPercent -> UsageLevel.Warn
    else -> UsageLevel.Ok
}

internal fun usageColor(percent: Int, config: HeapMonitorConfig): Color =
    when (usageLevel(percent, config)) {
        UsageLevel.Ok -> OverlayColors.ok
        UsageLevel.Warn -> OverlayColors.warn
        UsageLevel.Critical -> OverlayColors.critical
    }

internal fun usageTag(percent: Int, config: HeapMonitorConfig): String =
    when (usageLevel(percent, config)) {
        UsageLevel.Ok -> OverlayTags.USAGE_OK
        UsageLevel.Warn -> OverlayTags.USAGE_WARN
        UsageLevel.Critical -> OverlayTags.USAGE_CRITICAL
    }
