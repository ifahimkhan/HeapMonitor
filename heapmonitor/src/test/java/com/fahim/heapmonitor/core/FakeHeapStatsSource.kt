package com.fahim.heapmonitor.core

/**
 * Scripted [HeapStatsSource] for tests. Returns [script] entries in order, repeating the last one
 * forever. A `null` entry makes that read throw, to exercise the sampler's error handling.
 */
internal class FakeHeapStatsSource(
    private val script: List<RawHeapStats?>,
) : HeapStatsSource {

    private var index = 0
    var readCount: Int = 0
        private set

    override fun read(): RawHeapStats {
        readCount++
        val entry = script[minOf(index, script.lastIndex)]
        index++
        return entry ?: throw IllegalStateException("scripted failure")
    }
}

internal fun rawStats(
    maxBytes: Long = 200L * MB,
    totalBytes: Long = 120L * MB,
    freeBytes: Long = 36L * MB,
    gcCount: Long = 17L,
    gcTimeMs: Long = 340L,
    blockingGcCount: Long = 2L,
): RawHeapStats = RawHeapStats(
    maxBytes = maxBytes,
    totalBytes = totalBytes,
    freeBytes = freeBytes,
    nativeHeapSizeBytes = 30L * MB,
    nativeHeapAllocatedBytes = 20L * MB,
    nativeHeapFreeBytes = 10L * MB,
    gcStats = mapOf(
        GcStatsParser.KEY_GC_COUNT to gcCount.toString(),
        GcStatsParser.KEY_GC_TIME to gcTimeMs.toString(),
        GcStatsParser.KEY_BLOCKING_GC_COUNT to blockingGcCount.toString(),
        GcStatsParser.KEY_BLOCKING_GC_TIME to "12",
        GcStatsParser.KEY_BYTES_ALLOCATED to "123456789",
        GcStatsParser.KEY_BYTES_FREED to "98765432",
    ),
    memoryClassMb = 192,
    largeMemoryClassMb = 512,
    isLargeHeap = false,
)

internal const val MB: Long = 1024L * 1024L
