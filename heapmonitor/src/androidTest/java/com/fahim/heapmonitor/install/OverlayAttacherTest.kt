package com.fahim.heapmonitor.install

import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.heapmonitor.Defaults
import com.fahim.heapmonitor.HeapMonitor
import com.fahim.heapmonitor.HeapMonitorConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Empty host activity declared in `src/androidTest/AndroidManifest.xml`. */
class TestHostActivity : ComponentActivity()

@RunWith(AndroidJUnit4::class)
class OverlayAttacherTest {

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        HeapMonitor.uninstall()
        HeapMonitor.install(application, HeapMonitorConfig(enabled = true, sampleIntervalMs = 200))
        assertTrue(HeapMonitor.isInstalled)
    }

    @After
    fun tearDown() {
        HeapMonitor.uninstall()
    }

    @Test
    fun overlayAttachedOnceAndSurvivesRecreate() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> assertEquals(1, countOverlays(activity)) }
            scenario.recreate()
            scenario.onActivity { activity -> assertEquals(1, countOverlays(activity)) }
        }
    }

    @Test
    fun uninstallRemovesOverlay() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(1, countOverlays(activity))
                HeapMonitor.uninstall()
                assertEquals(0, countOverlays(activity))
            }
        }
    }

    @Test
    fun samplerProducesSnapshotsWhileForeground() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { }
            val deadline = System.currentTimeMillis() + 5_000
            while (HeapMonitor.snapshots.value.maxBytes == 0L && System.currentTimeMillis() < deadline) {
                Thread.yield()
            }
            assertTrue(HeapMonitor.snapshots.value.maxBytes > 0L)
        }
    }

    private fun countOverlays(activity: ComponentActivity): Int {
        val decor = activity.window.decorView as ViewGroup
        return (0 until decor.childCount)
            .map { decor.getChildAt(it) }
            .count { view: View -> view.tag == Defaults.OVERLAY_TAG }
    }
}
