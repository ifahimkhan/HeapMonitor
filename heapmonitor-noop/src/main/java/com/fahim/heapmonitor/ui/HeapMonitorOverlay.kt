package com.fahim.heapmonitor.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fahim.heapmonitor.HeapMonitorConfig

/** No-op mirror of the real overlay: renders nothing. */
@Composable
fun HeapMonitorOverlay(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") config: HeapMonitorConfig = HeapMonitorConfig(),
) {
    Spacer(modifier = modifier.size(0.dp))
}
