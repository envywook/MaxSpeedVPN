package com.envy.maxspeedvpn.core

import java.net.InetSocketAddress
import java.net.ServerSocket
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPortAvailabilityTest {
    @Test
    fun `rejects an active listener and accepts its port after close`() {
        val listener = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress("127.0.0.1", 0))
        }
        val port = listener.localPort

        assertFalse(isLocalPortAvailable(port))
        listener.close()
        assertTrue(isLocalPortAvailable(port))
    }
}
