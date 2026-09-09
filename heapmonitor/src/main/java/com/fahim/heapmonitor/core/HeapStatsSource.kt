package com.fahim.heapmonitor.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug

/** Raw, unparsed inputs read from the platform in a single pass. */
data class RawHeapStats(
    val maxBytes: Long,
    val totalBytes: Long,
    val freeBytes: Long,
    val nativeHeapSizeBytes: Long,
    val nativeHeapAllocatedBytes: Long,
    val nativeHeapFreeBytes: Long,
    val gcStats: Map<String, String>,
    val memoryClassMb: Int,
    val largeMemoryClassMb: Int,
    val isLargeHeap: Boolean,
) {
    companion object {
        val EMPTY = RawHeapStats(
            maxBytes = 0L,
            totalBytes = 0L,
            freeBytes = 0L,
            nativeHeapSizeBytes = 0L,
            nativeHeapAllocatedBytes = 0L,
            nativeHeapFreeBytes = 0L,
            gcStats = emptyMap(),
            memoryClassMb = 0,
            largeMemoryClassMb = 0,
            isLargeHeap = false,
        )
    }
}

/** Abstraction over the platform memory APIs so the sampler is unit-testable. */
interface HeapStatsSource {
    fun read(): RawHeapStats
}

/**
 * Real implementation backed by [Runtime], [Debug] and [ActivityManager].
 * Every platform call is wrapped so a failure yields a zero, never an exception.
 */
internal class RuntimeHeapStatsSource(context: Context) : HeapStatsSource {

    private val memoryClassMb: Int
    private val largeMemoryClassMb: Int
    private val isLargeHeap: Boolean

    init {
        val appContext = context.applicationContext ?: context
        val am = runCatching {
            appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        }.getOrNull()
        memoryClassMb = runCatching { am?.memoryClass ?: 0 }.getOrDefault(0)
        largeMemoryClassMb = runCatching { am?.largeMemoryClass ?: 0 }.getOrDefault(0)
        isLargeHeap = runCatching {
            appContext.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0
        }.getOrDefault(false)
    }

    override fun read(): RawHeapStats {
        val runtime = Runtime.getRuntime()
        return RawHeapStats(
            maxBytes = safeLong { runtime.maxMemory() },
            totalBytes = safeLong { runtime.totalMemory() },
            freeBytes = safeLong { runtime.freeMemory() },
            nativeHeapSizeBytes = safeLong { Debug.getNativeHeapSize() },
            nativeHeapAllocatedBytes = safeLong { Debug.getNativeHeapAllocatedSize() },
            nativeHeapFreeBytes = safeLong { Debug.getNativeHeapFreeSize() },
            gcStats = runCatching { Debug.getRuntimeStats() ?: emptyMap() }.getOrDefault(emptyMap()),
            memoryClassMb = memoryClassMb,
            largeMemoryClassMb = largeMemoryClassMb,
            isLargeHeap = isLargeHeap,
        )
    }

    private inline fun safeLong(block: () -> Long): Long = runCatching(block).getOrDefault(0L)
}
