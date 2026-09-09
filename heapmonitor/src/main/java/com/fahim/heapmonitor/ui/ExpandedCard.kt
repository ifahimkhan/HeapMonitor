package com.fahim.heapmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fahim.heapmonitor.HeapMonitorConfig
import com.fahim.heapmonitor.core.ByteFormatter
import com.fahim.heapmonitor.core.HeapSnapshot

/** One label/value line inside the card. */
internal data class MetricRow(val label: String, val value: String)

internal fun metricRows(snapshot: HeapSnapshot): List<MetricRow> = with(snapshot) {
    val gcDelta = if (gcCountDelta > 0) " (+$gcCountDelta)" else ""
    val largeHeap = if (isLargeHeap) " · largeHeap" else ""
    listOf(
        MetricRow("Heap max", ByteFormatter.format(maxBytes)),
        MetricRow("Allocated", ByteFormatter.format(totalBytes)),
        MetricRow("Used", ByteFormatter.format(usedBytes)),
        MetricRow("Free", ByteFormatter.format(freeBytes)),
        MetricRow("Available", ByteFormatter.format(availableBytes)),
        MetricRow("Used %", "$usedPercent%"),
        MetricRow("Native alloc", ByteFormatter.format(nativeHeapAllocatedBytes)),
        MetricRow("Native size", ByteFormatter.format(nativeHeapSizeBytes)),
        MetricRow("GC count", "$gcCount$gcDelta"),
        MetricRow("GC time", "$gcTimeMs ms"),
        MetricRow("Blocking GC", "$blockingGcCount"),
        MetricRow("Memory class", "$memoryClassMb MB (large: $largeMemoryClassMb MB)$largeHeap"),
    )
}

/** Full metrics card with usage bar and action buttons. */
@Composable
internal fun ExpandedCard(
    snapshot: HeapSnapshot,
    config: HeapMonitorConfig,
    onForceGc: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = OverlayDimens.cardMaxWidth)
            .background(OverlayColors.background, RoundedCornerShape(OverlayDimens.cornerRadius))
            .padding(OverlayDimens.cardPadding)
            .testTag(OverlayTags.EXPANDED),
    ) {
        metricRows(snapshot).forEach { row -> MetricLine(row) }
        Spacer(modifier = Modifier.height(6.dp))
        UsageBar(usedPercent = snapshot.usedPercent, config = config)
        Spacer(modifier = Modifier.height(2.dp))
        ActionRow(config = config, onForceGc = onForceGc, onCollapse = onCollapse)
    }
}

@Composable
private fun MetricLine(row: MetricRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = row.label,
            color = OverlayColors.textDim,
            fontSize = OverlayDimens.textSize,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Text(
            text = row.value,
            color = OverlayColors.text,
            fontSize = OverlayDimens.textSize,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionRow(
    config: HeapMonitorConfig,
    onForceGc: () -> Unit,
    onCollapse: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        if (config.showForceGcButton) {
            TextButton(
                onClick = onForceGc,
                modifier = Modifier.testTag(OverlayTags.FORCE_GC),
            ) {
                Text("Force GC", color = OverlayColors.accent, fontSize = OverlayDimens.textSize)
            }
        }
        TextButton(
            onClick = onCollapse,
            modifier = Modifier.testTag(OverlayTags.COLLAPSE),
        ) {
            Text("Collapse", color = OverlayColors.accent, fontSize = OverlayDimens.textSize)
        }
    }
}
