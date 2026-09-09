package com.fahim.heapmonitor.ui

import com.fahim.heapmonitor.core.HeapSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricRowsTest {

    private fun value(snapshot: HeapSnapshot, label: String): String =
        metricRows(snapshot).single { it.label == label }.value

    @Test
    fun `blocking gc row shows total and forced count`() {
        val s = HeapSnapshot.EMPTY.copy(blockingGcCount = 7L, explicitGcCount = 3L)
        assertEquals("7 (3 forced)", value(s, "Blocking GC"))
    }

    @Test
    fun `blocking gc row omits forced suffix when none`() {
        val s = HeapSnapshot.EMPTY.copy(blockingGcCount = 7L)
        assertEquals("7", value(s, "Blocking GC"))
    }

    @Test
    fun `blocking time row is present in ms`() {
        val s = HeapSnapshot.EMPTY.copy(blockingGcTimeMs = 12L)
        assertEquals("12 ms", value(s, "Blocking time"))
    }
}
