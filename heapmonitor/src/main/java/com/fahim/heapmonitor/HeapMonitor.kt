package com.fahim.heapmonitor

import android.app.Application
import com.fahim.heapmonitor.core.HeapSampler
import com.fahim.heapmonitor.core.HeapSnapshot
import com.fahim.heapmonitor.core.RuntimeHeapStatsSource
import com.fahim.heapmonitor.install.ApplicationRegistrar
import com.fahim.heapmonitor.install.DebugGate
import com.fahim.heapmonitor.install.HeapLog
import com.fahim.heapmonitor.install.LifecycleRegistrar
import com.fahim.heapmonitor.install.OverlayAttacher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Public facade of the HeapMonitor library.
 *
 * Normally installed automatically by `HeapMonitorInstaller`; call [install] yourself only if you
 * removed that provider from the manifest. Every member is safe to call from any thread and is a
 * no-op when the debug gate is closed (non-debuggable build with `enabled == null`).
 */
object HeapMonitor {

    internal data class InstallState(
        val config: HeapMonitorConfig,
        val sampler: HeapSampler,
        val attacher: OverlayAttacher,
        val registrar: LifecycleRegistrar,
    )

    private val lock = Any()
    private val emptyFlow = MutableStateFlow(HeapSnapshot.EMPTY).asStateFlow()

    @Volatile
    private var state: InstallState? = null

    /** `true` once [install] has succeeded and until [uninstall]. */
    val isInstalled: Boolean get() = state != null

    /** Latest heap snapshot; [HeapSnapshot.EMPTY] until installed and the first sample lands. */
    val snapshots: StateFlow<HeapSnapshot> get() = state?.sampler?.snapshots ?: emptyFlow

    /**
     * Installs the overlay on every `ComponentActivity` of [application].
     * Returns silently when already installed or when the debug gate is closed.
     */
    fun install(application: Application, config: HeapMonitorConfig = HeapMonitorConfig()) {
        synchronized(lock) {
            if (state != null) {
                HeapLog.d("install() ignored: already installed")
                return
            }
            if (!DebugGate.isEnabled(application, config)) return
            val sampler = HeapSampler(RuntimeHeapStatsSource(application), config.sampleIntervalMs)
            val attacher = OverlayAttacher(config, sampler)
            installLocked(config, sampler, attacher, ApplicationRegistrar(application))
        }
    }

    /** Stops sampling, unregisters callbacks and removes the overlay from tracked activities. */
    fun uninstall() {
        val current = synchronized(lock) { state.also { state = null } } ?: return
        runCatching { current.registrar.unregister(current.attacher) }
        runCatching { current.sampler.stop() }
        runCatching { current.attacher.detachAll() }
        HeapLog.d("uninstalled")
    }

    /**
     * Runs `Runtime.gc()` on a background dispatcher, counts it as an explicit GC in the next
     * [HeapSnapshot] and re-samples. Returns immediately; no-op when not installed.
     */
    fun forceGc() {
        state?.sampler?.forceGc()
    }

    /** Test seam: install with pre-built collaborators and no debug-gate check. */
    internal fun installForTest(
        config: HeapMonitorConfig,
        sampler: HeapSampler,
        attacher: OverlayAttacher,
        registrar: LifecycleRegistrar,
    ) {
        synchronized(lock) {
            if (state != null) return
            installLocked(config, sampler, attacher, registrar)
        }
    }

    private fun installLocked(
        config: HeapMonitorConfig,
        sampler: HeapSampler,
        attacher: OverlayAttacher,
        registrar: LifecycleRegistrar,
    ) {
        registrar.register(attacher)
        state = InstallState(config, sampler, attacher, registrar)
        HeapLog.d("installed (interval=${config.sampleIntervalMs} ms)")
    }
}
