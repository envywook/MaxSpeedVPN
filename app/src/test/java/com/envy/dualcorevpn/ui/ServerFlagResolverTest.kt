package com.envy.dualcorevpn.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerFlagResolverTest {
    @Test
    fun `resolves Austrian flag from Russian server name`() {
        assertEquals("🇦🇹", serverFlagFromName("Австрия Reality"))
    }

    @Test
    fun `resolves Austrian flag from English name city and country code`() {
        assertEquals("🇦🇹", serverFlagFromName("Austria Reality"))
        assertEquals("🇦🇹", serverFlagFromName("Vienna Premium"))
        assertEquals("🇦🇹", serverFlagFromName("AT-01"))
    }

    @Test
    fun `preserves existing country mappings in Russian and English`() {
        assertEquals("🇩🇪", serverFlagFromName("Германия DE-1"))
        assertEquals("🇺🇸", serverFlagFromName("United States Reality"))
        assertEquals("🇷🇺", serverFlagFromName("Москва Premium"))
        assertEquals("🇳🇱", serverFlagFromName("Amsterdam NL"))
    }

    @Test
    fun `uses the first explicit flag as the destination and removes it from displayed name`() {
        val name = "🇺🇸🇷🇺➙🇦🇹➙США"

        assertEquals("🇺🇸", serverFlagFromName(name))
        assertEquals("США", serverDisplayName(name))
    }

    @Test
    fun `keeps cascade route after a first destination flag`() {
        assertEquals("🇫🇮", serverFlagFromName("🇫🇮🇷🇺➙Финляндия TLS"))
        assertEquals("Финляндия TLS", serverDisplayName("🇫🇮🇷🇺➙Финляндия TLS"))
    }

    @Test
    fun `does not infer country from unrelated fragments`() {
        assertEquals("🌐", serverFlagFromName("Reality Premium"))
        assertEquals("🌐", serverFlagFromName("Stable Canadair"))
        assertEquals("🌐", serverFlagFromName("Status node"))
    }
}
