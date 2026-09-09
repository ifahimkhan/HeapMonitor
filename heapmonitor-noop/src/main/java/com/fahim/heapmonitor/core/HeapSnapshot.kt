package com.fahim.heapmonitor.core

/**
 * Immutable point-in-time view of JVM heap, native heap and ART GC counters.
 *
 * All byte values are raw `Long`s; use [ByteFormatter] for display. Derived values are computed
 * properties so no duplicated state can drift.
 */
data class HeapSnapshot(
    val timestampMs: Long,
    val maxBytes: Long,
    val totalBytes: Long,
    val freeBytes: Long,
    val nativeHeapSizeBytes: Long,
    val nativeHeapAllocatedBytes: Long,
    val nativeHeapFreeBytes: Long,
    val gcCount: Long,
    val gcTimeMs: Long,
    val blockingGcCount: Long,
    val blockingGcTimeMs: Long,
    val bytesAllocatedTotal: Long,
    val bytesFreedTotal: Long,
    val gcCountDelta: Long,
    val memoryClassMb: Int,
    val largeMemoryClassMb: Int,
    val isLargeHeap: Boolean,
    /**
     * Number of `Runtime.gc()` calls made through `HeapMonitor.forceGc()`. Each one is also
     * counted in [blockingGcCount] because ART treats explicit GCs as blocking.
     */
    val explicitGcCount: Long = 0L,
) {
    /** Bytes currently in use inside the VM-allocated heap. */
    val usedBytes: Long get() = totalBytes - freeBytes

    /** Bytes the VM may still hand out before hitting [maxBytes]. */
    val availableBytes: Long get() = (maxBytes - usedBytes).coerceAtLeast(0L)

    /** Used heap as a percentage of [maxBytes], clamped to 0..100; 0 when max is unknown. */
    val usedPercent: Int
        get() = if (maxBytes <= 0L) 0 else (usedBytes * 100L / maxBytes).toInt().coerceIn(0, 100)

    /** Blocking GCs not caused by Force GC: [blockingGcCount] minus [explicitGcCount], min 0. */
    val naturalBlockingGcCount: Long get() = (blockingGcCount - explicitGcCount).coerceAtLeast(0L)

    companion object {
        /** Snapshot emitted before the first real sample. */
        val EMPTY = HeapSnapshot(
            timestampMs = 0L,
            maxBytes = 0L,
            totalBytes = 0L,
            freeBytes = 0L,
            nativeHeapSizeBytes = 0L,
            nativeHeapAllocatedBytes = 0L,
            nativeHeapFreeBytes = 0L,
            gcCount = 0L,
            gcTimeMs = 0L,
            blockingGcCount = 0L,
            blockingGcTimeMs = 0L,
            bytesAllocatedTotal = 0L,
            bytesFreedTotal = 0L,
            gcCountDelta = 0L,
            memoryClassMb = 0,
            largeMemoryClassMb = 0,
            isLargeHeap = false,
            explicitGcCount = 0L,
        )
    }
}
