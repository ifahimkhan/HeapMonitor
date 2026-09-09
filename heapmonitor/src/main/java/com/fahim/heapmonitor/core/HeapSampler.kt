package com.fahim.heapmonitor.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Periodically reads a [HeapStatsSource] and publishes [HeapSnapshot]s on [snapshots].
 *
 * The loop runs in its own scope on [dispatcher]; a failing read is swallowed per-iteration so the
 * host app can never crash because of sampling.
 */
internal class HeapSampler(
    private val source: HeapStatsSource,
    intervalMs: Long,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val intervalMs: Long = intervalMs.coerceAtLeast(MIN_INTERVAL_MS)
    private val _snapshots = MutableStateFlow(HeapSnapshot.EMPTY)
    private val explicitGcCount = AtomicLong(0L)

    /** Long-lived scope for one-shot GC requests; independent of the start/stop sampling loop. */
    private val gcScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val lock = Any()
    private var scope: CoroutineScope? = null
    private var job: Job? = null

    /** Latest snapshot; [HeapSnapshot.EMPTY] until the first successful read. */
    val snapshots: StateFlow<HeapSnapshot> = _snapshots.asStateFlow()

    val isRunning: Boolean get() = synchronized(lock) { job?.isActive == true }

    /** Starts the sampling loop. Calling it while running is a no-op. */
    fun start() {
        synchronized(lock) {
            if (job?.isActive == true) return
            val newScope = CoroutineScope(SupervisorJob() + dispatcher)
            scope = newScope
            job = newScope.launch { loop() }
        }
    }

    /** Stops the loop. Safe to call repeatedly; [start] may be called again afterwards. */
    fun stop() {
        synchronized(lock) {
            job?.cancel()
            scope?.cancel()
            job = null
            scope = null
        }
    }

    /** Performs one synchronous read and emits it. Never throws. */
    fun sampleNow() {
        runCatching { _snapshots.value = buildSnapshot(source.read(), _snapshots.value) }
    }

    /**
     * Runs [gc] on [dispatcher], never on the caller's thread, then counts it as an explicit GC and
     * re-samples. Works whether or not the sampling loop is running.
     *
     * ART runs `Runtime.gc()` synchronously on the calling thread and flags it as a blocking
     * collection, so calling it from the UI thread would freeze the app for the pause.
     */
    fun forceGc(gc: () -> Unit = ::runtimeGc) {
        gcScope.launch {
            runCatching(gc)
            explicitGcCount.incrementAndGet()
            sampleNow()
        }
    }

    private suspend fun CoroutineScope.loop() {
        while (isActive) {
            sampleNow()
            delay(intervalMs)
        }
    }

    private fun buildSnapshot(raw: RawHeapStats, previous: HeapSnapshot): HeapSnapshot {
        val gc = GcStatsParser.parse(raw.gcStats)
        val delta =
            if (previous.timestampMs == 0L) 0L else (gc.gcCount - previous.gcCount).coerceAtLeast(0L)
        return HeapSnapshot(
            timestampMs = clock(),
            maxBytes = raw.maxBytes,
            totalBytes = raw.totalBytes,
            freeBytes = raw.freeBytes,
            nativeHeapSizeBytes = raw.nativeHeapSizeBytes,
            nativeHeapAllocatedBytes = raw.nativeHeapAllocatedBytes,
            nativeHeapFreeBytes = raw.nativeHeapFreeBytes,
            gcCount = gc.gcCount,
            gcTimeMs = gc.gcTimeMs,
            blockingGcCount = gc.blockingGcCount,
            blockingGcTimeMs = gc.blockingGcTimeMs,
            bytesAllocatedTotal = gc.bytesAllocated,
            bytesFreedTotal = gc.bytesFreed,
            gcCountDelta = delta,
            memoryClassMb = raw.memoryClassMb,
            largeMemoryClassMb = raw.largeMemoryClassMb,
            isLargeHeap = raw.isLargeHeap,
            explicitGcCount = explicitGcCount.get(),
        )
    }

    companion object {
        const val MIN_INTERVAL_MS = 100L

        private fun runtimeGc() = Runtime.getRuntime().gc()
    }
}
