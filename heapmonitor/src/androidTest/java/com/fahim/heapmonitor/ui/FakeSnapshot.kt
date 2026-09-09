package com.fahim.heapmonitor.ui

import com.fahim.heapmonitor.core.HeapSnapshot

private const val MB = 1024L * 1024L

/** Small snapshot factory for UI tests; mirrors the preview data (84/200 MB, 17 GCs). */
internal fun fakeSnapshot(
    usedMb: Int = 84,
    maxMb: Int = 200,
    gcCount: Long = 17,
    gcDelta: Long = 0,
): HeapSnapshot = HeapSnapshot.EMPTY.copy(
    timestampMs = 1L,
    maxBytes = maxMb * MB,
    totalBytes = (usedMb + 36) * MB,
    freeBytes = 36 * MB,
    nativeHeapSizeBytes = 30 * MB,
    nativeHeapAllocatedBytes = 20 * MB,
    nativeHeapFreeBytes = 10 * MB,
    gcCount = gcCount,
    gcTimeMs = 340L,
    blockingGcCount = 2L,
    blockingGcTimeMs = 12L,
    gcCountDelta = gcDelta,
    memoryClassMb = 192,
    largeMemoryClassMb = 512,
)
