package dev.aaa1115910.bv.network

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeetestCompanionServiceTest {
    @Test
    fun concurrentResultPostsNotifyTvOnlyOnce() {
        val callbackCount = AtomicInteger()
        val session = GeetestCompanionService.createSession(
            gt = "gt",
            challenge = "challenge",
            onResult = { callbackCount.incrementAndGet() },
        )
        val workerCount = 24
        val ready = CountDownLatch(workerCount)
        val start = CountDownLatch(1)
        val workers = List(workerCount) {
            thread(start = true) {
                ready.countDown()
                start.await()
                GeetestCompanionService.completeSession(
                    id = session.id,
                    challenge = "result-challenge",
                    validate = "validate",
                    seccode = "seccode",
                )
            }
        }

        try {
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            workers.forEach { worker ->
                worker.join(5_000)
                assertFalse(worker.isAlive)
            }

            assertEquals(1, callbackCount.get())
            assertTrue(session.completed)
            assertNotNull(session.result)
        } finally {
            start.countDown()
            workers.forEach { it.join(5_000) }
            GeetestCompanionService.removeSession(session.id)
        }
    }
}
