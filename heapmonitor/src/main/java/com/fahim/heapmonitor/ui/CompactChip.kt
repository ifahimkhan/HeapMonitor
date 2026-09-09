package com.fahim.heapmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fahim.heapmonitor.HeapMonitorConfig
import com.fahim.heapmonitor.core.ByteFormatter
import com.fahim.heapmonitor.core.HeapSnapshot

/** Builds the single-line chip text, e.g. `"84.0/200.0 MB · GC 17"`. */
internal fun chipText(snapshot: HeapSnapshot): String =
    "${ByteFormatter.formatMb(snapshot.usedBytes)}/${ByteFormatter.formatMb(snapshot.maxBytes)} MB · GC ${snapshot.gcCount}"

/** Collapsed pill: coloured status dot + one line of text. */
@Composable
internal fun CompactChip(
    snapshot: HeapSnapshot,
    config: HeapMonitorConfig,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(OverlayDimens.chipHeight)
            .background(OverlayColors.background, CircleShape)
            .padding(horizontal = OverlayDimens.chipHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(OverlayDimens.dotSize)
                .background(usageColor(snapshot.usedPercent, config), CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = chipText(snapshot),
            color = OverlayColors.text,
            fontSize = OverlayDimens.textSize,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}
