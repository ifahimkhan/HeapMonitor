package com.fahim.heapmonitor.core

/** Parsed ART garbage-collector counters. All values are cumulative since process start. */
data class GcStats(
    val gcCount: Long,
    val gcTimeMs: Long,
    val blockingGcCount: Long,
    val blockingGcTimeMs: Long,
    val bytesAllocated: Long,
    val bytesFreed: Long,
) {
    companion object {
        val EMPTY = GcStats(0L, 0L, 0L, 0L, 0L, 0L)
    }
}

/**
 * Parses the map returned by `android.os.Debug.getRuntimeStats()`.
 * Missing, blank or non-numeric values become `0`; this function never throws.
 */
internal object GcStatsParser {
    const val KEY_GC_COUNT = "art.gc.gc-count"
    const val KEY_GC_TIME = "art.gc.gc-time"
    const val KEY_BLOCKING_GC_COUNT = "art.gc.blocking-gc-count"
    const val KEY_BLOCKING_GC_TIME = "art.gc.blocking-gc-time"
    const val KEY_BYTES_ALLOCATED = "art.gc.bytes-allocated"
    const val KEY_BYTES_FREED = "art.gc.bytes-freed"

    fun parse(stats: Map<String, String>): GcStats = GcStats(
        gcCount = stats.longOrZero(KEY_GC_COUNT),
        gcTimeMs = stats.longOrZero(KEY_GC_TIME),
        blockingGcCount = stats.longOrZero(KEY_BLOCKING_GC_COUNT),
        blockingGcTimeMs = stats.longOrZero(KEY_BLOCKING_GC_TIME),
        bytesAllocated = stats.longOrZero(KEY_BYTES_ALLOCATED),
        bytesFreed = stats.longOrZero(KEY_BYTES_FREED),
    )

    private fun Map<String, String>.longOrZero(key: String): Long =
        this[key]?.trim()?.toLongOrNull() ?: 0L
}
