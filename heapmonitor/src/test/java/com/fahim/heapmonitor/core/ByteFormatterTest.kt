package com.fahim.heapmonitor.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatterTest {

    @Test
    fun `zero and negative are 0 B`() {
        assertEquals("0 B", ByteFormatter.format(0L))
        assertEquals("0 B", ByteFormatter.format(-1L))
        assertEquals("0 B", ByteFormatter.format(Long.MIN_VALUE))
    }

    @Test
    fun `bytes below 1024 have no decimal`() {
        assertEquals("1 B", ByteFormatter.format(1L))
        assertEquals("512 B", ByteFormatter.format(512L))
        assertEquals("1023 B", ByteFormatter.format(1023L))
    }

    @Test
    fun `kilobyte boundary`() {
        assertEquals("1.0 KB", ByteFormatter.format(1024L))
        assertEquals("1.5 KB", ByteFormatter.format(1536L))
        assertEquals("1024.0 KB", ByteFormatter.format(1024L * 1024L - 1L))
    }

    @Test
    fun `megabyte boundary`() {
        assertEquals("1.0 MB", ByteFormatter.format(1024L * 1024L))
        assertEquals("42.3 MB", ByteFormatter.format((42.3 * 1024 * 1024).toLong() + 1))
    }

    @Test
    fun `gigabyte boundary`() {
        assertEquals("1.0 GB", ByteFormatter.format(1024L * 1024L * 1024L))
        assertEquals("1.2 GB", ByteFormatter.format((1.2 * 1024 * 1024 * 1024).toLong() + 1))
    }

    @Test
    fun `formatMb has one decimal and no unit`() {
        assertEquals("0.0", ByteFormatter.formatMb(0L))
        assertEquals("0.0", ByteFormatter.formatMb(-5L))
        assertEquals("84.0", ByteFormatter.formatMb(84L * 1024L * 1024L))
        assertEquals("200.0", ByteFormatter.formatMb(200L * 1024L * 1024L))
        assertEquals("0.5", ByteFormatter.formatMb(512L * 1024L))
    }
}
