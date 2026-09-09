package com.fahim.heapmonitor.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeapSamplerTest {

    private val interval = 1_000L

    private fun sampler(
        script: List<RawHeapStats?>,
        dispatcher: TestDispatcher,
        intervalMs: Long = interval,
    ): Pair<HeapSampler, FakeHeapStatsSource> {
        val source = FakeHeapStatsSource(script)
        var now = 0L
        val sampler = HeapSampler(source, intervalMs, dispatcher) { now += 1L; now }
        return sampler to source
    }

    @Test
    fun `starts at EMPTY and emits on start`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (sampler, _) = sampler(listOf(rawStats()), dispatcher)
        assertEquals(HeapSnapshot.EMPTY, sampler.snapshots.value)

        sampler.start()
        runCurrent()

        val s = sampler.snapshots.value
        assertEquals(200L * MB, s.maxBytes)
        assertEquals(84L * MB, s.usedBytes)
        assertEquals(42, s.usedPercent)
        assertEquals(17L, s.gcCount)
        assertEquals(0L, s.gcCountDelta)
        assertEquals(192, s.memoryClassMb)
        sampler.stop()
    }

    @Test
    fun `gc delta is computed from previous snapshot`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (sampler, _) = sampler(
            listOf(rawStats(gcCount = 17), rawStats(gcCount = 20), rawStats(gcCount = 20)),
            dispatcher,
        )
        sampler.start()
        runCurrent()
        assertEquals(0L, sampler.snapshots.value.gcCountDelta)

        advanceTimeBy(interval + 1)
        assertEquals(20L, sampler.snapshots.value.gcCount)
        assertEquals(3L, sampler.snapshots.value.gcCountDelta)

        advanceTimeBy(interval)
        assertEquals(0L, sampler.snapshots.value.gcCountDelta)
        sampler.stop()
    }

    @Test
    fun `stop halts emissions`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (sampler, source) = sampler(listOf(rawStats()), dispatcher)
        sampler.start()
        runCurrent()
        assertTrue(sampler.isRunning)
        val readsBefore = source.readCount

        sampler.stop()
        assertFalse(sampler.isRunning)
        advanceTimeBy(interval * 5)
        assertEquals(readsBefore, source.readCount)
    }

    @Test
    fun `start is idempotent`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (sampler, source) = sampler(listOf(rawStats()), dispatcher)
        sampler.start()
        sampler.start()
        runCurrent()
        assertEquals(1, source.readCount)
        sampler.stop()
    }

    @Test
    fun `stop twice then restart works`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (sampler, source) = sampler(listOf(rawStats()), dispatcher)
        sampler.start()
        runCurrent()
        sampler.stop()
        sampler.stop()

        sampler.start()
        runCurrent()
        assertEquals(2, source.readCount)
        advanceTimeBy(interval + 1)
        assertEquals(3, source.readCount)
        sampler.stop()
    }

    @Test
    fun `throwing source does not kill the loop`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (sampler, source) = sampler(listOf(null, null, rawStats(gcCount = 9)), dispatcher)
        sampler.start()
        runCurrent()
        assertEquals(HeapSnapshot.EMPTY, sampler.snapshots.value)

        advanceTimeBy(interval * 2 + 1)
        assertTrue(sampler.isRunning)
        assertEquals(3, source.readCount)
        assertEquals(9L, sampler.snapshots.value.gcCount)
        sampler.stop()
    }

    @Test
    fun `interval is coerced to minimum`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (sampler, source) = sampler(listOf(rawStats()), dispatcher, intervalMs = 1L)
        sampler.start()
        runCurrent()
        advanceTimeBy(HeapSampler.MIN_INTERVAL_MS - 1)
        assertEquals(1, source.readCount)
        advanceTimeBy(2)
        assertEquals(2, source.readCount)
        sampler.stop()
    }

    @Test
    fun `sampleNow emits synchronously without start`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val (sampler, source) = sampler(listOf(rawStats(gcCount = 3)), dispatcher)
        sampler.sampleNow()
        assertEquals(1, source.readCount)
        assertEquals(3L, sampler.snapshots.value.gcCount)
        assertFalse(sampler.isRunning)
    }
}
