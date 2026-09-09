package com.fahim.heapmonitor.install

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.fahim.heapmonitor.HeapMonitor

/**
 * Auto-initialises [HeapMonitor] with default config before `Application.onCreate()`.
 *
 * To configure manually, remove this provider in your app manifest:
 * ```xml
 * <provider
 *     android:name="com.fahim.heapmonitor.install.HeapMonitorInstaller"
 *     android:authorities="${applicationId}.heapmonitor-installer"
 *     tools:node="remove" />
 * ```
 * then call `HeapMonitor.install(this, HeapMonitorConfig(...))` from `Application.onCreate()`.
 *
 * `onCreate()` only runs the debug-gate check and registers lifecycle callbacks; sampling starts
 * when the first activity starts.
 */
class HeapMonitorInstaller : ContentProvider() {

    override fun onCreate(): Boolean {
        val application = context?.applicationContext as? Application ?: return true
        runCatching { HeapMonitor.install(application) }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0
}
