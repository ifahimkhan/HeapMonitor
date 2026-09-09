package com.fahim.heapmonitor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HeapSnapshotTest {

    private fun snapshot(max: Long, total: Long, free: Long) = HeapSnapshot.EMPTY.copy(
        timestampMs = 1L,
        maxBytes = max,
        totalBytes = total,
        freeBytes = free,
    )

    @Test
    fun `used is total minus free`() {
        val s = snapshot(max = 200, total = 120, free = 36)
        assertEquals(84L, s.usedBytes)
    }

    @Test
    fun `available is max minus used`() {
        val s = snapshot(max = 200, total = 120, free = 36)
        assertEquals(116L, s.availableBytes)
    }

    @Test
    fun `used percent is computed against max`() {
        val s = snapshot(max = 200, total = 120, free = 36)
        assertEquals(42, s.usedPercent)
    }

    @Test
    fun `used percent clamps at 100 when used exceeds max`() {
        val s = snapshot(max = 100, total = 250, free = 0)
        assertEquals(100, s.usedPercent)
        assertEquals(0L, s.availableBytes)
    }

    @Test
    fun `used percent never negative`() {
        val s = snapshot(max = 100, total = 10, free = 50)
        assertEquals(0, s.usedPercent)
    }

    @Test
    fun `zero max yields zero percent without dividing`() {
        val s = snapshot(max = 0, total = 50, free = 10)
        assertEquals(0, s.usedPercent)
    }

    @Test
    fun `natural blocking count subtracts explicit GCs`() {
        val s = HeapSnapshot.EMPTY.copy(blockingGcCount = 5L, explicitGcCount = 2L)
        assertEquals(3L, s.naturalBlockingGcCount)
    }

    @Test
    fun `natural blocking count never negative`() {
        val s = HeapSnapshot.EMPTY.copy(blockingGcCount = 1L, explicitGcCount = 4L)
        assertEquals(0L, s.naturalBlockingGcCount)
    }

    @Test
    fun `EMPTY is all zeros`() {
        val e = HeapSnapshot.EMPTY
        assertEquals(0L, e.timestampMs)
        assertEquals(0L, e.usedBytes)
        assertEquals(0L, e.availableBytes)
        assertEquals(0, e.usedPercent)
        assertFalse(e.isLargeHeap)
    }
}
