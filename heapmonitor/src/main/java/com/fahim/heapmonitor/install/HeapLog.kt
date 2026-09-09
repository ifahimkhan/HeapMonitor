package com.fahim.heapmonitor.install

import android.util.Log
import com.fahim.heapmonitor.Defaults

/** Thin logger; only used once the debug gate is open, so release builds stay silent. */
internal object HeapLog {
    fun d(message: String) {
        runCatching { Log.d(Defaults.LOG_TAG, message) }
    }

    fun w(message: String, error: Throwable? = null) {
        runCatching { Log.w(Defaults.LOG_TAG, message, error) }
    }
}
