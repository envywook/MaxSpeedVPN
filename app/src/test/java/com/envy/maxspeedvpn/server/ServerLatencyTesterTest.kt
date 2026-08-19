package com.envy.maxspeedvpn.server

import com.envy.maxspeedvpn.subscription.ServerProfile
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerLatencyTesterTest {
    private fun server(id: String) = ServerProfile(id, "sub", id, "vless", "$id.example", 443, "{}")

    @Test
    fun `reports latency and endpoint failures independently`() = runBlocking {
        val tester = ServerLatencyTester { url ->
            if (url.contains("bad.example")) error("unreachable")
            42L
        }

        val results = tester.test(listOf(server("good"), server("bad")), concurrency = 2, timeoutMillis = 500)

        assertEquals(42L, results.getValue("good").latencyMillis)
        assertEquals(null, results.getValue("good").error)
        assertEquals(null, results.getValue("bad").latencyMillis)
        assertTrue(results.getValue("bad").error!!.isNotBlank())
    }

    @Test
    fun `never exceeds requested concurrency`() = runBlocking {
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val tester = ServerLatencyTester {
            val now = active.incrementAndGet()
            peak.updateAndGet { maxOf(it, now) }
            delay(40)
            active.decrementAndGet()
            40L
        }

        tester.test((1..8).map { server("s$it") }, concurrency = 3, timeoutMillis = 500)

        assertTrue(peak.get() <= 3)
        assertEquals(3, peak.get())
    }

    @Test
    fun `tests a single server without changing result shape`() = runBlocking {
        val tester = ServerLatencyTester { url ->
            assertEquals("https://single.example:443/", url)
            27L
        }

        val result = tester.testOne(server("single"), timeoutMillis = 3_000)

        assertEquals(27L, result.latencyMillis)
        assertEquals(null, result.error)
    }
}
