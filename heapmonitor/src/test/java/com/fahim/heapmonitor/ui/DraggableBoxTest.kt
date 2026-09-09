package com.fahim.heapmonitor.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.fahim.heapmonitor.HeapMonitorConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class DraggableBoxTest {

    private val bounds = IntSize(1000, 2000)
    private val content = IntSize(200, 100)

    @Test
    fun `offset inside bounds is unchanged`() {
        val result = clampOffset(Offset(50f, 60f), bounds, content, IntOffset.Zero)
        assertEquals(Offset(50f, 60f), result)
    }

    @Test
    fun `negative drag from top-start is clamped to zero`() {
        val result = clampOffset(Offset(-30f, -40f), bounds, content, IntOffset.Zero)
        assertEquals(Offset.Zero, result)
    }

    @Test
    fun `drag past bottom-end is clamped to remaining space`() {
        val result = clampOffset(Offset(5000f, 5000f), bounds, content, IntOffset.Zero)
        assertEquals(Offset(800f, 1900f), result)
    }

    @Test
    fun `top-end base position allows only negative x`() {
        val base = IntOffset(800, 0)
        assertEquals(Offset.Zero, clampOffset(Offset(50f, 0f), bounds, content, base))
        assertEquals(Offset(-800f, 0f), clampOffset(Offset(-900f, 0f), bounds, content, base))
    }

    @Test
    fun `zero bounds leaves offset untouched`() {
        val result = clampOffset(Offset(7f, 9f), IntSize.Zero, content, IntOffset.Zero)
        assertEquals(Offset(7f, 9f), result)
    }

    @Test
    fun `usage level thresholds`() {
        val config = HeapMonitorConfig(warnUsedPercent = 75, criticalUsedPercent = 90)
        assertEquals(UsageLevel.Ok, usageLevel(74, config))
        assertEquals(UsageLevel.Warn, usageLevel(75, config))
        assertEquals(UsageLevel.Warn, usageLevel(89, config))
        assertEquals(UsageLevel.Critical, usageLevel(90, config))
        assertEquals(OverlayTags.USAGE_CRITICAL, usageTag(100, config))
        assertEquals(OverlayColors.ok, usageColor(0, config))
    }
}
