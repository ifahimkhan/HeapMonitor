package com.fahim.heapmonitor.install

import android.app.Application

/** Abstracts `Application.registerActivityLifecycleCallbacks` so the facade is unit-testable. */
internal interface LifecycleRegistrar {
    fun register(callbacks: Application.ActivityLifecycleCallbacks)
    fun unregister(callbacks: Application.ActivityLifecycleCallbacks)
}

internal class ApplicationRegistrar(private val application: Application) : LifecycleRegistrar {
    override fun register(callbacks: Application.ActivityLifecycleCallbacks) {
        application.registerActivityLifecycleCallbacks(callbacks)
    }

    override fun unregister(callbacks: Application.ActivityLifecycleCallbacks) {
        application.unregisterActivityLifecycleCallbacks(callbacks)
    }
}
