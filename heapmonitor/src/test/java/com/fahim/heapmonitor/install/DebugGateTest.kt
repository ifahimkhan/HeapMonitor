package com.fahim.heapmonitor.install

import com.fahim.heapmonitor.HeapMonitorConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugGateTest {

    @Test
    fun `null enabled follows the debuggable flag`() {
        assertTrue(DebugGate.isEnabled(debuggableFlagSet = true, config = HeapMonitorConfig()))
        assertFalse(DebugGate.isEnabled(debuggableFlagSet = false, config = HeapMonitorConfig()))
    }

    @Test
    fun `enabled true overrides a non-debuggable build`() {
        val config = HeapMonitorConfig(enabled = true)
        assertTrue(DebugGate.isEnabled(debuggableFlagSet = false, config = config))
        assertTrue(DebugGate.isEnabled(debuggableFlagSet = true, config = config))
    }

    @Test
    fun `enabled false overrides a debuggable build`() {
        val config = HeapMonitorConfig(enabled = false)
        assertFalse(DebugGate.isEnabled(debuggableFlagSet = true, config = config))
        assertFalse(DebugGate.isEnabled(debuggableFlagSet = false, config = config))
    }
}
