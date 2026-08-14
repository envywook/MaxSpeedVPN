package com.envy.dualcorevpn.subscription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionDeepLinkTest {
    @Test
    fun `parses explicit url and optional name without fetching`() {
        assertEquals(
            SubscriptionImportRequest("https://provider.example/sub?id=1", "Работа"),
            SubscriptionDeepLink.parse("maxspeedvpn://add?url=https%3A%2F%2Fprovider.example%2Fsub%3Fid%3D1&name=%D0%A0%D0%B0%D0%B1%D0%BE%D1%82%D0%B0"),
        )
    }

    @Test
    fun `supports encoded path form`() {
        assertEquals(
            SubscriptionImportRequest("https://provider.example/sub"),
            SubscriptionDeepLink.parse("maxspeedvpn://subscription/https%3A%2F%2Fprovider.example%2Fsub"),
        )
    }

    @Test
    fun `clipboard accepts subscription urls and branded links only`() {
        assertEquals(SubscriptionImportRequest("https://provider.example/sub"), SubscriptionClipboard.parse("  https://provider.example/sub  "))
        assertEquals(
            SubscriptionImportRequest("https://provider.example/sub"),
            SubscriptionClipboard.parse("maxspeedvpn://add?url=https%3A%2F%2Fprovider.example%2Fsub"),
        )
        assertNull(SubscriptionClipboard.parse("lust://add?url=https%3A%2F%2Fprovider.example%2Fsub"))
        assertNull(SubscriptionClipboard.parse("vless://uuid@example.test:443"))
        assertNull(SubscriptionClipboard.parse("file:///data/local"))
    }

    @Test
    fun `rejects legacy unknown schemes hosts and non-http targets`() {
        assertNull(SubscriptionDeepLink.parse("https://provider.example/sub"))
        assertNull(SubscriptionDeepLink.parse("lust://add?url=https%3A%2F%2Fprovider.example%2Fsub"))
        assertNull(SubscriptionDeepLink.parse("maxspeedvpn://connect?url=https%3A%2F%2Fprovider.example%2Fsub"))
        assertNull(SubscriptionDeepLink.parse("maxspeedvpn://add?url=file%3A%2F%2F%2Fdata%2Flocal"))
        assertNull(SubscriptionDeepLink.parse("maxspeedvpn://add?url=javascript%3Aalert(1)"))
    }
}
