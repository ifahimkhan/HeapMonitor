package com.fahim.heapmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.fahim.heapmonitor.HeapMonitorConfig

/** Thin rounded bar whose fill and colour reflect [usedPercent]. */
@Composable
internal fun UsageBar(
    usedPercent: Int,
    config: HeapMonitorConfig,
    modifier: Modifier = Modifier,
) {
    val fraction = usedPercent.coerceIn(0, 100) / 100f
    val shape = RoundedCornerShape(OverlayDimens.barHeight)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(OverlayDimens.barHeight)
            .clip(shape)
            .background(OverlayColors.track)
            .testTag(usageTag(usedPercent, config)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(usageColor(usedPercent, config), shape),
        )
    }
}
