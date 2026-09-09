package com.fahim.heapmonitor.core

import java.util.Locale

/** Human-readable byte formatting used by the overlay. */
object ByteFormatter {
    private const val KB = 1024.0
    private const val MB = KB * 1024.0
    private const val GB = MB * 1024.0

    /** `"0 B"`, `"512 B"`, `"1.5 KB"`, `"42.3 MB"`, `"1.2 GB"`; negative input yields `"0 B"`. */
    fun format(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val value = bytes.toDouble()
        return when {
            value < KB -> "$bytes B"
            value < MB -> oneDecimal(value / KB) + " KB"
            value < GB -> oneDecimal(value / MB) + " MB"
            else -> oneDecimal(value / GB) + " GB"
        }
    }

    /** Megabytes with one decimal and no unit, e.g. `"42.3"`; negative input yields `"0.0"`. */
    fun formatMb(bytes: Long): String {
        if (bytes <= 0L) return "0.0"
        return oneDecimal(bytes.toDouble() / MB)
    }

    private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)
}
