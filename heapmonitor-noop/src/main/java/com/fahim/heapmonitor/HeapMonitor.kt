package com.fahim.heapmonitor

import android.app.Application
import com.fahim.heapmonitor.core.HeapSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * No-op mirror of the real `HeapMonitor` facade. Use it as `releaseImplementation` so release
 * builds carry no sampling or overlay code at all. Every call is a no-op.
 */
object HeapMonitor {
    private val emptyFlow = MutableStateFlow(HeapSnapshot.EMPTY).asStateFlow()

    /** Always `false` in the no-op artifact. */
    val isInstalled: Boolean get() = false

    /** Always [HeapSnapshot.EMPTY]. */
    val snapshots: StateFlow<HeapSnapshot> get() = emptyFlow

    @Suppress("UNUSED_PARAMETER")
    fun install(application: Application, config: HeapMonitorConfig = HeapMonitorConfig()) = Unit

    fun uninstall() = Unit

    fun forceGc() = Unit
}
