package com.fahim.heapmonitor.install

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fahim.heapmonitor.Defaults
import com.fahim.heapmonitor.HeapMonitorConfig
import com.fahim.heapmonitor.core.HeapSampler
import com.fahim.heapmonitor.ui.HeapMonitorOverlay
import java.util.Collections
import java.util.WeakHashMap

/**
 * Adds one overlay [ComposeView] to every resumed [ComponentActivity] window and drives the sampler
 * from the started-activity count (foreground only). Never holds strong Activity references.
 */
internal class OverlayAttacher(
    private val config: HeapMonitorConfig,
    private val sampler: HeapSampler,
) : Application.ActivityLifecycleCallbacks {

    private val lock = Any()
    private var startedCount = 0
    private val attached: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap())
    private val warnedClasses: MutableSet<String> = Collections.synchronizedSet(HashSet())

    override fun onActivityStarted(activity: Activity) = guarded("onActivityStarted") {
        val becameForeground = synchronized(lock) { ++startedCount == 1 }
        if (becameForeground) {
            HeapLog.d("app foregrounded, sampler start")
            sampler.start()
        }
    }

    override fun onActivityStopped(activity: Activity) = guarded("onActivityStopped") {
        val becameBackground = synchronized(lock) {
            startedCount = (startedCount - 1).coerceAtLeast(0)
            startedCount == 0
        }
        if (becameBackground) {
            HeapLog.d("app backgrounded, sampler stop")
            sampler.stop()
        }
    }

    override fun onActivityResumed(activity: Activity) = guarded("onActivityResumed") {
        if (activity !is ComponentActivity) {
            warnOnce(activity)
            return@guarded
        }
        attach(activity)
    }

    override fun onActivityDestroyed(activity: Activity) = guarded("onActivityDestroyed") {
        detach(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    /** Removes the overlay from every activity still tracked. Called by `HeapMonitor.uninstall()`. */
    fun detachAll() = guarded("detachAll") {
        val activities = synchronized(lock) { attached.toList() }
        activities.forEach { detach(it) }
    }

    private fun attach(activity: ComponentActivity) {
        val decor = activity.window?.decorView as? ViewGroup ?: return
        if (decor.findViewWithTag<View>(Defaults.OVERLAY_TAG) != null) return
        if (activity.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        // On a local relaunch (recreate / rotation) Android reuses the decor view, which still
        // carries the destroyed activity as its owner, so refresh the owners in that case too.
        val decorOwner = decor.findViewTreeLifecycleOwner()
        if (decorOwner == null || decorOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            activity.initializeViewTreeOwners()
        }
        HeapLog.d("attach overlay to ${activity.javaClass.simpleName}")

        val view = ComposeView(activity).apply {
            tag = Defaults.OVERLAY_TAG
            // Owners live on the overlay view itself so a stale owner on the decor cannot break it.
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { HeapMonitorOverlay(config = config) }
        }
        decor.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        decor.bringChildToFront(view)
        synchronized(lock) { attached.add(activity) }
    }

    private fun detach(activity: Activity) {
        val decor = activity.window?.decorView as? ViewGroup
        decor?.findViewWithTag<View>(Defaults.OVERLAY_TAG)?.let { decor.removeView(it) }
        synchronized(lock) { attached.remove(activity) }
    }

    private fun warnOnce(activity: Activity) {
        val name = activity.javaClass.name
        if (warnedClasses.add(name)) {
            HeapLog.w("$name is not a ComponentActivity; overlay skipped")
        }
    }

    private inline fun guarded(callback: String, block: () -> Unit) {
        runCatching(block).onFailure { HeapLog.w("$callback failed", it) }
    }
}
