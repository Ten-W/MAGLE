package com.tai.oeviewer

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class IndexReadsTest {
    @Test(timeout = 3000) fun limitsInFlightReadsWithoutDroppingAnyResults() {
        val workers = Executors.newFixedThreadPool(8)
        val running = java.util.concurrent.atomic.AtomicInteger()
        val peak = java.util.concurrent.atomic.AtomicInteger()
        val accepted = mutableListOf<String>()
        try {
            assertTrue(readIndexChunk(workers, (0..7).map { "$it" }, { false }, {
                val active = running.incrementAndGet()
                peak.updateAndGet { old -> maxOf(old, active) }
                try { Thread.sleep(15); it } finally { running.decrementAndGet() }
            }, parallelism = 2) { id, result -> assertEquals(id, result.getOrThrow()); accepted += id })
            assertEquals(8, accepted.distinct().size)
            assertTrue(peak.get() <= 2)
        } finally { workers.shutdownNow() }
    }
    @Test(timeout = 3000) fun editingYieldsBlockedReadsAndCompletedItemsRemainAvailable() {
        val workers = Executors.newFixedThreadPool(2)
        val control = Executors.newSingleThreadExecutor()
        val entered = CountDownLatch(2)
        val paused = AtomicBoolean(false)
        val accepted = mutableMapOf<String, Int>()
        try {
            assertTrue(readIndexChunk(workers, listOf("cached"), { false }, { 7 }) { id, result -> accepted[id] = result.getOrThrow() })
            val index = control.submit<Boolean> {
                readIndexChunk(workers, listOf("slow-a", "slow-b"), { paused.get() }, {
                    entered.countDown(); Thread.sleep(10_000); 1
                }) { id, result -> accepted[id] = result.getOrThrow() }
            }
            assertTrue(entered.await(1, TimeUnit.SECONDS))
            paused.set(true)
            assertFalse(index.get(1, TimeUnit.SECONDS))
            assertEquals(mapOf("cached" to 7), accepted)
            paused.set(false)
            assertTrue(readIndexChunk(workers, listOf("slow-a", "slow-b"), { false }, { 9 }) { id, result -> accepted[id] = result.getOrThrow() })
            assertEquals(mapOf("cached" to 7, "slow-a" to 9, "slow-b" to 9), accepted)
        } finally { control.shutdownNow(); workers.shutdownNow() }
    }
}
