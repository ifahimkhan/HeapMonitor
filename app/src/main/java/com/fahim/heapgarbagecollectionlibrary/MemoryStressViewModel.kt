package com.fahim.heapgarbagecollectionlibrary

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Allocates and drops byte arrays so the overlay has something to show. */
class MemoryStressViewModel : ViewModel() {

    private val chunks = MutableStateFlow<List<ByteArray>>(emptyList())
    private val _heldChunks = MutableStateFlow(0)
    private val _lastError = MutableStateFlow<String?>(null)

    /** Number of chunks currently held. */
    val heldChunks: StateFlow<Int> = _heldChunks.asStateFlow()

    /** Last allocation failure message, or `null`. */
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** Allocates one `mb` MB array and keeps it alive. */
    fun allocate(mb: Int) {
        runCatching { ByteArray(mb * BYTES_PER_MB) }
            .onSuccess { array ->
                chunks.update { it + array }
                _lastError.value = null
                publishCount()
            }
            .onFailure { _lastError.value = "Allocation of $mb MB failed: ${it.javaClass.simpleName}" }
    }

    /** Drops every held chunk so the next GC can reclaim them. */
    fun release() {
        chunks.value = emptyList()
        _lastError.value = null
        publishCount()
    }

    /** Allocates and immediately discards [CHURN_COUNT] 1 MB arrays to provoke a GC. */
    fun churn() {
        runCatching {
            var sink = 0L
            repeat(CHURN_COUNT) { sink += ByteArray(BYTES_PER_MB).size }
            sink
        }.onFailure { _lastError.value = "Churn failed: ${it.javaClass.simpleName}" }
    }

    private fun publishCount() {
        _heldChunks.value = chunks.value.size
    }

    private companion object {
        const val BYTES_PER_MB = 1024 * 1024
        const val CHURN_COUNT = 50
    }
}
