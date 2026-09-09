package com.fahim.heapmonitor.install

import android.content.Context
import android.content.pm.ApplicationInfo
import com.fahim.heapmonitor.HeapMonitorConfig

/** Decides whether the monitor may run: explicit config wins, else the app's debuggable flag. */
internal object DebugGate {

    /** Pure decision used by tests and by the [Context] overload. */
    fun isEnabled(debuggableFlagSet: Boolean, config: HeapMonitorConfig): Boolean =
        config.enabled ?: debuggableFlagSet

    fun isEnabled(context: Context, config: HeapMonitorConfig): Boolean =
        runCatching { isEnabled(isDebuggable(context), config) }.getOrDefault(false)

    private fun isDebuggable(context: Context): Boolean {
        val info = context.applicationInfo ?: return false
        return info.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
}
