package com.fahim.heapgarbagecollectionlibrary

import android.app.Application

/**
 * Sample application. HeapMonitor auto-installs through its ContentProvider, so nothing is needed
 * here for the default (zero-code) integration.
 */
class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Manual config (only needed if you removed HeapMonitorInstaller in the manifest):
        // HeapMonitor.install(this, HeapMonitorConfig(sampleIntervalMs = 500, startExpanded = true))
    }
}
