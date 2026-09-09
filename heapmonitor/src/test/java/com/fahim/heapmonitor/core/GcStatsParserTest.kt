package com.fahim.heapmonitor.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GcStatsParserTest {

    @Test
    fun `full map parses every field`() {
        val stats = GcStatsParser.parse(
            mapOf(
                "art.gc.gc-count" to "17",
                "art.gc.gc-time" to "340",
                "art.gc.blocking-gc-count" to "2",
                "art.gc.blocking-gc-time" to "12",
                "art.gc.bytes-allocated" to "123456789",
                "art.gc.bytes-freed" to "98765432",
            ),
        )
        assertEquals(GcStats(17L, 340L, 2L, 12L, 123456789L, 98765432L), stats)
    }

    @Test
    fun `missing keys become zero`() {
        val stats = GcStatsParser.parse(mapOf("art.gc.gc-count" to "5"))
        assertEquals(GcStats.EMPTY.copy(gcCount = 5L), stats)
    }

    @Test
    fun `empty map is EMPTY`() {
        assertEquals(GcStats.EMPTY, GcStatsParser.parse(emptyMap()))
    }

    @Test
    fun `garbage values become zero`() {
        val stats = GcStatsParser.parse(
            mapOf(
                "art.gc.gc-count" to "abc",
                "art.gc.gc-time" to "",
                "art.gc.blocking-gc-count" to "  ",
                "art.gc.blocking-gc-time" to "1.5",
                "art.gc.bytes-allocated" to " 42 ",
                "art.gc.bytes-freed" to "99999999999999999999999",
            ),
        )
        assertEquals(GcStats.EMPTY.copy(bytesAllocated = 42L), stats)
    }
}
