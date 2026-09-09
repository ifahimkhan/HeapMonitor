package com.fahim.heapmonitor

import android.app.Application
import com.fahim.heapmonitor.core.HeapSampler
import com.fahim.heapmonitor.core.HeapSnapshot
import com.fahim.heapmonitor.core.FakeHeapStatsSource
import com.fahim.heapmonitor.core.rawStats
import com.fahim.heapmonitor.install.LifecycleRegistrar
import com.fahim.heapmonitor.install.OverlayAttacher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeapMonitorFacadeTest {

    /** Records register/unregister calls instead of touching a real Application. */
    private class FakeRegistrar : LifecycleRegistrar {
        var registered: Application.ActivityLifecycleCallbacks? = null
        var unregisterCount = 0

        override fun register(callbacks: Application.ActivityLifecycleCallbacks) {
            registered = callbacks
        }

        override fun unregister(callbacks: Application.ActivityLifecycleCallbacks) {
            unregisterCount++
            registered = null
        }
    }

    private fun installFake(
        gcCount: Long = 5L,
        dispatcher: TestDispatcher = StandardTestDispatcher(),
    ): Pair<HeapSampler, FakeRegistrar> {
        val sampler = HeapSampler(
            source = FakeHeapStatsSource(listOf(rawStats(gcCount = gcCount))),
            intervalMs = 1_000L,
            dispatcher = dispatcher,
        )
        val config = HeapMonitorConfig(enabled = true)
        val registrar = FakeRegistrar()
        HeapMonitor.installForTest(config, sampler, OverlayAttacher(config, sampler), registrar)
        return sampler to registrar
    }

    @After
    fun tearDown() {
        HeapMonitor.uninstall()
    }

    @Test
    fun `not installed by default`() {
        assertFalse(HeapMonitor.isInstalled)
        assertEquals(HeapSnapshot.EMPTY, HeapMonitor.snapshots.value)
    }

    @Test
    fun `forceGc is safe when not installed`() {
        HeapMonitor.forceGc()
        assertEquals(HeapSnapshot.EMPTY, HeapMonitor.snapshots.value)
    }

    @Test
    fun `uninstall is safe when not installed`() {
        HeapMonitor.uninstall()
        assertFalse(HeapMonitor.isInstalled)
    }

    @Test
    fun `install registers callbacks and exposes the sampler flow`() = runTest {
        val (sampler, registrar) = installFake()
        assertTrue(HeapMonitor.isInstalled)
        assertTrue(registrar.registered is OverlayAttacher)
        assertSame(sampler.snapshots, HeapMonitor.snapshots)
    }

    @Test
    fun `install twice is idempotent`() {
        val (firstSampler, firstRegistrar) = installFake()
        val (_, secondRegistrar) = installFake()
        assertSame(firstSampler.snapshots, HeapMonitor.snapshots)
        assertTrue(firstRegistrar.registered is OverlayAttacher)
        assertNull(secondRegistrar.registered)
    }

    @Test
    fun `forceGc runs off the caller thread then samples`() = runTest {
        installFake(gcCount = 42L, dispatcher = StandardTestDispatcher(testScheduler))
        HeapMonitor.forceGc()
        assertEquals(HeapSnapshot.EMPTY, HeapMonitor.snapshots.value)

        runCurrent()
        val s = HeapMonitor.snapshots.value
        assertEquals(42L, s.gcCount)
        assertEquals(1L, s.explicitGcCount)
    }

    @Test
    fun `uninstall resets state and unregisters`() {
        val (sampler, registrar) = installFake()
        HeapMonitor.uninstall()
        assertFalse(HeapMonitor.isInstalled)
        assertEquals(1, registrar.unregisterCount)
        assertFalse(sampler.isRunning)
        assertEquals(HeapSnapshot.EMPTY, HeapMonitor.snapshots.value)
    }
}
